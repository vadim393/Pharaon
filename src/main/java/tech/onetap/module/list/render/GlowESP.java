package tech.onetap.module.list.render;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.impl.PlayerGlowFramebuffer;

@ModuleInformation(
        moduleName = "GlowESP",
        moduleDesc = "Подсветка игроков",
        moduleCategory = ModuleCategory.RENDER
)
public class GlowESP extends Module {
    private final BooleanSetting self = new BooleanSetting("Себя", true);
    private final BooleanSetting players = new BooleanSetting("Игроки", false);
    private final BooleanSetting throughWalls = new BooleanSetting("Сквозь стены", false);
    private final BooleanSetting additive = new BooleanSetting("Аддаптив", true);
    private final SliderSetting alpha = new SliderSetting("Альфа", 140.0f, 10.0f, 255.0f, 1.0f);
    private final BooleanSetting silhouette = new BooleanSetting("Силуэт", true);
    private final SliderSetting silhouetteAlpha = new SliderSetting("Альфа силуэта", 70.0f, 0.0f, 255.0f, 1.0f).setVisible(silhouette::getValue);
    private final SliderSetting radius = new SliderSetting("Радиус", 2.8f, 0.5f, 30.0f, 0.1f);
    private final SliderSetting passes = new SliderSetting("Пассы", 4.0f, 2.0f, 12.0f, 1.0f);
    private final SliderSetting strength = new SliderSetting("Сила", 1.0f, 0.35f, 2.5f, 0.05f);

    public boolean shouldCapture(PlayerEntityRenderState state) {
        if (!isEnabled() || mc.player == null || state == null || state.spectator) {
            return false;
        }

        if (state.id == mc.player.getId()) {
            return self.getValue() && !mc.options.getPerspective().isFirstPerson();
        }

        return players.getValue();
    }

    public int getCaptureColor() {
        return ColorProvider.setAlpha(ColorProvider.getThemeColor(), alpha.getIntValue());
    }

    public boolean isThroughWallsEnabled() {
        return throughWalls.getValue();
    }

    @SuppressWarnings("unchecked")
    public void capture(PlayerEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, EntityModel<?> rawModel, int light) {
        if (!shouldCapture(state)) {
            return;
        }

        if (vertexConsumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
        }

        int color = getCaptureColor();
        PlayerGlowFramebuffer.capture(color, isThroughWallsEnabled(), () -> {
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
            EntityModel<PlayerEntityRenderState> model = (EntityModel<PlayerEntityRenderState>) rawModel;
            model.render(matrices, buffer, light, OverlayTexture.DEFAULT_UV, -1);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        });
    }

    public void renderComposite() {
        if (!isEnabled()) {
            return;
        }

        float silhouetteOpacity = silhouette.getValue() ? silhouetteAlpha.getFloatValue() / 255.0f : 0.0f;
        PlayerGlowFramebuffer.renderComposite(
                radius.getFloatValue(),
                passes.getIntValue(),
                additive.getValue(),
                silhouetteOpacity,
                strength.getFloatValue()
        );
    }

    @Override
    public void onDisable() {
        super.onDisable();
        PlayerGlowFramebuffer.reset();
    }
}
