package tech.onetap.util.render.renderers;

import java.util.Objects;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import tech.onetap.util.render.msdf.MsdfFont;
import tech.onetap.util.render.msdf.MsdfRenderer;
import tech.onetap.util.IMinecraft;
import tech.onetap.mixin.DrawContextAccessor;
import tech.onetap.util.render.renderers.DrawUtil;
import org.joml.Vector4f;

public class CustomDrawContext extends DrawContext implements IMinecraft {
   public CustomDrawContext(Immediate vertexConsumerProvider) {
      super(mc, vertexConsumerProvider);
   }

   public CustomDrawContext(DrawContext originalContext) {
      super(mc, ((DrawContextAccessor)originalContext).getVertexConsumers());
   }

   public static CustomDrawContext of(DrawContext originalContext) {
      return new CustomDrawContext(originalContext);
   }

   public void drawText(MsdfFont font, String text, float x, float y, int color, float fontSize) {
      MsdfRenderer.renderText(font, text, fontSize, color, this.getMatrices().peek().getPositionMatrix(), x, y, 0.0F);
   }

   public void drawText(MsdfFont font, Text text, float x, float y, float fontSize) {
      MsdfRenderer.renderText(font, text, fontSize, this.getMatrices().peek().getPositionMatrix(), x, y, 0.0F);
   }

   public void drawText(MsdfFont font, Text text, float x, float y, float fontSize, int alpha) {
      MsdfRenderer.renderText(font, text, fontSize, this.getMatrices().peek().getPositionMatrix(), x, y, 0.0F, alpha);
   }

   public void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
      DrawUtil.drawRound(x, y, width, height, radius, color);
   }

   public void drawRoundedRect(float x, float y, float width, float height, Vector4f radius, int color) {
      DrawUtil.drawRound(x, y, width, height, radius, color);
   }

   public void drawRect(float x, float y, float width, float height, int color) {
      DrawUtil.drawRect(this.getMatrices(), x, y, width, height, color);
   }

   public void drawRoundBlur(float x, float y, float width, float height, float radius, int color, float blurRadius) {
      DrawUtil.drawRoundBlur(x, y, width, height, radius, color, blurRadius);
   }
}
