package tech.onetap.event.list;

import net.minecraft.client.util.math.MatrixStack;
import tech.onetap.event.Event;

public final class EventRender3D extends Event {
   private final MatrixStack matrix;
   private final float partialTicks;

   public EventRender3D(MatrixStack matrix, float partialTicks) {
      this.matrix = matrix;
      this.partialTicks = partialTicks;
   }

   public MatrixStack getMatrix() {
      return this.matrix;
   }

   public float getPartialTicks() {
      return this.partialTicks;
   }
}
