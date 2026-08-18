package tech.onetap.utility.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;

public final class GlowMaskBufferSource {
    private static final int BUFFER_SIZE = 786432;
    private static final BufferAllocator ALLOCATOR = new BufferAllocator(BUFFER_SIZE);
    private static final VertexConsumerProvider.Immediate IMMEDIATE = VertexConsumerProvider.immediate(ALLOCATOR);

    private GlowMaskBufferSource() {
    }

    public static VertexConsumer getBuffer(RenderLayer layer) {
        return IMMEDIATE.getBuffer(layer);
    }

    public static VertexConsumerProvider provider() {
        return GlowMaskBufferSource::getBuffer;
    }

    public static void draw() {
        IMMEDIATE.draw();
    }
}
