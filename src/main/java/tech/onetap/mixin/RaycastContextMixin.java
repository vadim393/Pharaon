package tech.onetap.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.onetap.module.list.movement.NoWeb;

@Mixin(RaycastContext.class)
public class RaycastContextMixin {
    @Inject(
            method = "getBlockShape",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onGetBlockShape(BlockState state, BlockView world, BlockPos pos, CallbackInfoReturnable<VoxelShape> cir) {
        if (state.isOf(Blocks.COBWEB) && NoWeb.isBreakThroughActive()) {
            cir.setReturnValue(VoxelShapes.empty());
        }
    }
}
