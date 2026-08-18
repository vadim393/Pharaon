package tech.onetap.module.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import tech.onetap.event.list.EventWorldRender;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.list.combat.AntiBot;
import tech.onetap.module.list.player.FreeCamera;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ColorSetting;
import tech.onetap.module.settings.ModeListSetting;
import tech.onetap.util.base.Instance;
import tech.onetap.util.friend.FriendRepository;
import tech.onetap.util.render.lines.VertexUtil;
import tech.onetap.util.render.providers.ColorProvider;

import java.awt.*;

@ModuleInformation(moduleName = "Tracers", moduleCategory = ModuleCategory.RENDER)
public class Tracers extends Module {
    private final ModeListSetting renderTargets = new ModeListSetting("Рисовать на",
            new BooleanSetting("Игроки", true),
            new BooleanSetting("Друзья", true),
            new BooleanSetting("Голые", true),
            new BooleanSetting("Предметы", false)
    );
    private final BooleanSetting onlyWhenNotVisible = new BooleanSetting("Не в поле зрения", false);
    private final BooleanSetting onlyNetherite = new BooleanSetting("Незеритовая броня", false);
    private final BooleanSetting pinkElytra = new BooleanSetting("Розовый цвет Элитр", false);
    private final ColorSetting nakedColor = new ColorSetting("Цвет голых", ColorProvider.rgba(255, 195, 60, 255))
            .setVisible(() -> renderTargets.isEnabled("Голые"));
    private final ColorSetting itemColor = new ColorSetting("Цвет предметов", ColorProvider.rgba(170, 210, 255, 255))
            .setVisible(() -> renderTargets.isEnabled("Предметы"));

    @Subscribe
    public void onWorldRender(EventWorldRender event) {
        MatrixStack stack = event.getMatrixStack();
        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
        FreeCamera freeCamera = Instance.get(FreeCamera.class);
        AntiBot antiBot = Instance.get(AntiBot.class);
        PlayerEntity fakePlayer = freeCamera != null ? freeCamera.fakePlayer : null;

        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        Vector3f lookVec = mc.gameRenderer.getCamera().getHorizontalPlane();
        Vec3d eyePos = cameraPos.add(new Vec3d(lookVec).multiply(3));

        stack.push();
        RenderSystem.enableBlend();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
        RenderSystem.lineWidth(1f);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);

        boolean need = false;

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (entity == fakePlayer) continue;
            if (!entity.isAlive()) continue;
            if (onlyWhenNotVisible.getValue() && mc.worldRenderer.frustum.isVisible(entity.getBoundingBox())) continue;

            Vec3d targetPos;
            Color color;

            if (entity instanceof PlayerEntity player) {
                if (mc.getNetworkHandler() == null || mc.getNetworkHandler().getPlayerListEntry(player.getUuid()) == null) continue;
                if (antiBot != null && antiBot.isBot(player)) continue;

                boolean isFriend = FriendRepository.isFriend(player.getNameForScoreboard());
                boolean isNaked = isNaked(player);

                if (isFriend) {
                    if (!renderTargets.isEnabled("Друзья")) continue;
                    if (onlyNetherite.getValue() && !isWearingAnyNetherite(player)) continue;
                    color = new Color(ColorProvider.rgba(0, 255, 0, 255));
                } else if (isNaked) {
                    if (!renderTargets.isEnabled("Голые")) continue;
                    color = new Color(nakedColor.getValue());
                } else {
                    if (!renderTargets.isEnabled("Игроки")) continue;
                    if (!FriendRepository.shouldAttack(player)) continue;
                    if (onlyNetherite.getValue() && !isWearingAnyNetherite(player)) continue;
                    color = new Color(
                            pinkElytra.getValue() && isWearingElytra(player)
                                    ? ColorProvider.rgba(255, 105, 180, 255)
                                    : -1
                    );
                }

                double tx = player.prevX + (player.getX() - player.prevX) * tickDelta;
                double ty = player.prevY + (player.getY() - player.prevY) * tickDelta;
                double tz = player.prevZ + (player.getZ() - player.prevZ) * tickDelta;
                targetPos = new Vec3d(tx, ty, tz);
            } else if (entity instanceof ItemEntity itemEntity) {
                if (!renderTargets.isEnabled("Предметы")) continue;
                double tx = itemEntity.prevX + (itemEntity.getX() - itemEntity.prevX) * tickDelta;
                double ty = itemEntity.prevY + (itemEntity.getY() - itemEntity.prevY) * tickDelta + itemEntity.getHeight() * 0.5;
                double tz = itemEntity.prevZ + (itemEntity.getZ() - itemEntity.prevZ) * tickDelta;
                targetPos = new Vec3d(tx, ty, tz);
                color = new Color(itemColor.getValue());
            } else {
                continue;
            }

            VertexUtil.vertexLine(stack, buffer,
                    (float) (eyePos.x - cameraPos.x),
                    (float) (eyePos.y - cameraPos.y),
                    (float) (eyePos.z - cameraPos.z),

                    (float) (targetPos.x - cameraPos.x),
                    (float) (targetPos.y - cameraPos.y),
                    (float) (targetPos.z - cameraPos.z),
                    color
            );

            need = true;
        }

        if (need) BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.disableBlend();
        stack.pop();
    }

    private boolean isWearingAnyNetherite(PlayerEntity player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getEquippedStack(slot);
            if (isNetheriteArmor(stack)) return true;
        }
        return false;
    }

    private boolean isNaked(PlayerEntity player) {
        return player.getEquippedStack(EquipmentSlot.HEAD).isEmpty()
                && player.getEquippedStack(EquipmentSlot.CHEST).isEmpty()
                && player.getEquippedStack(EquipmentSlot.LEGS).isEmpty()
                && player.getEquippedStack(EquipmentSlot.FEET).isEmpty();
    }

    private boolean isWearingElytra(LivingEntity entity) {
        ItemStack chestStack = entity.getEquippedStack(EquipmentSlot.CHEST);
        return chestStack.isOf(Items.ELYTRA);
    }

    private boolean isNetheriteArmor(ItemStack stack) {
        return stack.isOf(Items.NETHERITE_HELMET)
                || stack.isOf(Items.NETHERITE_CHESTPLATE)
                || stack.isOf(Items.NETHERITE_LEGGINGS)
                || stack.isOf(Items.NETHERITE_BOOTS);
    }

}
