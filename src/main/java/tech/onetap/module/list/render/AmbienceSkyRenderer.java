package tech.onetap.module.list.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import tech.onetap.util.render.providers.ColorProvider;

public final class AmbienceSkyRenderer {
    private static final float SKY_SIZE = 96.0f;
    private static final ShaderProgramKey SKY_SHADER = new ShaderProgramKey(
            Identifier.of("mre", "core/sky_shader"),
            VertexFormats.POSITION,
            Defines.EMPTY
    );
    private static final int PLASMA_PRIMARY = ColorProvider.rgba(44, 24, 88, 255);
    private static final int PLASMA_SECONDARY = ColorProvider.rgba(42, 126, 214, 255);
    private static final int PLASMA_ACCENT = ColorProvider.rgba(255, 122, 226, 255);
    private static final int COSMOS_PRIMARY = ColorProvider.rgba(10, 18, 48, 255);
    private static final int COSMOS_SECONDARY = ColorProvider.rgba(82, 58, 144, 255);
    private static final int COSMOS_ACCENT = ColorProvider.rgba(150, 220, 255, 255);

    private AmbienceSkyRenderer() {
    }

    public static void render(WorldTweaks ambience, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }

        try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);

            ShaderProgram shader = RenderSystem.setShader(SKY_SHADER);
            if (shader != null) {
                applyUniforms(ambience, shader);
            }

            drawSkyCube();
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private static void applyUniforms(WorldTweaks ambience, ShaderProgram shader) {
        boolean cosmos = ambience.isSkyShaderMode("Cosmos");
        int primary = mixTheme(cosmos ? COSMOS_PRIMARY : PLASMA_PRIMARY, ColorProvider.getThemeColor(), cosmos ? 0.08f : 0.14f);
        int secondary = mixTheme(cosmos ? COSMOS_SECONDARY : PLASMA_SECONDARY, ColorProvider.getThemeColorTwo(), cosmos ? 0.12f : 0.10f);
        int accent = mixTheme(cosmos ? COSMOS_ACCENT : PLASMA_ACCENT, ColorProvider.getThemeColor(), 0.06f);
        float time = ((System.currentTimeMillis() % 1000000L) / 1000.0f) * ambience.getSkyShaderSpeed();

        if (shader.getUniform("time") != null) shader.getUniform("time").set(time);
        if (shader.getUniform("scale") != null) shader.getUniform("scale").set(ambience.getSkyShaderScale());
        if (shader.getUniform("mode") != null) shader.getUniform("mode").set(cosmos ? 1 : 0);

        setColorUniform(shader, "primaryColor", primary);
        setColorUniform(shader, "secondaryColor", secondary);
        setColorUniform(shader, "accentColor", accent);
    }

    private static void setColorUniform(ShaderProgram shader, String name, int color) {
        if (shader.getUniform(name) == null) {
            return;
        }

        shader.getUniform(name).set(
                ColorProvider.red(color) / 255.0f,
                ColorProvider.green(color) / 255.0f,
                ColorProvider.blue(color) / 255.0f,
                ColorProvider.alpha(color) / 255.0f
        );
    }

    private static int mixTheme(int baseColor, int themeColor, float amount) {
        return ColorProvider.interpolate(baseColor, ColorProvider.setAlpha(themeColor, 255), amount);
    }

    private static void drawSkyCube() {
        Matrix4f matrix = new Matrix4f();
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        float s = SKY_SIZE;
        addFace(builder, matrix, -s, -s, -s, -s, s, -s, s, s, -s, s, -s, -s);
        addFace(builder, matrix, s, -s, s, s, s, s, -s, s, s, -s, -s, s);
        addFace(builder, matrix, -s, s, s, -s, s, -s, s, s, -s, s, s, s);
        addFace(builder, matrix, -s, -s, -s, -s, -s, s, s, -s, s, s, -s, -s);
        addFace(builder, matrix, -s, -s, s, -s, s, s, -s, s, -s, -s, -s, -s);
        addFace(builder, matrix, s, -s, -s, s, s, -s, s, s, s, s, -s, s);
        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    private static void addFace(
            BufferBuilder builder,
            Matrix4f matrix,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4
    ) {
        builder.vertex(matrix, x1, y1, z1);
        builder.vertex(matrix, x2, y2, z2);
        builder.vertex(matrix, x3, y3, z3);
        builder.vertex(matrix, x4, y4, z4);
    }
}
