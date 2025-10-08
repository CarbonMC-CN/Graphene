package net.carbonmc.graphene.mixin.chunk;

import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunkSection.class)
public abstract class LevelChunkSectionOptimizedMixin {
    @Shadow
    private short nonEmptyBlockCount;
    @Shadow
    private short tickingBlockCount;
    @Shadow
    private short tickingFluidCount;

    @Shadow
    public abstract PalettedContainer<BlockState> getStates();

    /**
     * @author CarbonMC
     * @reason Optimized block state setting for chunk generation
     */
    @Inject(method = "setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("HEAD"), cancellable = true)
    private void graphene$optimizedSetBlockState(int x, int y, int z, BlockState state, boolean checkAccess,
                                                 CallbackInfoReturnable<BlockState> cir) {
        if (!CoolConfig.XtackChunk.get()) {
            return;
        }
        if (!checkAccess) {
            PalettedContainer<BlockState> container = this.getStates();
            BlockState oldState = container.get(x, y, z);

            // 使用非检查模式设置块状态
            container.getAndSetUnchecked(x, y, z, state);

            // 手动更新计数
            this.graphene$updateBlockCounts(oldState, state);

            cir.setReturnValue(oldState);
        }
    }

    private void graphene$updateBlockCounts(BlockState oldState, BlockState newState) {
        if (!CoolConfig.XtackChunk.get()) {
            return;
        }
        FluidState oldFluid = oldState.getFluidState();
        FluidState newFluid = newState.getFluidState();

        // 更新非空方块计数
        if (oldState.isAir() && !newState.isAir()) {
            this.nonEmptyBlockCount++;
        } else if (!oldState.isAir() && newState.isAir()) {
            this.nonEmptyBlockCount--;
        }

        // 更新随机Tick方块计数
        if (oldState.isRandomlyTicking() && !newState.isRandomlyTicking()) {
            this.tickingBlockCount--;
        } else if (!oldState.isRandomlyTicking() && newState.isRandomlyTicking()) {
            this.tickingBlockCount++;
        }

        // 更新流体计数
        if (!oldFluid.isEmpty() && newFluid.isEmpty()) {
            this.tickingFluidCount--;
        } else if (oldFluid.isEmpty() && !newFluid.isEmpty()) {
            this.tickingFluidCount++;
        }
    }
}