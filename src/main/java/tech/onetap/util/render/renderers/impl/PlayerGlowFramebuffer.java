package tech.onetap.util.render.renderers.impl;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL11;
import tech.onetap.util.render.providers.ResourceProvider;

public final class PlayerGlowFramebuffer {
    private static final ShaderProgramKey KAWASE_DOWN_SHADER_KEY = new ShaderProgramKey(
            ResourceProvider.getShaderIdentifier("glow_blur_downscale"),
            VertexFormats.POSITION,
            Defines.EMPTY
    );
    private static final ShaderProgramKey KAWASE_UP_SHADER_KEY = new ShaderProgramKey(
            ResourceProvider.getShaderIdentifier("glow_blur_upscale"),
            VertexFormats.POSITION,
            Defines.EMPTY
    );
    private static final float MASK_CUTOUT_ALPHA = 0.84f;
    private static final Supplier<SimpleFramebuffer> MASK_FBO_SUPPLIER = Suppliers.memoize(() -> {
        SimpleFramebuffer framebuffer = new SimpleFramebuffer(1, 1, true);
        framebuffer.setTexFilter(GL11.GL_LINEAR);
        return framebuffer;
    });
    private static final Supplier<SimpleFramebuffer> EDGE_FBO_SUPPLIER = Suppliers.memoize(() -> {
        SimpleFramebuffer framebuffer = new SimpleFramebuffer(1, 1, false);
        framebuffer.setTexFilter(GL11.GL_LINEAR);
        return framebuffer;
    });
    private static final Supplier<SimpleFramebuffer> BLUR_PING_FBO_SUPPLIER = Suppliers.memoize(() -> {
        SimpleFramebuffer framebuffer = new SimpleFramebuffer(1, 1, false);
        framebuffer.setTexFilter(GL11.GL_LINEAR);
        return framebuffer;
    });
    private static final Supplier<SimpleFramebuffer> BLUR_PONG_FBO_SUPPLIER = Suppliers.memoize(() -> {
        SimpleFramebuffer framebuffer = new SimpleFramebuffer(1, 1, false);
        framebuffer.setTexFilter(GL11.GL_LINEAR);
        return framebuffer;
    });

    private static boolean framePrepared;
    private static boolean hasCapture;

    private PlayerGlowFramebuffer() {
    }

    public static void beginFrame() {
        framePrepared = false;
        hasCapture = false;
    }

    public static void reset() {
        framePrepared = false;
        hasCapture = false;
    }

    public static void capture(int color, boolean throughWalls, Runnable renderer) {
        MinecraftClient client = MinecraftClient.getInstance();
        Framebuffer mainFbo = client.getFramebuffer();
        if (mainFbo == null || mainFbo.textureWidth <= 0 || mainFbo.textureHeight <= 0) {
            return;
        }

        SimpleFramebuffer maskFbo = MASK_FBO_SUPPLIER.get();
        ensureFramebuffer(maskFbo, mainFbo.textureWidth, mainFbo.textureHeight);

        if (!framePrepared) {
            maskFbo.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            maskFbo.clear();
            framePrepared = true;
        }

        if (!throughWalls) {
            maskFbo.copyDepthFrom(mainFbo);
        }

        maskFbo.beginWrite(true);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        if (throughWalls) {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
        } else {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
        }

        RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION);
        RenderSystem.setShaderColor(
                ((color >> 16) & 0xFF) / 255.0f,
                ((color >> 8) & 0xFF) / 255.0f,
                (color & 0xFF) / 255.0f,
                ((color >> 24) & 0xFF) / 255.0f
        );

        renderer.run();

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        mainFbo.beginWrite(true);
        hasCapture = true;
    }

    public static void renderComposite(float radius, int passes, boolean additive, float silhouetteAlpha, float intensity) {
        if (!hasCapture) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        Framebuffer mainFbo = client.getFramebuffer();
        if (mainFbo == null || mainFbo.textureWidth <= 0 || mainFbo.textureHeight <= 0) {
            return;
        }

        SimpleFramebuffer pingFbo = BLUR_PING_FBO_SUPPLIER.get();
        SimpleFramebuffer pongFbo = BLUR_PONG_FBO_SUPPLIER.get();
        SimpleFramebuffer edgeFbo = EDGE_FBO_SUPPLIER.get();
        ensureFramebuffer(pingFbo, mainFbo.textureWidth, mainFbo.textureHeight);
        ensureFramebuffer(pongFbo, mainFbo.textureWidth, mainFbo.textureHeight);
        ensureFramebuffer(edgeFbo, mainFbo.textureWidth, mainFbo.textureHeight);

        Framebuffer source = MASK_FBO_SUPPLIER.get();
        Framebuffer destination = pingFbo;
        int blurPasses = Math.max(2, passes);

        pushScreenMatrices();
        for (int pass = 0; pass < blurPasses; pass++) {
            float passFactor = (pass + 1.0f) / (float) blurPasses;
            float offset = Math.max(0.35f, radius * (0.45f + passFactor * 0.75f));
            applyBlurPass(KAWASE_DOWN_SHADER_KEY, source, destination, offset);
            source = destination;
            destination = source == pingFbo ? pongFbo : pingFbo;

            applyBlurPass(KAWASE_UP_SHADER_KEY, source, destination, offset);
            source = destination;
            destination = source == pingFbo ? pongFbo : pingFbo;
        }

        edgeFbo.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        edgeFbo.clear();
        edgeFbo.beginWrite(true);
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        drawFramebufferTexture(source.getColorAttachment());

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, MASK_CUTOUT_ALPHA);
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.ZERO,
                GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ZERO,
                GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA
        );
        drawFramebufferTexture(MASK_FBO_SUPPLIER.get().getColorAttachment());
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        if (silhouetteAlpha > 0.001f) {
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, silhouetteAlpha);
            drawFramebufferTexture(MASK_FBO_SUPPLIER.get().getColorAttachment());
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        mainFbo.beginWrite(true);
        if (additive) {
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        } else {
            RenderSystem.defaultBlendFunc();
        }
        drawFramebufferTextureScaled(edgeFbo.getColorAttachment(), intensity);

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderTexture(0, 0);
        popScreenMatrices();

        hasCapture = false;
    }

    private static void ensureFramebuffer(SimpleFramebuffer framebuffer, int width, int height) {
        if (framebuffer.textureWidth != width || framebuffer.textureHeight != height) {
            framebuffer.resize(width, height);
        }

        if (framebuffer.texFilter != GL11.GL_LINEAR) {
            framebuffer.setTexFilter(GL11.GL_LINEAR);
        }
    }

    private static void pushScreenMatrices() {
        MinecraftClient client = MinecraftClient.getInstance();
        float width = client.getWindow().getScaledWidth();
        float height = client.getWindow().getScaledHeight();

        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0.0f, width, height, 0.0f, -1.0f, 1.0f), ProjectionType.ORTHOGRAPHIC);

        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.identity();
    }

    private static void popScreenMatrices() {
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.popMatrix();
        RenderSystem.restoreProjectionMatrix();
    }

    private static void applyBlurPass(ShaderProgramKey shaderKey, Framebuffer source, Framebuffer destination, float offset) {
        destination.beginWrite(true);
        RenderSystem.setShaderTexture(0, source.getColorAttachment());

        ShaderProgram shader = RenderSystem.setShader(shaderKey);
        if (shader.getUniform("uHalfTexelSize") != null) {
            shader.getUniform("uHalfTexelSize").set(0.5f / (float) source.textureWidth, 0.5f / (float) source.textureHeight);
        }
        if (shader.getUniform("uOffset") != null) {
            shader.getUniform("uOffset").set(offset);
        }

        drawFullScreenQuad();
        destination.endWrite();
    }

    private static void drawFramebufferTexture(int textureId) {
        MinecraftClient client = MinecraftClient.getInstance();
        float width = client.getWindow().getScaledWidth();
        float height = client.getWindow().getScaledHeight();
        Matrix4f matrix = new Matrix4f();

        RenderSystem.setShaderTexture(0, textureId);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        builder.vertex(matrix, 0.0f, 0.0f, 0.0f).texture(0.0f, 0.0f).color(-1);
        builder.vertex(matrix, 0.0f, height, 0.0f).texture(0.0f, 1.0f).color(-1);
        builder.vertex(matrix, width, height, 0.0f).texture(1.0f, 1.0f).color(-1);
        builder.vertex(matrix, width, 0.0f, 0.0f).texture(1.0f, 0.0f).color(-1);
        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    private static void drawFramebufferTextureScaled(int textureId, float strength) {
        float remaining = Math.max(0.05f, strength);
        while (remaining > 0.001f) {
            float alpha = Math.min(1.0f, remaining);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            drawFramebufferTexture(textureId);
            remaining -= 1.0f;
        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void drawFullScreenQuad() {
        MinecraftClient client = MinecraftClient.getInstance();
        float width = client.getWindow().getScaledWidth();
        float height = client.getWindow().getScaledHeight();
        Matrix4f matrix = new Matrix4f();

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        builder.vertex(matrix, 0.0f, 0.0f, 0.0f);
        builder.vertex(matrix, 0.0f, height, 0.0f);
        builder.vertex(matrix, width, height, 0.0f);
        builder.vertex(matrix, width, 0.0f, 0.0f);
        BufferRenderer.drawWithGlobalProgram(builder.end());
    }
}

