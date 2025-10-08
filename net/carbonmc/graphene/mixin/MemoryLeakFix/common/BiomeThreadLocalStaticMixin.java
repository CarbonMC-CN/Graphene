package net.carbonmc.graphene.mixin.MemoryLeakFix.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Supplier;

/*
 * Graphene's MemoryLeakFix
 * Biome ThreadLocal Static Cache
 * Turns the per-instance ThreadLocal<Long2FloatLinkedOpenHashMap>
 * into a single static instance, eliminating 1 ThreadLocal per Biome
 * and allowing cross-instance cache hits on the same thread.
 *
 * Applies to MC 1.14.4+ (1.15 … 1.21+).
 *
 * @author CarbonMC
 * @since 1.14.4+
 * @see net.minecraft.world.level.biome.Biome
 */
@Mixin(value = Biome.class)
public class BiomeThreadLocalStaticMixin {
    @Unique
    private static final ThreadLocal<Long2FloatLinkedOpenHashMap> graphene$STATIC_CACHE =
            ThreadLocal.withInitial(Long2FloatLinkedOpenHashMap::new);

    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/ThreadLocal;withInitial(Ljava/util/function/Supplier;)Ljava/lang/ThreadLocal;"
            )
    )
    private ThreadLocal<Long2FloatLinkedOpenHashMap> graphene$replaceWithStatic(
            Supplier<Long2FloatLinkedOpenHashMap> original, Operation<ThreadLocal<Long2FloatLinkedOpenHashMap>> op) {
        return graphene$STATIC_CACHE;
    }
}