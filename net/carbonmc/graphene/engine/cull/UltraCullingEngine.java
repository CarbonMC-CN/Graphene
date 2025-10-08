package net.carbonmc.graphene.engine.cull;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.TimeUnit;

public final class UltraCullingEngine {
    private static final Cache<Long, Boolean> CULL_CACHE = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.SECONDS)
            .build();

    public static boolean shouldCull(BlockGetter level, BlockPos pos, Direction face) {
        if (!CoolConfig.useUltraCulling()) return false;

        long cacheKey = ((long)pos.asLong() << 8) | face.ordinal();
        return CULL_CACHE.get(cacheKey, k -> {
            Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            Vec3 dir = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
            int depth = CoolConfig.ultraCullingDepth();

            BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
            for (int i = 1; i <= depth; i++) {
                mpos.set(pos.getX() + face.getStepX() * i,
                        pos.getY() + face.getStepY() * i,
                        pos.getZ() + face.getStepZ() * i);
                BlockState state = level.getBlockState(mpos);
                if (!state.canOcclude()) return false;
            }
            return true;
        });
    }
}