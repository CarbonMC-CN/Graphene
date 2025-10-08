package net.carbonmc.graphene.mixin.server;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Mixin(WorldGenRegion.class)
public abstract class MixinWorldGenRegion {

    @Shadow
    private List<ChunkAccess> cache;
    @Shadow
    private int size;
    @Shadow
    private ChunkPos firstPos;
    @Shadow
    private ChunkPos lastPos;

    private Cache<Long, ChunkAccess> chunkCache;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {

        this.chunkCache = Caffeine.newBuilder()
                .maximumSize(256)
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .build();
    }

    @Overwrite
    public ChunkAccess getChunk(int chunkX, int chunkZ) {
        long key = ((long)chunkX << 32) | (chunkZ & 0xFFFFFFFFL);

        ChunkAccess cachedChunk = chunkCache.getIfPresent(key);
        if (cachedChunk != null) {
            return cachedChunk;
        }

        if (chunkX < firstPos.x || chunkX > lastPos.x || chunkZ < firstPos.z || chunkZ > lastPos.z) {
            throw new RuntimeException(String.format(
                    "We are asking MixinRandomA chunk for MixinRandomA chunk out of bound | %s %s", chunkX, chunkZ));
        }

        int index = (chunkX - firstPos.x) + (chunkZ - firstPos.z) * size;
        ChunkAccess chunk = cache.get(index);

        chunkCache.put(key, chunk);

        return chunk;
    }

    @Overwrite
    public BlockState getBlockState(BlockPos pos) {
        int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        return getChunk(chunkX, chunkZ).getBlockState(pos);
    }

    @Overwrite
    public FluidState getFluidState(BlockPos pos) {
        int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        return getChunk(chunkX, chunkZ).getFluidState(pos);
    }
}