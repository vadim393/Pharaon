package tech.onetap.utility.render;

import net.minecraft.client.render.VertexConsumer;
import tech.onetap.util.render.providers.ColorProvider;

public final class BlockOutlineShaderVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final String shaderMode;
    private final int primaryColor;
    private final int secondaryColor;
    private int currentColor;

    public BlockOutlineShaderVertexConsumer(VertexConsumer delegate, String shaderMode, int primaryColor, int secondaryColor) {
        this.delegate = delegate;
        this.shaderMode = "Glow".equalsIgnoreCase(shaderMode) ? "Fade" : shaderMode;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.currentColor = primaryColor;
    }

    @Override
    public VertexConsumer vertex(float x, float y, float z) {
        currentColor = resolveColor(x, y, z);
        return delegate.vertex(x, y, z).color(currentColor);
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        return delegate.color(
                ColorProvider.red(currentColor),
                ColorProvider.green(currentColor),
                ColorProvider.blue(currentColor),
                ColorProvider.alpha(currentColor)
        );
    }

    @Override
    public VertexConsumer texture(float u, float v) {
        return delegate.texture(u, v);
    }

    @Override
    public VertexConsumer overlay(int u, int v) {
        return delegate.overlay(u, v);
    }

    @Override
    public VertexConsumer light(int u, int v) {
        return delegate.light(u, v);
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        return delegate.normal(x, y, z);
    }

    private int resolveColor(float x, float y, float z) {
        float time = (System.currentTimeMillis() % 1000000L) / 1000.0f;
        int alpha = Math.max(ColorProvider.alpha(primaryColor), ColorProvider.alpha(secondaryColor));

        return switch (shaderMode) {
            case "Snow" -> {
                float shimmer = (float) (Math.sin(time * 3.2f + x * 4.5f + y * 2.2f + z * 4.5f) * 0.5f + 0.5f);
                int snowy = ColorProvider.interpolateColor(primaryColor, ColorProvider.rgba(255, 255, 255, alpha), 0.65f + shimmer * 0.35f);
                yield ColorProvider.setAlpha(snowy, alpha);
            }
            case "Smoke" -> {
                int darkPrimary = ColorProvider.interpolateColor(primaryColor, ColorProvider.rgba(18, 18, 18, alpha), 0.55f);
                int darkSecondary = ColorProvider.interpolateColor(secondaryColor, ColorProvider.rgba(48, 48, 48, alpha), 0.65f);
                float smoke = (float) (Math.sin(time * 1.4f + x * 1.2f + y * 0.9f + z * 1.2f) * 0.5f + 0.5f);
                yield ColorProvider.setAlpha(ColorProvider.interpolateColor(darkPrimary, darkSecondary, smoke), alpha);
            }
            case "koka" -> {
                float mixValue = (float) (Math.sin(time * 2.4f + x * 2.0f + y * 1.6f + z * 2.0f) * 0.5f + 0.5f);
                int mixed = ColorProvider.interpolateColor(primaryColor, secondaryColor, mixValue);
                float glow = 0.18f + 0.12f * (float) (Math.sin(time * 4.1f + y * 4.0f) * 0.5f + 0.5f);
                int glowing = ColorProvider.interpolateColor(mixed, ColorProvider.rgba(255, 255, 255, alpha), glow);
                yield ColorProvider.setAlpha(glowing, alpha);
            }
            default -> {
                float fade = (float) (Math.sin(time * 2.3f + x * 2.1f + y * 1.7f + z * 2.1f) * 0.5f + 0.5f);
                yield ColorProvider.setAlpha(ColorProvider.interpolateColor(primaryColor, secondaryColor, fade), alpha);
            }
        };
    }
}
