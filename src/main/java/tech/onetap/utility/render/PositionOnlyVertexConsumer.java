package tech.onetap.utility.render;

import net.minecraft.client.render.VertexConsumer;

public final class PositionOnlyVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private float x;
    private float y;
    private float z;
    private boolean pendingVertex;

    public PositionOnlyVertexConsumer(VertexConsumer delegate) {
        this.delegate = delegate;
    }

    public void flush() {
        if (!pendingVertex) {
            return;
        }

        delegate.vertex(x, y, z);
        pendingVertex = false;
    }

    @Override
    public void vertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
        flush();
        delegate.vertex(x, y, z);
    }

    @Override
    public VertexConsumer vertex(float x, float y, float z) {
        flush();
        this.x = x;
        this.y = y;
        this.z = z;
        this.pendingVertex = true;
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        return this;
    }

    @Override
    public VertexConsumer texture(float u, float v) {
        return this;
    }

    @Override
    public VertexConsumer overlay(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer light(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        return this;
    }
}
