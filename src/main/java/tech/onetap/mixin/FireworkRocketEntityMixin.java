package tech.onetap.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import tech.onetap.event.list.FireworkEvent;

@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin {
    @Shadow
    public LivingEntity shooter;

    @ModifyConstant(method = "tick", constant = @Constant(doubleValue = 1.5D))
    private double replaceSpeed(double original) {
        var event = new FireworkEvent(shooter, (float) original);
        event.post();
        return event.getSpeed();
    }
}
