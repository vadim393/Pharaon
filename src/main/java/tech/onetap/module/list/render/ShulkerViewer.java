package tech.onetap.module.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.util.math.Vector2f;
import tech.onetap.event.list.EventHUD;
import tech.onetap.event.list.EventHandledScreen;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeListSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.render.math.ProjectionUtil;
import tech.onetap.util.render.renderers.DrawUtil;
import tech.onetap.util.render.providers.ColorProvider;

@ModuleInformation(
        moduleName = "ShulkerViewer",
        moduleCategory = ModuleCategory.RENDER,
        moduleDesc = "Просмотр содержимого шалкера"
)
public final class ShulkerViewer extends Module {

    private final SliderSetting size = new SliderSetting("Размер", 0.5, 0.3, 1.0, 0.1);
    private final ModeListSetting showIf = new ModeListSetting(
            "Отображать когда",
            new BooleanSetting("В руке (другие)", true),
            new BooleanSetting("В руке (себя)", true),
            new BooleanSetting("На земле", true),
            new BooleanSetting("В инвентаре", true)
    );

    private static final Identifier SHULKER_GUI_TEXTURE = Identifier.of("minecraft", "textures/gui/container/shulker_box.png");

    public ShulkerViewer() {
    }

    @Subscribe
    public void onRenderHUD(EventHUD e) {
        if (!this.isEnabled()) return;
        if (mc.player == null || mc.world == null) return;

        if (showIf.isEnabled("В руке (другие)")) {
            handleHandRenderOthers(e);
        }
        if (showIf.isEnabled("В руке (себя)")) {
            handleHandRenderSelf(e);
        }
        if (showIf.isEnabled("На земле")) {
            handleGroundRender(e);
        }
    }

    @Subscribe
    public void onRenderInventory(EventHandledScreen e) {
        if (!this.isEnabled() || !showIf.isEnabled("В инвентаре")) return;

        if (org.lwjgl.glfw.GLFW.glfwGetKey(mc.getWindow().getHandle(), org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) != org.lwjgl.glfw.GLFW.GLFW_PRESS
            && org.lwjgl.glfw.GLFW.glfwGetKey(mc.getWindow().getHandle(), org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) != org.lwjgl.glfw.GLFW.GLFW_PRESS) {
            return;
        }

        Slot slot = e.getSlotHover();
        if (slot != null && slot.hasStack()) {
            ItemStack stack = slot.getStack();
            if (isShulkerBox(stack)) {
                int screenWidth = mc.getWindow().getScaledWidth();
                int screenHeight = mc.getWindow().getScaledHeight();
                int x = (int) (screenWidth / 2 - 176 * 0.85f / 2);
                int y = screenHeight / 2 - 170;
                renderInventoryTooltipCentered(e.getContext(), stack, x, y, 0.85f);
            }
        }
    }

    private void renderInventoryTooltipCentered(DrawContext context, ItemStack stack, int x, int y, float scale) {
        DefaultedList<ItemStack> items = getShulkerItems(stack);
        if (items == null) return;

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x, y, 500);
        matrices.scale(scale, scale, 1.0f);
        matrices.translate(-x, -y, 0);

        float[] color = getShulkerColor(stack);
        RenderSystem.setShaderColor(color[0], color[1], color[2], 1.0F);

        context.drawTexture(RenderLayer::getGuiTextured, SHULKER_GUI_TEXTURE, x, y, 0, 0, 176, 17, 256, 256);
        context.drawTexture(RenderLayer::getGuiTextured, SHULKER_GUI_TEXTURE, x, y + 17, 0, 17, 176, 54, 256, 256);
        context.drawTexture(RenderLayer::getGuiTextured, SHULKER_GUI_TEXTURE, x, y + 17 + 54, 0, 160, 176, 7, 256, 256);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        for (int i = 0; i < items.size(); i++) {
            ItemStack item = items.get(i);
            if (!item.isEmpty()) {
                int slotX = x + 8 + (i % 9) * 18;
                int slotY = y + 18 + (i / 9) * 18;
                drawItemWithOverlayCount(context, item, slotX, slotY);
            }
        }

        matrices.pop();
    }

    private void renderInventoryTooltip(DrawContext context, ItemStack stack, int mouseX, int mouseY) {
        DefaultedList<ItemStack> items = getShulkerItems(stack);
        if (items == null) return;

        int x = mouseX + 12;
        int y = mouseY - 12;
        
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(0, 0, 500);

        float[] color = getShulkerColor(stack);
        RenderSystem.setShaderColor(color[0], color[1], color[2], 1.0F);

        context.drawTexture(RenderLayer::getGuiTextured, SHULKER_GUI_TEXTURE, x, y, 0, 0, 176, 17, 256, 256);
        context.drawTexture(RenderLayer::getGuiTextured, SHULKER_GUI_TEXTURE, x, y + 17, 0, 17, 176, 54, 256, 256);
        context.drawTexture(RenderLayer::getGuiTextured, SHULKER_GUI_TEXTURE, x, y + 17 + 54, 0, 160, 176, 7, 256, 256);
        
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        for (int i = 0; i < items.size(); i++) {
            ItemStack item = items.get(i);
            if (!item.isEmpty()) {
                int slotX = x + 8 + (i % 9) * 18;
                int slotY = y + 18 + (i / 9) * 18;
                drawItemWithOverlayCount(context, item, slotX, slotY);
            }
        }

        matrices.pop();
    }

    private void handleHandRenderOthers(EventHUD e) {
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player) {
                ItemStack stack = player.getMainHandStack();
                if (!stack.isEmpty()) {
                    double x = MathHelper.lerp(e.getRenderTickCounter().getTickDelta(true), player.lastRenderX, player.getX());
                    double y = MathHelper.lerp(e.getRenderTickCounter().getTickDelta(true), player.lastRenderY, player.getY());
                    double z = MathHelper.lerp(e.getRenderTickCounter().getTickDelta(true), player.lastRenderZ, player.getZ());
                    renderShulkerContents(e, stack, x, y + player.getHeight() + 0.5, z);
                }
            }
        }
    }

    private void handleHandRenderSelf(EventHUD e) {
        ItemStack stack = mc.player.getMainHandStack();
        if (!stack.isEmpty() && isShulkerBox(stack)) {
            double x = MathHelper.lerp(e.getRenderTickCounter().getTickDelta(true), mc.player.lastRenderX, mc.player.getX());
            double y = MathHelper.lerp(e.getRenderTickCounter().getTickDelta(true), mc.player.lastRenderY, mc.player.getY());
            double z = MathHelper.lerp(e.getRenderTickCounter().getTickDelta(true), mc.player.lastRenderZ, mc.player.getZ());
            renderShulkerContents(e, stack, x, y + mc.player.getHeight() + 1.2, z);
        }
    }

    private void handleGroundRender(EventHUD e) {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ItemEntity itemEntity) {
                ItemStack stack = itemEntity.getStack();
                if (!stack.isEmpty()) {
                    double x = MathHelper.lerp(e.getRenderTickCounter().getTickDelta(true), entity.lastRenderX, entity.getX());
                    double y = MathHelper.lerp(e.getRenderTickCounter().getTickDelta(true), entity.lastRenderY, entity.getY()) + 1.3;
                    double z = MathHelper.lerp(e.getRenderTickCounter().getTickDelta(true), entity.lastRenderZ, entity.getZ());
                    renderShulkerContents(e, stack, x, y, z);
                }
            }
        }
    }

    private void renderShulkerContents(EventHUD e, ItemStack stack, double x, double y, double z) {
        if (!isShulkerBox(stack)) return;

        DefaultedList<ItemStack> items = getShulkerItems(stack);
        if (items == null) return;

        Vector2f screenPos = ProjectionUtil.project(x, y, z);
        if (screenPos.getX() == Float.MAX_VALUE || screenPos.getY() == Float.MAX_VALUE) return;

        double distance = mc.player.getEyePos().distanceTo(new Vec3d(x, y, z));
        float scale = (float) (MathHelper.clamp(1.0D - (distance / 20.0D), 0.3D, 1.0D) * size.getFloatValue());

        MatrixStack matrices = e.getDrawContext().getMatrices();
        matrices.push();
        matrices.translate(screenPos.getX(), screenPos.getY(), 500.0F);
        matrices.scale(scale, scale, 1.0F);

        float[] color = getShulkerColor(stack);
        RenderSystem.setShaderColor(color[0], color[1], color[2], 0.8F);
        
        int width = 176;
        int height = 72;
        e.getDrawContext().drawTexture(RenderLayer::getGuiTextured, SHULKER_GUI_TEXTURE, -88, -36, 0, 0, width, 17, 256, 256);
        e.getDrawContext().drawTexture(RenderLayer::getGuiTextured, SHULKER_GUI_TEXTURE, -88, -19, 0, 17, width, 54, 256, 256);
        e.getDrawContext().drawTexture(RenderLayer::getGuiTextured, SHULKER_GUI_TEXTURE, -88, 35, 0, 160, width, 7, 256, 256);
        
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        for (int i = 0; i < items.size(); i++) {
            ItemStack item = items.get(i);
            if (!item.isEmpty()) {
                int slotX = -80 + (i % 9) * 18;
                int slotY = -18 + (i / 9) * 18;
                drawItemWithOverlayCount(e.getDrawContext(), item, slotX, slotY);
            }
        }

        matrices.pop();
    }

    private float[] getShulkerColor(ItemStack stack) {
        Block block = Block.getBlockFromItem(stack.getItem());
        if (block instanceof ShulkerBoxBlock shulker) {
            net.minecraft.util.DyeColor color = shulker.getColor();
            if (color != null) {
                int colorValue = color.getEntityColor();
                return new float[]{
                    (float) (colorValue >> 16 & 255) / 255.0F,
                    (float) (colorValue >> 8 & 255) / 255.0F,
                    (float) (colorValue & 255) / 255.0F
                };
            }
        }
        return new float[]{0.537f, 0.341f, 0.898f};
    }

    @SuppressWarnings("deprecation")
    private DefaultedList<ItemStack> getShulkerItems(ItemStack stack) {
        ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
        if (container != null) {
            DefaultedList<ItemStack> items = DefaultedList.ofSize(27, ItemStack.EMPTY);
            container.copyTo(items);
            return items;
        }

        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) return null;

        NbtCompound nbt = customData.getNbt();
        if (!nbt.contains("BlockEntityTag", 10)) return null;

        NbtCompound blockEntityTag = nbt.getCompound("BlockEntityTag");
        if (!blockEntityTag.contains("Items", 9)) return null;

        DefaultedList<ItemStack> items = DefaultedList.ofSize(27, ItemStack.EMPTY);
        NbtList itemsList = blockEntityTag.getList("Items", 10);

        for (int i = 0; i < itemsList.size(); i++) {
            NbtCompound itemTag = itemsList.getCompound(i);
            int slot = itemTag.getByte("Slot") & 255;
            if (slot >= 0 && slot < items.size()) {
                if (mc.world != null && mc.world.getRegistryManager() != null) {
                    items.set(slot, ItemStack.fromNbtOrEmpty(mc.world.getRegistryManager(), itemTag));
                } else {
                    items.set(slot, ItemStack.EMPTY);
                }
            }
        }

        return items;
    }

    private void drawItemWithOverlayCount(DrawContext context, ItemStack item, int x, int y) {
        context.drawItem(item, x, y);
        String countText = item.getCount() > 1 ? String.valueOf(item.getCount()) : null;
        context.drawStackOverlay(mc.textRenderer, item, x, y, countText);
    }

    private boolean isShulkerBox(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return Block.getBlockFromItem(stack.getItem()) instanceof ShulkerBoxBlock;
    }
}
