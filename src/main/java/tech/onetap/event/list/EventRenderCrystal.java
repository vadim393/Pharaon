package tech.onetap.event.list;

import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import tech.onetap.event.Event;

public class EventRenderCrystal extends Event {
    private final EndCrystalEntityRenderState state;
    private final MatrixStack matrixStack;
    private final int light;
    private int color = -1;

    public EventRenderCrystal(EndCrystalEntityRenderState state, MatrixStack matrixStack, int light) {
        this.state = state;
        this.matrixStack = matrixStack;
        this.light = light;
    }

    public EndCrystalEntityRenderState getState() {
        return state;
    }

    public MatrixStack getMatrixStack() {
        return matrixStack;
    }

    public int getLight() {
        return light;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
