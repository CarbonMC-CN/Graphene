package net.carbonmc.graphene.engine.cull;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class AsyncTracer {
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();
    private static final ExecutorService TRACER_POOL = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            r -> new Thread(r, "AsyncTracer-" + THREAD_COUNTER.incrementAndGet())
    );

    private static final Cache<Long, Boolean> TRACE_CACHE = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(500, TimeUnit.MILLISECONDS)
            .build();

    public static CompletableFuture<Boolean> traceAsync(Vec3 start, Vec3 end, BlockGetter level) {
        long cacheKey = computeTraceHash(start, end);
        Boolean cached = TRACE_CACHE.getIfPresent(cacheKey);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        TRACER_POOL.execute(() -> {
            try {
                boolean result = traceVisibility(start, end, level);
                TRACE_CACHE.put(cacheKey, result);
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private static long computeTraceHash(Vec3 start, Vec3 end) {
        return ((long)Float.floatToIntBits((float)start.x) << 48) |
                ((long)Float.floatToIntBits((float)start.y) << 32) |
                ((long)Float.floatToIntBits((float)start.z) << 16) |
                ((long)Float.floatToIntBits((float)end.x) << 12) |
                ((long)Float.floatToIntBits((float)end.y) << 8) |
                Float.floatToIntBits((float)end.z);
    }

    private static boolean traceVisibility(Vec3 start, Vec3 end, BlockGetter level) {
        Vec3 dir = end.subtract(start);
        double dist = dir.length();
        if (dist < 0.001) return true;

        dir = dir.normalize();
        double step = Math.min(0.25, dist / 20); // More precise stepping
        Vec3 current = start;
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

        while (current.distanceTo(start) < dist) {
            mpos.set(current.x, current.y, current.z);
            BlockState state = level.getBlockState(mpos);

            if (!state.isAir()) {
                VoxelShape shape = state.getCollisionShape(level, mpos);
                if (!shape.isEmpty() && shape.bounds().move(mpos).contains(current)) {
                    return false;
                }
            }
            current = current.add(dir.scale(step));
        }
        return true;
    }

    public static void shutdown() {
        TRACER_POOL.shutdownNow();
    }
}