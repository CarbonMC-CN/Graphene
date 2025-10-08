package net.carbonmc.graphene.mixin.client.culling;

import net.carbonmc.graphene.engine.cull.LeafOptiEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class LeafRenderMixin {
    @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true)
    private static void onShouldRenderFace(BlockState state, BlockGetter level,
                                           BlockPos pos, Direction face,
                                           BlockPos offsetPos,
                                           CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof LeavesBlock) {
            cir.setReturnValue(!LeafOptiEngine.shouldCullFace(level, pos, face));
        }
    }
}