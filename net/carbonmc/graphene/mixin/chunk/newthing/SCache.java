package net.carbonmc.graphene.mixin.chunk.newthing;

import net.carbonmc.graphene.optimization.chunk.ChunkOptif;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

import static net.carbonmc.graphene.config.CoolConfig.CTU;

@Mixin(ServerChunkCache.class)
public abstract class SCache {

    @Unique
    private long graphene$getRegionIdFromCoords(int x, int z) {
        try {
            ChunkPos chunkPos = new ChunkPos(x, z);
            long regionId = ChunkOptif.toid(chunkPos);

            // 添加验证
            if (regionId == 0 || regionId == -1) {
            //    System.err.println("Invalid region ID for coordinates (" + x + ", " + z + "): " + regionId);
                return 0L; // 返回0表示无效，避免后续处理
            }

            return regionId;
        } catch (Exception e) {
            //System.err.println("Error calculating region ID for (" + x + ", " + z + "): " + e.getMessage());
            return 0L;
        }
    }

    @Unique
    private void graphene$manageRegionContext(int x, int z, boolean begin) {
        long regionId = graphene$getRegionIdFromCoords(x, z);

        // 只在regionId有效时管理上下文
        if (regionId != 0 && regionId != -1) {
            if (begin) {
                ChunkOptif.go(regionId);
            } else {
                ChunkOptif.to();
            }
        }
    }

    @Inject(
            method = "getChunkFuture(IILnet/minecraft/world/level/chunk/ChunkStatus;Z)Ljava/util/concurrent/CompletableFuture;",
            at = @At("HEAD"),
            require = 0
    )
    private void onChunkFutureBegin(int x, int z, ChunkStatus status, boolean load, CallbackInfoReturnable<CompletableFuture<?>> info) {
        if (CTU.get()) {
            graphene$manageRegionContext(x, z, true);
        }
    }

    @Inject(
            method = "getChunk(IILnet/minecraft/world/level/chunk/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;",
            at = @At("HEAD"),
            require = 0
    )
    private void onChunkAccessBegin(int x, int z, ChunkStatus status, boolean load, CallbackInfoReturnable<?> info) {
        if (CTU.get()) {
            graphene$manageRegionContext(x, z, true);
        }
    }

    @Inject(
            method = "getChunkNow(II)Lnet/minecraft/world/level/chunk/LevelChunk;",
            at = @At("HEAD"),
            require = 0
    )
    private void onChunkNowBegin(int x, int z, CallbackInfoReturnable<?> info) {
        if (CTU.get()) {
            graphene$manageRegionContext(x, z, true);
        }
    }

    @Inject(
            method = "getChunk(IILnet/minecraft/world/level/chunk/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;",
            at = @At("RETURN"),
            require = 0
    )
    private void onChunkAccessComplete(int x, int z, ChunkStatus status, boolean load, CallbackInfoReturnable<?> info) {
        if (CTU.get()) {
            graphene$manageRegionContext(x, z, false);
        }
    }

    @Inject(
            method = "getChunkNow(II)Lnet/minecraft/world/level/chunk/LevelChunk;",
            at = @At("RETURN"),
            require = 0
    )
    private void onChunkNowComplete(int x, int z, CallbackInfoReturnable<?> info) {
        if (CTU.get()) {
            graphene$manageRegionContext(x, z, false);
        }
    }

    @Inject(
            method = "getChunkFuture(IILnet/minecraft/world/level/chunk/ChunkStatus;Z)Ljava/util/concurrent/CompletableFuture;",
            at = @At("RETURN"),
            require = 0
    )
    private void onChunkFutureComplete(int x, int z, ChunkStatus status, boolean load, CallbackInfoReturnable<CompletableFuture<?>> info) {
        if (CTU.get()) {
            graphene$manageRegionContext(x, z, false);
        }
    }
}
