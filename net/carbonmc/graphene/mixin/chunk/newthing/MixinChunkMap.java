package net.carbonmc.graphene.mixin.chunk.newthing;

import com.mojang.datafixers.util.Either;
import net.carbonmc.graphene.optimization.chunk.ChunkOptif;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import static net.carbonmc.graphene.config.CoolConfig.CTU;
@Mixin(ChunkMap.class)
public abstract class MixinChunkMap {

    @Unique
    private boolean graphene$isFeatureEnabled() {
        return CTU.get();
    }
    @Unique
    private void graphene$beginRegionContext(ChunkPos chunkPos) {
        if (graphene$isFeatureEnabled()) {
            ChunkOptif.go(ChunkOptif.toid(chunkPos));
        }
    }
    @Unique
    private void graphene$endRegionContext() {
        if (graphene$isFeatureEnabled()) {
            ChunkOptif.to();
        }
    }
    @Inject(
            method = "prepareEntityTickingChunk",
            at = @At("RETURN"),
            remap = false
    )
    private void onFollowupTaskComplete(ChunkHolder holder, CallbackInfoReturnable<?> callbackInfo) {
        graphene$endRegionContext();
    }
    @Inject(
            method = "scheduleChunkGeneration",
            at = @At("HEAD"),
            remap = false
    )
    private void onGenerationTaskStart(ChunkHolder p_140361_, ChunkStatus p_140362_, CallbackInfoReturnable<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> cir) {
        graphene$beginRegionContext(p_140361_.getPos());
    }

    @Inject(
            method = "scheduleChunkGeneration",
            at = @At("RETURN"),
            remap = false
    )
    private void onGenerationTaskComplete(ChunkHolder p_140361_, ChunkStatus p_140362_, CallbackInfoReturnable<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> cir) {
        graphene$endRegionContext();
    }

    @Inject(
            method = "prepareEntityTickingChunk",
            at = @At("HEAD"),
            remap = false
    )
    private void onFollowupTaskStart(ChunkHolder holder, CallbackInfoReturnable<?> callbackInfo) {
        graphene$beginRegionContext(holder.getPos());
    }

}
