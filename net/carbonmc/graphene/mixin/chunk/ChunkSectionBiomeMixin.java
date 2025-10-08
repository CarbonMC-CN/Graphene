package net.carbonmc.graphene.mixin.chunk;

import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LevelChunkSection.class)
public abstract class ChunkSectionBiomeMixin {
    @Unique
    private static final int GRAPHENE_BIOME_SLICE = 4;

    @Shadow
    private PalettedContainerRO<Holder<Biome>> biomes;

    /**
     * @author CarbonMC
     * @reason Optimize biome population with better iteration order for cache locality
     */
    @Overwrite
    public void fillBiomesFromNoise(BiomeResolver biomeResolver, Climate.Sampler sampler, int sectionX, int sectionY, int sectionZ) {
        if (!CoolConfig.XtackChunk.get()) {
            return;
        }
        PalettedContainer<Holder<Biome>> newBiomes = this.biomes.recreate();

        // 优化迭代顺序：Y -> Z -> X 以提高缓存性能
        for (int localY = 0; localY < GRAPHENE_BIOME_SLICE; localY++) {
            for (int localZ = 0; localZ < GRAPHENE_BIOME_SLICE; localZ++) {
                for (int localX = 0; localX < GRAPHENE_BIOME_SLICE; localX++) {
                    Holder<Biome> biome = biomeResolver.getNoiseBiome(
                            (sectionX << 2) + localX,
                            (sectionY << 2) + localY,
                            (sectionZ << 2) + localZ,
                            sampler
                    );
                    newBiomes.getAndSetUnchecked(localX, localY, localZ, biome);
                }
            }
        }

        this.biomes = newBiomes;
    }
}