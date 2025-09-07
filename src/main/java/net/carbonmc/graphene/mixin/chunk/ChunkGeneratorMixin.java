package net.carbonmc.graphene.mixin.chunk;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mojang.datafixers.util.Pair;
import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.concurrent.TimeUnit;
@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {

    // 结构查找结果缓存
    @Unique
    private final Cache<StructureLookupKey, Pair<BlockPos, Holder<Structure>>> structureLookupCache = Caffeine.newBuilder()
            .maximumSize(512)
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .build();

    /**
     * 优化 findNearestMapStructure 方法
     * 使用缓存减少重复的结构查找计算
     */
    @Inject(method = "findNearestMapStructure", at = @At("HEAD"), cancellable = true)
    public void onFindNearestMapStructure(ServerLevel level, HolderSet<Structure> structures, BlockPos pos,
                                          int radius, boolean skipExisting, CallbackInfoReturnable<Pair<BlockPos, Holder<Structure>>> cir) {
        if (!CoolConfig.XtackChunk_BETA.get()) {
            return;
        }
        StructureLookupKey cacheKey = new StructureLookupKey(
                level.getSeed(),
                structures.hashCode(), // 使用结构集的哈希值
                pos,
                radius,
                skipExisting
        );

        // 尝试从缓存获取
        Pair<BlockPos, Holder<Structure>> cachedResult = structureLookupCache.getIfPresent(cacheKey);
        if (cachedResult != null) {
            cir.setReturnValue(cachedResult);
            cir.cancel();
        }
    }

    /**
     * 在 findNearestMapStructure 返回时缓存结果
     */
    @Inject(method = "findNearestMapStructure", at = @At("RETURN"))
    public void onFindNearestMapStructureReturn(ServerLevel level, HolderSet<Structure> structures, BlockPos pos,
                                                int radius, boolean skipExisting, CallbackInfoReturnable<Pair<BlockPos, Holder<Structure>>> cir) {
        if (!CoolConfig.XtackChunk_BETA.get()) {
            return;
        }
        if (cir.getReturnValue() != null) {
            StructureLookupKey cacheKey = new StructureLookupKey(
                    level.getSeed(),
                    structures.hashCode(), // 使用结构集的哈希值
                    pos,
                    radius,
                    skipExisting
            );
            structureLookupCache.put(cacheKey, cir.getReturnValue());
        }
    }

    /**
     * 优化 getNearestGeneratedStructure 方法
     * 使用缓存减少重复的环形结构查找计算
     */
    @Inject(method = "getNearestGeneratedStructure", at = @At("HEAD"), cancellable = true)
    private void onGetNearestGeneratedStructure(Set<Holder<Structure>> structures, ServerLevel level,
                                                StructureManager structureManager, BlockPos pos,
                                                boolean skipExisting, ConcentricRingsStructurePlacement placement,
                                                CallbackInfoReturnable<Pair<BlockPos, Holder<Structure>>> cir) {
        if (!CoolConfig.XtackChunk_BETA.get()) {
            return;
        }
        StructureLookupKey cacheKey = new StructureLookupKey(
                level.getSeed(),
                structures.hashCode(), // 使用结构集的哈希值
                pos,
                0, // 半径不适用于环形结构
                skipExisting
        );

        // 尝试从缓存获取
        Pair<BlockPos, Holder<Structure>> cachedResult = structureLookupCache.getIfPresent(cacheKey);
        if (cachedResult != null) {
            cir.setReturnValue(cachedResult);
            cir.cancel();
        }
    }

    /**
     * 在 getNearestGeneratedStructure 返回时缓存结果
     */
    @Inject(method = "getNearestGeneratedStructure", at = @At("RETURN"))
    private void onGetNearestGeneratedStructureReturn(Set<Holder<Structure>> structures, ServerLevel level,
                                                      StructureManager structureManager, BlockPos pos,
                                                      boolean skipExisting, ConcentricRingsStructurePlacement placement,
                                                      CallbackInfoReturnable<Pair<BlockPos, Holder<Structure>>> cir) {
        if (!CoolConfig.XtackChunk_BETA.get()) {
            return;
        }
        if (cir.getReturnValue() != null) {
            StructureLookupKey cacheKey = new StructureLookupKey(
                    level.getSeed(),
                    structures.hashCode(), // 使用结构集的哈希值
                    pos,
                    0, // 半径不适用于环形结构
                    skipExisting
            );
            structureLookupCache.put(cacheKey, cir.getReturnValue());
        }
    }
}

/**
 * 结构查找缓存键类
 */
class StructureLookupKey {
    private final long seed;
    private final int structuresHash;
    private final BlockPos pos;
    private final int radius;
    private final boolean skipExisting;
    private final int hashCode;

    public StructureLookupKey(long seed, int structuresHash, BlockPos pos, int radius, boolean skipExisting) {

        this.seed = seed;
        this.structuresHash = structuresHash;
        this.pos = pos;
        this.radius = radius;
        this.skipExisting = skipExisting;
        this.hashCode = computeHashCode();
    }

    private int computeHashCode() {
        int result = (int) (seed ^ (seed >>> 32));
        result = 31 * result + structuresHash;
        result = 31 * result + pos.hashCode();
        result = 31 * result + radius;
        result = 31 * result + (skipExisting ? 1 : 0);
        return result;
    }

    @Override

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StructureLookupKey that = (StructureLookupKey) o;
        return seed == that.seed &&
                structuresHash == that.structuresHash &&
                radius == that.radius &&
                skipExisting == that.skipExisting &&
                pos.equals(that.pos);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}