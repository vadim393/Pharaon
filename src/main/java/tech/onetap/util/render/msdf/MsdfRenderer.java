package tech.onetap.util.render.msdf;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import tech.onetap.util.render.providers.ResourceProvider;

import java.util.List;

@UtilityClass
public class MsdfRenderer {

    public final ShaderProgramKey MSDF_FONT_SHADER_KEY = new ShaderProgramKey(
            ResourceProvider.getShaderIdentifier("msdf_font"),
            VertexFormats.POSITION_TEXTURE_COLOR,
            Defines.EMPTY
    );

    public void renderText(
            MsdfFont font,
            String text,
            float size,
            int color,
            Matrix4f matrix,
            float x,
            float y,
            float z
    ) {
        renderText(font, text, size, color, matrix, x, y, z, false, 0.0f, 1.0f, 0.0F);
    }

    public void renderText(
            MsdfFont font,
            String text,
            float size,
            int color,
            Matrix4f matrix,
            float x,
            float y,
            float z,
            boolean enableFadeout,
            float fadeoutStart,
            float fadeoutEnd,
            float maxWidth
    ) {
        float thickness = 0.05f;
        float spacing = 0;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        ShaderProgram shader = RenderSystem.setShader(MSDF_FONT_SHADER_KEY);
        setShaderFont(shader, font);
        shader.getUniform("Thickness").set(thickness);
        shader.getUniform("Smoothness").set(0.5f);

        shader.getUniform("EnableFadeout").set(enableFadeout ? 1 : 0);
        shader.getUniform("FadeoutStart").set(fadeoutStart);
        shader.getUniform("FadeoutEnd").set(fadeoutEnd);
        shader.getUniform("MaxWidth").set(maxWidth);
        shader.getUniform("TextPosX").set(x);

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        font.applyGlyphs(
                matrix,
                builder,
                text,
                size,
                thickness * 0.5f * size,
                spacing,
                x - 0.75F,
                y + (size * 0.7F),
                z,
                color
        );

        BuiltBuffer builtBuffer = builder.endNullable();
        if (builtBuffer != null) {
            BufferRenderer.drawWithGlobalProgram(builtBuffer);
        }

        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public void renderText(
            MsdfFont font,
            String text,
            float size,
            int color,
            Matrix4f matrix,
            float x,
            float y,
            float z,
            boolean enableFadeout,
            float fadeoutStart,
            float fadeoutEnd
    ) {
        float maxWidth = font.getWidth(text, size) * 2.0F;
        renderText(font, text, size, color, matrix, x, y, z, enableFadeout, fadeoutStart, fadeoutEnd, maxWidth);
    }

    public void renderText(
            MsdfFont font,
            Text text,
            float size,
            Matrix4f matrix,
            float x,
            float y,
            float z
    ) {
        renderText(font, text, size, matrix, x, y, z, false, 0.0f, 1.0f, 0.0F);
    }

    public void renderText(
            MsdfFont font,
            Text text,
            float size,
            Matrix4f matrix,
            float x,
            float y,
            float z,
            int alpha
    ) {
        renderText(font, text, size, matrix, x, y, z, false, 0.0f, 1.0f, 0.0F, alpha);
    }

    public void renderText(
            MsdfFont font,
            Text text,
            float size,
            Matrix4f matrix,
            float x,
            float y,
            float z,
            boolean enableFadeout,
            float fadeoutStart,
            float fadeoutEnd,
            float maxWidth
    ) {
        List<FormattedTextProcessor.TextSegment> segments = FormattedTextProcessor.processText(text, -1);
        renderStyledText(font, segments, size, matrix, x, y, z, enableFadeout, fadeoutStart, fadeoutEnd, maxWidth, null);
    }

    public void renderText(
            MsdfFont font,
            Text text,
            float size,
            Matrix4f matrix,
            float x,
            float y,
            float z,
            boolean enableFadeout,
            float fadeoutStart,
            float fadeoutEnd,
            float maxWidth,
            int alpha
    ) {
        List<FormattedTextProcessor.TextSegment> segments = FormattedTextProcessor.processText(text, -1);
        renderStyledText(font, segments, size, matrix, x, y, z, enableFadeout, fadeoutStart, fadeoutEnd, maxWidth, alpha);
    }

    public void renderText(
            MsdfFont font,
            Text text,
            float size,
            Matrix4f matrix,
            float x,
            float y,
            float z,
            boolean enableFadeout,
            float fadeoutStart,
            float fadeoutEnd
    ) {
        float maxWidth = font.getWidth(text, size) * 2.0F;
        renderText(font, text, size, matrix, x, y, z, enableFadeout, fadeoutStart, fadeoutEnd, maxWidth);
    }

    private static void renderStyledText(
            MsdfFont defaultFont,
            List<FormattedTextProcessor.TextSegment> segments,
            float size,
            Matrix4f matrix,
            float x,
            float y,
            float z,
            boolean enableFadeout,
            float fadeoutStart,
            float fadeoutEnd,
            float maxWidth,
            Integer alpha
    ) {
        float thickness = 0.05f;
        float spacing = -0.3F;
        float currentX = x;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        ShaderProgram shader = RenderSystem.setShader(MSDF_FONT_SHADER_KEY);
        shader.getUniform("Thickness").set(thickness);
        shader.getUniform("Smoothness").set(0.5f);
        shader.getUniform("EnableFadeout").set(enableFadeout ? 1 : 0);
        shader.getUniform("FadeoutStart").set(fadeoutStart);
        shader.getUniform("FadeoutEnd").set(fadeoutEnd);
        shader.getUniform("MaxWidth").set(maxWidth);
        shader.getUniform("TextPosX").set(x);

        BufferBuilder builder = null;
        MsdfFont currentFont = null;

        for (FormattedTextProcessor.TextSegment segment : segments) {
            MsdfFont segmentFont = resolveSegmentFont(defaultFont, segment);
            if (currentFont != segmentFont) {
                if (builder != null) {
                    BuiltBuffer builtBuffer = builder.endNullable();
                    if (builtBuffer != null) {
                        BufferRenderer.drawWithGlobalProgram(builtBuffer);
                    }
                }

                currentFont = segmentFont;
                setShaderFont(shader, currentFont);
                builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            }

            int color = segment.color();
            if (alpha != null && alpha != 255) {
                color = (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
            }

            currentFont.applyGlyphs(
                    matrix,
                    builder,
                    segment.text(),
                    size,
                    thickness * 0.5f * size,
                    spacing,
                    currentX - 0.75F,
                    y + (size * 0.7F),
                    z,
                    color
            );

            currentX += currentFont.getWidth(segment.text(), size);
        }

        if (builder != null) {
            BuiltBuffer builtBuffer = builder.endNullable();
            if (builtBuffer != null) {
                BufferRenderer.drawWithGlobalProgram(builtBuffer);
            }
        }

        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static MsdfFont resolveSegmentFont(MsdfFont defaultFont, FormattedTextProcessor.TextSegment segment) {
        return segment.bold() ? Fonts.SFBOLD.get() : defaultFont;
    }

    private static void setShaderFont(ShaderProgram shader, MsdfFont font) {
        RenderSystem.setShaderTexture(0, font.getTextureId());
        shader.getUniform("Range").set(font.getAtlas().range());
    }
}
