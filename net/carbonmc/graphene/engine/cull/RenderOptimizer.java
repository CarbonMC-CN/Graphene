package net.carbonmc.graphene.engine.cull;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RenderOptimizer {
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
    private static final AtomicBoolean FALLBACK_MODE = new AtomicBoolean(false);
    private static ExecutorService tracerPool;
    private static ScheduledExecutorService timeoutChecker;
    private static ScheduledExecutorService cacheCleaner;
    private static final Cache<Integer, Boolean> ENTITY_CACHE = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(500, TimeUnit.MILLISECONDS)
            .build();

    private static final Cache<Long, Boolean> BLOCK_CACHE = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.SECONDS)
            .build();

    public static void initialize() {
        if (INITIALIZED.getAndSet(true)) return;

        int threads = Math.max(1, CoolConfig.tracingThreads.get());
        tracerPool = Executors.newWorkStealingPool(threads);
        timeoutChecker = Executors.newSingleThreadScheduledExecutor();
        cacheCleaner = Executors.newSingleThreadScheduledExecutor();
        cacheCleaner.scheduleAtFixedRate(() -> {
            if (FALLBACK_MODE.get() && System.currentTimeMillis() % 60000 < 100) {
                FALLBACK_MODE.set(false);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    public static void shutdown() {
        if (tracerPool != null) tracerPool.shutdownNow();
        if (timeoutChecker != null) timeoutChecker.shutdownNow();
        if (cacheCleaner != null) cacheCleaner.shutdownNow();
    }

    public static boolean shouldSkipEntity(Entity entity) {
        if (!CoolConfig.useAsyncTracing.get()) return false;
        if (FALLBACK_MODE.get()) return fallbackEntityCheck(entity);

        int entityId = entity.getId();
        Boolean cached = ENTITY_CACHE.getIfPresent(entityId);
        if (cached != null) {
            return cached;
        }

        Vec3 eyePos = getCameraPosition();
        Vec3 targetPos = entity.getBoundingBox().getCenter();

        boolean result = AsyncTracer.traceAsync(eyePos, targetPos, entity.level())
                .exceptionally(e -> {
                    FALLBACK_MODE.set(true);
                    return fallbackEntityCheck(entity);
                })
                .join();

        ENTITY_CACHE.put(entityId, result);
        return result;
    }

    public static boolean shouldCullBlockFace(BlockGetter level, BlockPos pos, Direction face) {
        if (!CoolConfig.useAsyncTracing.get()) return false;
        if (FALLBACK_MODE.get()) {
            return LeafOptiEngine.checkSimpleConnection(level, pos.relative(face), face);
        }

        Vec3 start = getFaceCenter(pos, face);
        Vec3 end = start.add(new Vec3(
                face.getStepX() * CoolConfig.traceDistance.get(),
                face.getStepY() * CoolConfig.traceDistance.get(),
                face.getStepZ() * CoolConfig.traceDistance.get()
        ));
        long posKey = pos.asLong();
        Boolean cached = BLOCK_CACHE.getIfPresent(posKey);
        if (cached != null) {
            return cached;
        }

        try {
            boolean result = traceAsync(start, end, level)
                    .exceptionally(e -> {
                        FALLBACK_MODE.set(true);
                        return LeafOptiEngine.checkSimpleConnection(level, pos.relative(face));
                    })
                    .getNow(false);

            BLOCK_CACHE.put(posKey, result);
            return result;
        } catch (Exception e) {
            FALLBACK_MODE.set(true);
            return LeafOptiEngine.checkSimpleConnection(level, pos.relative(face), face);
        }
    }

    private static CompletableFuture<Boolean> traceAsync(Vec3 start, Vec3 end, BlockGetter level) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Future<?> task = tracerPool.submit(() -> {
            try {
                if (Thread.interrupted()) return;
                future.complete(traceVisibility(start, end, level));
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        timeoutChecker.schedule(() -> {
            if (!future.isDone()) {
                task.cancel(true);
                future.complete(false);
            }
        }, 1, TimeUnit.MINUTES);

        return future;
    }

    private static boolean traceVisibility(Vec3 start, Vec3 end, BlockGetter level) {
        Vec3 direction = end.subtract(start);
        double distance = direction.length();
        if (distance < 0.001) return true;

        direction = direction.normalize();
        double stepSize = Math.min(0.5, distance / 8);
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        Vec3 current = start;

        while (current.distanceTo(start) < distance) {
            if (Thread.interrupted()) return false;

            mpos.set(current.x, current.y, current.z);
            BlockState state = level.getBlockState(mpos);

            if (!state.isAir() && state.getCollisionShape(level, mpos).bounds().move(mpos).contains(current)) {
                return false;
            }

            current = current.add(direction.scale(stepSize));
        }
        return true;
    }

    private static boolean fallbackEntityCheck(Entity entity) {
        double dist = getCameraPosition().distanceTo(entity.position());
        return dist > CoolConfig.fallbackDistance.get();
    }

    private static Vec3 getCameraPosition() {
        return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
    }

    private static Vec3 getFaceCenter(BlockPos pos, Direction face) {
        return new Vec3(
                pos.getX() + 0.5 + face.getStepX() * 0.501,
                pos.getY() + 0.5 + face.getStepY() * 0.501,
                pos.getZ() + 0.5 + face.getStepZ() * 0.501
        );
    }

    public static boolean isInFallbackMode() {
        return FALLBACK_MODE.get();
    }

    private static class CachedResult {
        final CompletableFuture<Boolean> future;
        final long timestamp = System.currentTimeMillis();

        CachedResult(CompletableFuture<Boolean> future) {
            this.future = future;
        }
    }
}