package tech.onetap.utility.render;

import net.minecraft.client.render.VertexConsumer;
import tech.onetap.util.render.providers.ColorProvider;

public record ColoredVertexConsumer(VertexConsumer delegate, int color) implements VertexConsumer {

    @Override
    public VertexConsumer vertex(float x, float y, float z) {
        return delegate.vertex(x, y, z).color(color);
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        return delegate.color(ColorProvider.red(color), ColorProvider.green(color), ColorProvider.blue(color), ColorProvider.alpha(color));
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
}
