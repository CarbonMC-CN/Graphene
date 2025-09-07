package net.carbonmc.graphene.mixin.chunk;

import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunk.class)
public abstract class ChunkEntityLoadingMixin {

    @Inject(
            method = "addEntity",
            at = @At("HEAD"),
            cancellable = true
    )
    private void graphene$deferEntityLoading(net.minecraft.world.entity.Entity entity, CallbackInfo ci) {
        // 在区块加载阶段延迟实体加载
        if (!CoolConfig.FAST_CHUNK_ENTITY.get()) {
            return;
        }
        if (((LevelChunk)(Object)this).getLevel().isClientSide()) {
            ci.cancel();
        }
    }
}