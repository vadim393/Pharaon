package tech.onetap.event.list;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import tech.onetap.event.Event;

@Getter
@Setter
public class EventRenderEntity extends Event {
    private final EntityRenderState state;
    private final MatrixStack matrices;
    private final VertexConsumerProvider vertexConsumers;
    private final int light;
    private final EntityModel<?> model;
    private int color;

    public EventRenderEntity(EntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, EntityModel<?> model) {
        this.state = state;
        this.matrices = matrices;
        this.vertexConsumers = vertexConsumers;
        this.light = light;
        this.model = model;
        this.color = -1;
    }
}
