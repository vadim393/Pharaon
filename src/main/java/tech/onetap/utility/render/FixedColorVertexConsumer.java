package tech.onetap.utility.render;

import net.minecraft.client.render.VertexConsumer;

public final class FixedColorVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final int color;
    private final int red;
    private final int green;
    private final int blue;
    private final int alpha;

    public FixedColorVertexConsumer(VertexConsumer delegate, int color) {
        this.delegate = delegate;
        this.color = color;
        this.red = (color >> 16) & 0xFF;
        this.green = (color >> 8) & 0xFF;
        this.blue = color & 0xFF;
        this.alpha = (color >> 24) & 0xFF;
    }

    @Override
    public VertexConsumer vertex(float x, float y, float z) {
        delegate.vertex(x, y, z);
        delegate.color(this.red, this.green, this.blue, this.alpha);
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        delegate.color(this.red, this.green, this.blue, this.alpha);
        return this;
    }

    @Override
    public VertexConsumer texture(float u, float v) {
        delegate.texture(u, v);
        return this;
    }

    @Override
    public VertexConsumer overlay(int u, int v) {
        delegate.overlay(u, v);
        return this;
    }

    @Override
    public VertexConsumer light(int u, int v) {
        delegate.light(u, v);
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        delegate.normal(x, y, z);
        return this;
    }

    @Override
    public void vertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
        delegate.vertex(x, y, z, this.color, u, v, overlay, light, normalX, normalY, normalZ);
    }
}
