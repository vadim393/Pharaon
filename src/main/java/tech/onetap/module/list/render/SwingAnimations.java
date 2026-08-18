package tech.onetap.module.list.render;



import net.minecraft.client.util.math.MatrixStack;

import net.minecraft.entity.LivingEntity;

import net.minecraft.item.AxeItem;

import net.minecraft.item.ItemStack;

import net.minecraft.item.MiningToolItem;

import net.minecraft.item.ShovelItem;

import net.minecraft.item.SwordItem;

import net.minecraft.item.TridentItem;

import net.minecraft.item.consume.UseAction;

import net.minecraft.util.Arm;

import net.minecraft.util.math.MathHelper;

import net.minecraft.util.math.RotationAxis;

import tech.onetap.module.Module;

import tech.onetap.module.ModuleCategory;

import tech.onetap.module.ModuleInformation;

import tech.onetap.module.settings.BooleanSetting;

import tech.onetap.module.list.combat.KillAura;

import tech.onetap.module.settings.ModeSetting;

import tech.onetap.module.settings.SliderSetting;

import tech.onetap.util.base.Instance;



@ModuleInformation(moduleName = "Swing Animations", moduleCategory = ModuleCategory.RENDER)

public class SwingAnimations extends Module {

    private final ModeSetting mode = new ModeSetting("Мод", "Никакой", "Никакой", "Smooth", "Self", "Self 2", "Down", "Forward", "Touch", "BlockHit", "BlockHit2", "Pander", "Curt", "HMI");

    public final BooleanSetting onlyWithKillAura = new BooleanSetting("Только с KillAura", false);

    public final BooleanSetting onlyOnHit = new BooleanSetting("Только при ударе", false);

    private final SliderSetting power = new SliderSetting("Сила",3, 0F, 10F, 1F).setVisible(() -> !mode.getValue().equals("Pander") && !mode.is("BlockHit") && !mode.is("BlockHit2") && !mode.getValue().equals("Forward") && !mode.getValue().equals("Curt") && !mode.getValue().equals("Никакой") && !mode.is("HMI"));

    public final SliderSetting speed = new SliderSetting("Скорость",7F, 0F, 10F, 1F);

    public final SliderSetting angle = new SliderSetting("Угол",90, 0F, 360, 5F).setVisible(() -> mode.getValue().equals("Self") || mode.getValue().equals("Self 2"));

    private final ModeSetting attackMode = new ModeSetting("Атака", "Взмахи", "Взмахи", "Вперед", "Обычная").setVisible(() -> mode.is("HMI"));

    private final SliderSetting rightX = new SliderSetting("Правая X", 0.0F, -2F, 2F, 0.1F).setVisible(() -> mode.is("HMI"));

    private final SliderSetting rightY = new SliderSetting("Правая Y", 0.0F, -2F, 2F, 0.1F).setVisible(() -> mode.is("HMI"));

    private final SliderSetting rightZ = new SliderSetting("Правая Z", 0.0F, -2F, 2F, 0.1F).setVisible(() -> mode.is("HMI"));

    private final SliderSetting leftX = new SliderSetting("Левая X", 0.0F, -2F, 2F, 0.1F).setVisible(() -> mode.is("HMI"));

    private final SliderSetting leftY = new SliderSetting("Левая Y", 0.0F, -2F, 2F, 0.1F).setVisible(() -> mode.is("HMI"));

    private final SliderSetting leftZ = new SliderSetting("Левая Z", 0.0F, -2F, 2F, 0.1F).setVisible(() -> mode.is("HMI"));



    public void renderSwordAnimation(MatrixStack matrices, float swingProgress, float equipProgress, Arm arm) {

        var anim = (float) Math.sin(swingProgress * (Math.PI / 2) * 2);

        var sin2 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);

        String currentMode = mode.getValue();
        if (onlyOnHit.getValue() && swingProgress <= 0) {
            currentMode = "Никакой";
        }

        switch (currentMode) {
case "Никакой" -> {

                applyVanillaSwing(matrices, swingProgress, equipProgress, arm);

            }

            case "HMI" -> {

                renderHmiSwing(matrices, swingProgress, equipProgress, arm);

            }

            case "Smooth" -> {

                matrices.translate(0.56F, -0.52F, -0.72F);

                var swingPower = power.getValue() * 10.0F;

                var f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);

                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) (45.0F + f * (-swingPower / 4.0F))));

                var f1 = MathHelper.sin(MathHelper.sqrt(swingProgress * swingProgress) * (float) Math.PI);

                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) (sin2 * -(swingPower / 4.0F))));

                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) (sin2 * -swingPower)));

                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-45.0F));

            }

            case "Self 2" -> {

                matrices.translate(0.56F, -0.52F, -0.72F);

                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));

                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-30));

                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) (-angle.getValue() - (power.getValue() * 10) * anim)));

            }

            case "Forward" -> {

                matrices.translate(0.56F, -0.52F, -0.72F);

                matrices.translate(0, 0, -0.3 * sin2);

                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -35));

                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2 * 35));

            }

            case "Self" -> {

                matrices.translate(0.56F, -0.52F, -0.72F);

                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));

                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-60));

                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) (-angle.getValue() - (power.getValue() * 10) * anim)));

            }

            case "Down" -> {

                matrices.translate(0.56F, -0.52F - (anim * power.getValue() / 24), -0.72F);

                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));

                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-30));

                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));

            }

            case "Touch" -> {

                matrices.translate(0.56F, -0.52F, -0.72F);

                matrices.scale(1F, 1F, (float) (1 + anim * power.getValue() / 4));

                matrices.translate(0, 0, -0.265f);

                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100));

            }

            case "Curt" -> {

                matrices.translate(0.56F, -0.52F, -0.72F);



                var sqrtSwing = MathHelper.sqrt(swingProgress);

                var sinMain = MathHelper.sin(sqrtSwing * (float) Math.PI);

                var sinExtra = MathHelper.sin(swingProgress * (float) Math.PI);



                matrices.translate(0.4f - sinMain * 0.2f, -0.2f + sinMain * 0.3f, -0.5f - sinExtra * 0.2f);

                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(91));

                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40 + sinMain * -100));

                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60));

            }

            case "Pander" -> {

                matrices.translate(0.56F, -0.52F, -0.72F);

                matrices.scale(0.8f, 0.8f, 0.8f);

                var anim2 = 1.0F - MathHelper.lerp(mc.getRenderTickCounter().getTickDelta(true), mc.gameRenderer.firstPersonRenderer.prevEquipProgressMainHand, mc.gameRenderer.firstPersonRenderer.equipProgressMainHand);

                matrices.translate(0.3 - anim * 0.15f, 0.2f - anim2 * 0.12f, -0.15f - anim * 0.13f);

                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(76 - (10 * anim)));

                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-16 - (8 * anim)));

                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-83 - (26 * anim)));

            }

            case "BlockHit" -> {

                matrices.translate(0.56F, -0.52F, -0.72F);

                var f = MathHelper.sin((float) (swingProgress * swingProgress * Math.PI));

                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0f));

                var g = MathHelper.sin((float) (MathHelper.sqrt(swingProgress) * Math.PI));

                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f * -20.0f));

                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(g * -20.0f));

                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0f));

                matrices.translate(0.4f, 0.2f, 0.2f);

                matrices.translate(-0.5f, 0.08f, 0.0f);

                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(20.0F));

                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));

                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(20.0F));

            }

            case "BlockHit2" -> {
                matrices.translate(0.56F, -0.52F, -0.72F);

                if (swingProgress > 0) {
                    float f = MathHelper.sin((float) (swingProgress * swingProgress * Math.PI));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0f));
                    float h = MathHelper.sin((float) (MathHelper.sqrt(swingProgress) * Math.PI));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f * -20.0f));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(h * -20.0f));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(h * -80.0f));
                    matrices.translate(0.4f, 0.2f, 0.2f);
                    matrices.translate(-0.5f, 0.08f, 0.0f);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(20.0F));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(20.0F));
                }
            }

        }

    }

    public boolean isHmiMode() {
        return mode.is("HMI");
    }

    public void applyHmiHandPosition(MatrixStack matrices, Arm arm, boolean mainHand) {
        float side = mainHand ? 1.0F : -1.0F;
        if (mainHand) {
            matrices.translate(side * (float) rightX.getValue(), (float) rightY.getValue(), (float) rightZ.getValue());
        } else {
            matrices.translate(side * (float) leftX.getValue(), (float) leftY.getValue(), (float) leftZ.getValue());
        }
    }

    private void applyVanillaSwing(MatrixStack matrices, float swingProgress, float equipProgress, Arm arm) {
        matrices.translate(0.56F, -0.52F + equipProgress * -0.6F, -0.72F);

        float f = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        float g = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * 6.2831855F);
        float h = -0.2F * MathHelper.sin(swingProgress * 3.1415927F);
        matrices.translate(f, g, h);

        int i = arm == Arm.RIGHT ? 1 : -1;
        float f2 = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * (45.0F + f2 * -20.0F)));

        float g2 = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * g2 * -20.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g2 * -80.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * -45.0F));
    }

    private void renderHmiSwing(MatrixStack matrices, float swingProgress, float equipProgress, Arm arm) {
        matrices.translate(0.56F, -0.52F + equipProgress * -0.6F, -0.72F);

        if (mc.player == null || mc.world == null) {
            return;
        }

        boolean isMainHand = arm == mc.player.getMainArm();
        ItemStack stack = isMainHand ? mc.player.getMainHandStack() : mc.player.getOffHandStack();

        if (onlyWithKillAura.getValue()) {
            LivingEntity target = Instance.get(KillAura.class).getTarget();
            if (target == null || !target.isAlive()) {
                applyVanillaSwing(matrices, swingProgress, equipProgress, arm);
                return;
            }
        }

        float side = isMainHand ? 1.0F : -1.0F;

        float swing = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        float wave = MathHelper.sin(swingProgress * (float) Math.PI);
        boolean forward = attackMode.is("Вперед");
        boolean normal = attackMode.is("Обычная");

        UseAction action = stack.getUseAction();
        boolean sword = stack.getItem() instanceof SwordItem;
        boolean axe = stack.getItem() instanceof AxeItem;
        boolean shovel = stack.getItem() instanceof ShovelItem;
        boolean tool = stack.getItem() instanceof MiningToolItem || stack.getItem() instanceof TridentItem;

        if (sword && forward) {
            // выпад вперёд
            matrices.translate(0.12F * side * wave, 0.08F * wave, -0.7F * swing);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10.0F * side * wave));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-24.0F * wave));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-18.0F * side * wave));
        } else if (sword && normal) {
            // обычный взмах мечом
            matrices.translate(-0.35F * side * wave, -0.3F * wave, -0.5F * swing);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(60.0F * side * wave));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30.0F * swing));
            return;
        } else if ((axe || tool) && !sword) {
            // рубящий удар инструментом
            matrices.translate(0.15F * side * wave, -0.25F * wave, -0.55F * swing);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-35.0F * wave));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-15.0F * side * wave));
            return;
        } else if (shovel) {
            // копание лопатой
            matrices.translate(0.1F * side * wave, -0.2F * wave, -0.4F * swing);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30.0F * wave));
            return;
        } else if (action == UseAction.BLOCK) {
            matrices.translate(0.4F, 0.08F, 0.1F);
            return;
        }

        // взмахи по умолчанию
        matrices.translate(0.1F * side * wave, -0.2F * wave, -0.4F * swing);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30.0F * wave));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-12.0F * side * wave));
    }

}