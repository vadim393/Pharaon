package tech.onetap.mixin;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.onetap.module.list.render.NoRender;
import tech.onetap.util.base.Instance;

@Mixin(AbstractBlock.AbstractBlockState.class)
public class GrassBlockStateMixin {
    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void onGetRenderType(CallbackInfoReturnable<BlockRenderType> cir) {
        NoRender noRender = Instance.get(NoRender.class);
        if (!noRender.isEnabled() || !noRender.elements.isEnabled("Трава")) {
            return;
        }

        BlockState state = (BlockState)(Object)this;
        if (state.isOf(Blocks.SHORT_GRASS)
                || state.isOf(Blocks.TALL_GRASS)
                || state.isOf(Blocks.FERN)
                || state.isOf(Blocks.LARGE_FERN)
                || state.isOf(Blocks.DEAD_BUSH)
                || state.isOf(Blocks.SEAGRASS)
                || state.isOf(Blocks.TALL_SEAGRASS)) {
            cir.setReturnValue(BlockRenderType.INVISIBLE);
        }
    }
}
