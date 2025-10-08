package net.carbonmc.graphene.mixin.client.chest;

import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderChestBlock.class)
public abstract class OptimizedEnderChestMixin {
    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void controlEnderRendering(BlockState state, CallbackInfoReturnable<RenderShape> cir) {
        if (shouldOptimizeEnder()) {
            cir.setReturnValue(RenderShape.MODEL);
        }
    }

    @Inject(method = "getTicker", at = @At("HEAD"), cancellable = true)
    private <T extends BlockEntity> void removeTicker(Level world, BlockState state, BlockEntityType<T> type,
                                                      CallbackInfoReturnable<BlockEntityTicker<T>> cir) {
        if (shouldOptimizeEnder()) {
            cir.setReturnValue(null);
        }
    }

    private boolean shouldOptimizeEnder() {
        return CoolConfig.ENABLE_OPTIMIZATION.get() &&
                CoolConfig.OPTIMIZE_ENDER_CHESTS.get() &&
                CoolConfig.RENDER_MODE.get() == CoolConfig.RenderMode.SIMPLE;
    }
}