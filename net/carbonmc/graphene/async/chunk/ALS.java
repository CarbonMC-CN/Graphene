package net.carbonmc.graphene.async.chunk;

import net.carbonmc.graphene.Graphene;
import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.CompletableFuture;

import static net.carbonmc.graphene.config.CoolConfig.CTU;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ALS {
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!CTU.get()) {
            return;
        }
        LevelAccessor levelAccessor = event.getLevel();
        if (!(levelAccessor instanceof ServerLevel level)) {
            return;
        }
        ChunkPos pos = event.getChunk().getPos();
        processChunkPostLoadNonBlocking(level, pos);
    }
    private static void processChunkPostLoadNonBlocking(ServerLevel level, ChunkPos pos) {
        if (level == null || pos == null || level.getServer().isStopped()) {
            return;
        }

        long seed = level.getSeed();

        CompletableFuture<int[]> future = Graphene.CKU.ad(
                () -> generateChunkFeatureData(seed, pos.x, pos.z)
        );
        future.thenAcceptAsync(result -> {
            cacheChunkProcessingResult(pos, result);
            level.getServer().execute(() -> quickMarkChunkForSaving(level, pos));
        }, Graphene.CKU.q).exceptionally(throwable -> {
            System.err.println("[ALS] Chunk processing failed for " + pos + ": " + throwable.getMessage());
            return null;
        });
    }
    private static void quickMarkChunkForSaving(ServerLevel level, ChunkPos pos) {
        if (level == null || pos == null || level.getServer().isStopped()) {
            return;
        }

        try {
            if (!level.hasChunk(pos.x, pos.z)) {
                return;
            }
            LevelChunk chunk = level.getChunk(pos.x, pos.z);
            if (chunk != null && !chunk.isUnsaved()) {
                chunk.setUnsaved(true);

                if (CoolConfig.DEBUG_LOGGING.get()) {
                    System.out.println("[ALS] Marked chunk for saving: " + pos);
                }
            }
        } catch (Exception e) {
            System.err.println("[ALS] Error marking chunk " + pos + ": " + e.getMessage());
        }
    }

    private static int[] generateChunkFeatureData(long seed, int chunkX, int chunkZ) {
        int[] featureData = new int[256];
        long baseState = initializeRNGState(seed, chunkX, chunkZ);
        for (int i = 0; i < 256; i++) {
            baseState = advanceRNGState(baseState);
            featureData[i] = extractFeatureValue(baseState);
        }
        return featureData;
    }
    private static long initializeRNGState(long seed, int chunkX, int chunkZ) {
        return seed ^ ((long)chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL) ^ 0x9E3779B97F4A7C15L;
    }
    private static long advanceRNGState(long state) {
        state ^= state << 21;
        state ^= state >>> 35;
        state ^= state << 4;
        return state;
    }
    private static int extractFeatureValue(long state) {
        return Long.hashCode(state) & 0x7FFFFFFF;
    }
    private static void cacheChunkProcessingResult(ChunkPos pos, int[] result) {
        if (pos != null && result != null) {
            try {
                Graphene.CHUNK_CACHE.put(pos, result);
            } catch (Exception e) {
                System.err.println("[ALS] Error caching chunk data: " + e.getMessage());
            }
        }
    }
}