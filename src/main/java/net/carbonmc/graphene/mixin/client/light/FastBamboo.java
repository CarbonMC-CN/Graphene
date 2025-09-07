package net.carbonmc.graphene.mixin.client.light;

import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 跳过比MOJANG那命时间还长的竹子光照计算。
 */
@Mixin(net.minecraft.world.level.block.state.BlockBehaviour.class)
public abstract class FastBamboo {

    @Inject(
            method = "getShadeBrightness",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onGetShadeBrightness(BlockState state,
                                      BlockGetter level,
                                      BlockPos pos,
                                      CallbackInfoReturnable<Float> cir) {
        if (!CoolConfig.BambooLight.get()) {
            return;
        }
        if (state.getBlock() instanceof BambooStalkBlock) {
            cir.setReturnValue(1.0F);
        }
    }
}