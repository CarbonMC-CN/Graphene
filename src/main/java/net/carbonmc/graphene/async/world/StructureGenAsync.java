package net.carbonmc.graphene.async.world;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.carbonmc.graphene.AsyncHandler;
import net.carbonmc.graphene.async.AsyncSystemInitializer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异步结构生成系统，用于高效处理世界结构生成
 */
@AsyncHandler(threadPool = "compute", fallbackToSync = true)
public class StructureGenAsync {
    private static final Logger LOGGER = LogManager.getLogger("StructureGenAsync");

    // 配置常量
    private static final int MAX_RETRIES = 5;
    private static final int MAX_TASKS_PER_TICK = 50;
    private static final int CHUNK_LOAD_RADIUS = 2;
    private static final long TASK_TIMEOUT_MS = 30000;
    private static final int MAX_QUEUE_SIZE = 1000;
    private static final int MAX_CONCURRENT_TASKS = 200;
    private static final long CACHE_EXPIRE_MINUTES = 5;
    private static final long CACHE_MAX_SIZE = 1000;

    // 任务队列和资源控制
    private static final BlockingQueue<StructureTask> structureQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
    private static final Semaphore taskSemaphore = new Semaphore(MAX_CONCURRENT_TASKS);

    // 区块状态缓存
    private static final Cache<ChunkPosKey, Boolean> chunkStatusCache = Caffeine.newBuilder()
            .maximumSize(CACHE_MAX_SIZE)
            .expireAfterWrite(CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
            .build();

    /**
     * 初始化系统
     */
    public static void init() {
        LOGGER.info("Async Structure Generator initialized with queue size: {}, max tasks: {}",
                MAX_QUEUE_SIZE, MAX_CONCURRENT_TASKS);
    }

    /**
     * 关闭系统并清理资源
     */
    public static void shutdown() {
        structureQueue.clear();
        chunkStatusCache.invalidateAll();
        LOGGER.info("Async Structure Generator shutdown completed");
    }

    /**
     * 异步放置结构
     */
    public static CompletableFuture<Void> placeStructureAsync(ServerLevel level, StructureTemplate template, BlockPos pos) {
        // 参数验证
        if (level == null || template == null || pos == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid parameters"));
        }

        // 检查位置是否已加载
        if (!level.isLoaded(pos)) {
            LOGGER.warn("Attempted to place structure at unloaded position {}", pos);
            return CompletableFuture.failedFuture(new IllegalStateException("Position unloaded"));
        }

        // 获取任务许可
        if (!taskSemaphore.tryAcquire()) {
            LOGGER.warn("Too many concurrent structure generation tasks, skipping {}", pos);
            return CompletableFuture.failedFuture(new IllegalStateException("Too many concurrent tasks"));
        }

        // 创建并提交任务
        StructureTask task = new StructureTask(level, template, pos);
        if (!structureQueue.offer(task)) {
            taskSemaphore.release();
            LOGGER.warn("Structure queue full, skipping generation at {}", pos);
            return CompletableFuture.failedFuture(new IllegalStateException("Queue full"));
        }

        // 确保任务完成后释放许可
        return task.future().whenComplete((r, e) -> taskSemaphore.release());
    }

    /**
     * 服务器tick事件处理
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            processStructureTasks();
        }
    }

    /**
     * 处理结构生成任务
     */
    private static void processStructureTasks() {
        int processed = 0;
        while (processed < MAX_TASKS_PER_TICK && !structureQueue.isEmpty()) {
            StructureTask task = structureQueue.poll();
            if (task != null) {
                processed++;
                processSingleTask(task);
            }
        }
    }

    /**
     * 处理单个结构生成任务
     */
    private static void processSingleTask(StructureTask task) {
        AsyncSystemInitializer.getThreadPool("compute").execute(() -> {
            try {
                // 检查任务超时
                if (isTaskTimedOut(task)) {
                    handleTaskTimeout(task);
                    return;
                }

                // 检查重试次数
                if (exceededMaxRetries(task)) {
                    handleMaxRetriesExceeded(task);
                    return;
                }

                // 检查区域加载状态
                if (!isAreaReadyForGeneration(task)) {
                    retryTask(task);
                    return;
                }

                // 执行结构生成
                placeStructureSafely(task);
                task.future().complete(null);
            } catch (Exception e) {
                LOGGER.error("Failed to place structure at {}", task.pos(), e);
                task.future().completeExceptionally(e);
            }
        });
    }

    /**
     * 检查任务是否超时
     */
    private static boolean isTaskTimedOut(StructureTask task) {
        return System.currentTimeMillis() - task.creationTime() > TASK_TIMEOUT_MS;
    }

    /**
     * 处理任务超时
     */
    private static void handleTaskTimeout(StructureTask task) {
        LOGGER.error("Structure generation timed out at {}", task.pos());
        task.future().completeExceptionally(new TimeoutException("Operation timed out"));
    }

    /**
     * 检查是否超过最大重试次数
     */
    private static boolean exceededMaxRetries(StructureTask task) {
        return task.retryCount().incrementAndGet() > MAX_RETRIES;
    }

    /**
     * 处理超过最大重试次数的情况
     */
    private static void handleMaxRetriesExceeded(StructureTask task) {
        LOGGER.error("Structure generation failed after {} retries at {}", MAX_RETRIES, task.pos());
        task.future().completeExceptionally(new TimeoutException("Max retries exceeded"));
    }

    /**
     * 检查区域是否准备好生成
     */
    private static boolean isAreaReadyForGeneration(StructureTask task) {
        return isAreaLoaded(task.level(), task.pos()) && isTerrainReady(task.level(), task.pos());
    }

    /**
     * 检查区域是否已加载
     */
    private static boolean isAreaLoaded(ServerLevel level, BlockPos pos) {
        try {
            return level.isAreaLoaded(pos, CHUNK_LOAD_RADIUS);
        } catch (Exception e) {
            LOGGER.warn("Failed to check area load status at {}", pos, e);
            return false;
        }
    }

    /**
     * 检查地形是否准备好
     */
    private static boolean isTerrainReady(ServerLevel level, BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        ChunkPosKey key = new ChunkPosKey(level.dimension().location().toString(), chunkX, chunkZ);

        try {
            return chunkStatusCache.get(key, k -> checkChunkStatus(level, chunkX, chunkZ));
        } catch (Exception e) {
            LOGGER.warn("Cache lookup failed for terrain status at [{}, {}]", chunkX, chunkZ, e);
            return false;
        }
    }

    /**
     * 检查区块状态
     */
    private static boolean checkChunkStatus(ServerLevel level, int chunkX, int chunkZ) {
        try {
            return level.getChunkSource()
                    .getChunk(chunkX, chunkZ, ChunkStatus.FEATURES, false)
                    .getStatus()
                    .isOrAfter(ChunkStatus.FEATURES);
        } catch (Exception e) {
            LOGGER.warn("Failed to check terrain status at [{}, {}]", chunkX, chunkZ, e);
            return false;
        }
    }

    /**
     * 安全放置结构
     */
    private static void placeStructureSafely(StructureTask task) {
        try {
            task.template().placeInWorld(
                    task.level(),
                    task.pos(),
                    task.pos(),
                    new StructurePlaceSettings(),
                    task.level().random,
                    2
            );
            LOGGER.debug("Successfully placed structure at {}", task.pos());
        } catch (Exception e) {
            throw new RuntimeException("Failed to place structure", e);
        }
    }

    /**
     * 重试任务
     */
    private static void retryTask(StructureTask task) {
        try {
            if (!structureQueue.offer(task)) {
                LOGGER.warn("Failed to retry structure generation at {}, queue full", task.pos());
                task.future().completeExceptionally(new IllegalStateException("Queue full during retry"));
            }
        } catch (Exception e) {
            LOGGER.error("Error during task retry at {}", task.pos(), e);
            task.future().completeExceptionally(e);
        }
    }

    /**
     * 结构生成任务内部类
     */
    private static class StructureTask {
        private final ServerLevel level;
        private final StructureTemplate template;
        private final BlockPos pos;
        private final AtomicInteger retryCount = new AtomicInteger(0);
        private final long creationTime = System.currentTimeMillis();
        private final CompletableFuture<Void> future = new CompletableFuture<>();

        public StructureTask(ServerLevel level, StructureTemplate template, BlockPos pos) {
            this.level = level;
            this.template = template;
            this.pos = pos;
        }

        public ServerLevel level() { return level; }
        public StructureTemplate template() { return template; }
        public BlockPos pos() { return pos; }
        public AtomicInteger retryCount() { return retryCount; }
        public long creationTime() { return creationTime; }
        public CompletableFuture<Void> future() { return future; }
    }

    /**
     * 区块位置键内部类
     */
    private static class ChunkPosKey {
        private final String dimension;
        private final int chunkX;
        private final int chunkZ;

        public ChunkPosKey(String dimension, int chunkX, int chunkZ) {
            this.dimension = dimension;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChunkPosKey that = (ChunkPosKey) o;
            return chunkX == that.chunkX &&
                    chunkZ == that.chunkZ &&
                    dimension.equals(that.dimension);
        }

        @Override
        public int hashCode() {
            return 31 * (31 * dimension.hashCode() + chunkX) + chunkZ;
        }
    }
}