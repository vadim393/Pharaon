package tech.onetap.util.render.renderers.impl;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import tech.onetap.util.render.builders.states.QuadColorState;
import tech.onetap.util.render.builders.states.QuadRadiusState;
import tech.onetap.util.render.builders.states.SizeState;
import tech.onetap.util.render.providers.ResourceProvider;
import tech.onetap.util.render.renderers.IRenderer;

public record BuiltBlur(
        SizeState size,
        QuadRadiusState radius,
        QuadColorState color,
        float smoothness,
        float blurRadius
) implements IRenderer {

    private static final ShaderProgramKey BLUR_SHADER_KEY = new ShaderProgramKey(
            ResourceProvider.getShaderIdentifier("blur"),
            VertexFormats.POSITION_COLOR,
            Defines.EMPTY
    );

    private static final Supplier<SimpleFramebuffer> CAPTURE_FBO_SUPPLIER = Suppliers
            .memoize(() -> new SimpleFramebuffer(1, 1, false));

    private static final int BLUR_CAPTURE_FRAME_INTERVAL = 1;

    private static int frameCounter = 0;
    private static int lastCaptureFrame = -1;
    private static int lastCaptureWidth = -1;
    private static int lastCaptureHeight = -1;

    public static void beginFrame() {
        frameCounter++;
        if (frameCounter == Integer.MAX_VALUE) {
            frameCounter = 0;
            lastCaptureFrame = -1;
        }
    }

    @Override
    public void render(Matrix4f matrix, float x, float y, float z) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Framebuffer mainFbo = mc.getFramebuffer();
        if (mainFbo == null || mainFbo.textureWidth <= 0 || mainFbo.textureHeight <= 0) {
            return;
        }

        SimpleFramebuffer captureFbo = CAPTURE_FBO_SUPPLIER.get();
        ensureFramebuffer(captureFbo, mainFbo.textureWidth, mainFbo.textureHeight);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        boolean refreshCapture = mainFbo.textureWidth != lastCaptureWidth
                || mainFbo.textureHeight != lastCaptureHeight
                || lastCaptureFrame < 0
                || frameCounter - lastCaptureFrame >= BLUR_CAPTURE_FRAME_INTERVAL;

        if (refreshCapture) {
            captureFbo.beginWrite(false);
            mainFbo.draw(captureFbo.textureWidth, captureFbo.textureHeight);
            mainFbo.beginWrite(false);

            lastCaptureFrame = frameCounter;
            lastCaptureWidth = mainFbo.textureWidth;
            lastCaptureHeight = mainFbo.textureHeight;
        }

        RenderSystem.setShaderTexture(0, captureFbo.getColorAttachment());

        float width = this.size.width();
        float height = this.size.height();
        ShaderProgram shader = RenderSystem.setShader(BLUR_SHADER_KEY);
        if (shader == null) {
            RenderSystem.setShaderTexture(0, 0);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            return;
        }
        shader.getUniform("Size").set(width, height);
        shader.getUniform("Radius").set(this.radius.radius1(), this.radius.radius2(),
                this.radius.radius3(), this.radius.radius4());
        shader.getUniform("Smoothness").set(this.smoothness);
        shader.getUniform("BlurRadius").set(this.blurRadius);

        BufferBuilder builder = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        builder.vertex(matrix, x, y, z).color(color.color1());
        builder.vertex(matrix, x, y + height, z).color(color.color2());
        builder.vertex(matrix, x + width, y + height, z).color(color.color3());
        builder.vertex(matrix, x + width, y, z).color(color.color4());

        BufferRenderer.drawWithGlobalProgram(builder.end());

        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void ensureFramebuffer(SimpleFramebuffer framebuffer, int width, int height) {
        if (framebuffer.textureWidth != width || framebuffer.textureHeight != height) {
            framebuffer.resize(width, height);
        }
        if (framebuffer.texFilter != GL11.GL_LINEAR) {
            framebuffer.setTexFilter(GL11.GL_LINEAR);
        }
    }
}