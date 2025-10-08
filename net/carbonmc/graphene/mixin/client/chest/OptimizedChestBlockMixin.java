package net.carbonmc.graphene.mixin.client.chest;

import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChestBlock.class)
public abstract class OptimizedChestBlockMixin {
    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void forceStaticModel(BlockState state, CallbackInfoReturnable<RenderShape> cir) {
        if (shouldOptimize()) {
            cir.setReturnValue(RenderShape.MODEL);
        }
    }

    @Inject(method = "getTicker", at = @At("HEAD"), cancellable = true)
    private <T extends BlockEntity> void removeTicker(Level world, BlockState state, BlockEntityType<T> type,
                                                      CallbackInfoReturnable<BlockEntityTicker<T>> cir) {
        if (shouldOptimize()) {
            cir.setReturnValue(null);
        }
    }

    private boolean shouldOptimize() {
        return CoolConfig.ENABLE_OPTIMIZATION.get() &&
                CoolConfig.RENDER_MODE.get() == CoolConfig.RenderMode.SIMPLE;
    }
}