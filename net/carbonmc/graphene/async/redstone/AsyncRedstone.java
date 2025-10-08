package net.carbonmc.graphene.async.redstone;

import net.carbonmc.graphene.async.AsyncSystemInitializer;
import net.carbonmc.graphene.event.AsyncHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@AsyncHandler(threadPool = "compute", fallbackToSync = true)
public class AsyncRedstone {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final BlockingQueue<RedstoneTask> redstoneQueue = new LinkedBlockingQueue<>(1000);
    private static final Map<BlockPos, RedstoneSnapshot> activeTasks = new ConcurrentHashMap<>();
    private static final int MAX_SNAPSHOT_AGE_MS = 100;
    private static final int MAX_UPDATES_PER_TICK = 100;
    private static final int REDSTONE_RADIUS = 10;
    private static final Semaphore taskSemaphore = new Semaphore(500);

    public static void init() {
        LOGGER.info("Async Redstone System initialized");
    }

    public static void shutdown() {
        redstoneQueue.clear();
        activeTasks.clear();
        LOGGER.info("Async Redstone System shutdown completed");
    }

    public static void computeRedstoneAsync(ServerLevel level, BlockPos sourcePos) {
        if (!level.isLoaded(sourcePos)) {
            LOGGER.warn("Attempted redstone computation at unloaded position {}", sourcePos);
            return;
        }

        if (activeTasks.containsKey(sourcePos)) {
            LOGGER.debug("Redstone computation already in progress for {}", sourcePos);
            return;
        }

        if (!taskSemaphore.tryAcquire()) {
            LOGGER.warn("Too many concurrent redstone computations, skipping {}", sourcePos);
            return;
        }

        try {
            RedstoneSnapshot snapshot = new RedstoneSnapshot(level, sourcePos);
            if (!snapshot.isValid()) {
                LOGGER.warn("Failed to create valid snapshot for {}", sourcePos);
                return;
            }

            RedstoneTask task = new RedstoneTask(snapshot);
            if (redstoneQueue.offer(task)) {
                activeTasks.put(sourcePos, snapshot);
            } else {
                LOGGER.warn("Redstone queue full, computation skipped for {}", sourcePos);
                taskSemaphore.release();
            }
        } catch (Exception e) {
            LOGGER.error("Error creating redstone snapshot for {}", sourcePos, e);
            taskSemaphore.release();
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            processRedstoneTasks();
            applyRedstoneUpdates();
        }
    }

    private static void processRedstoneTasks() {
        int processed = 0;
        while (processed < MAX_UPDATES_PER_TICK && !redstoneQueue.isEmpty()) {
            RedstoneTask task = redstoneQueue.poll();
            if (task != null) {
                processed++;
                AsyncSystemInitializer.getThreadPool("compute").execute(() -> {
                    try {
                        processRedstoneTask(task);
                    } finally {
                        taskSemaphore.release();
                    }
                });
            }
        }
    }

    private static void processRedstoneTask(RedstoneTask task) {
        try {
            if (!task.snapshot().isStillValid()) {
                LOGGER.debug("Redstone snapshot expired during computation");
                return;
            }

            Map<BlockPos, Integer> powerLevels = new HashMap<>();
            Deque<BlockPos> queue = new ArrayDeque<>();
            queue.add(task.snapshot().sourcePos());

            while (!queue.isEmpty()) {
                BlockPos current = queue.poll();
                int power = calculatePower(task.snapshot(), current);
                powerLevels.put(current, power);
                task.results().add(new RedstoneResult(current, power));

                for (BlockPos neighbor : getNeighbors(current)) {
                    if (!powerLevels.containsKey(neighbor) &&
                            task.snapshot().containsPosition(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }

            task.completed().set(true);
        } catch (Exception e) {
            LOGGER.error("Redstone computation failed", e);
            task.error().set(e);
        }
    }

    private static void applyRedstoneUpdates() {
        Iterator<Map.Entry<BlockPos, RedstoneSnapshot>> it = activeTasks.entrySet().iterator();
        int updatesApplied = 0;

        while (it.hasNext() && updatesApplied < MAX_UPDATES_PER_TICK) {
            Map.Entry<BlockPos, RedstoneSnapshot> entry = it.next();
            RedstoneSnapshot snapshot = entry.getValue();

            for (RedstoneTask task : snapshot.assignedTasks()) {
                if (task.completed().get() && updatesApplied < MAX_UPDATES_PER_TICK) {
                    if (task.error().get() == null && snapshot.isStillValid()) {
                        for (RedstoneResult result : task.results()) {
                            if (updatesApplied++ >= MAX_UPDATES_PER_TICK) break;

                            BlockPos pos = result.pos();
                            if (snapshot.level().isLoaded(pos)) {
                                try {
                                    snapshot.level().updateNeighborsAt(pos,
                                            snapshot.level().getBlockState(pos).getBlock());
                                } catch (Exception e) {
                                    LOGGER.error("Failed to update redstone at {}", pos, e);
                                }
                            }
                        }
                        LOGGER.debug("Applied {} redstone updates", task.results().size());
                    }
                    it.remove();
                    break;
                }
            }
        }
    }

    private static int calculatePower(RedstoneSnapshot snapshot, BlockPos pos) {
        try {
            BlockState state = snapshot.getBlockState(pos);
            return state != null ? state.getSignal(snapshot.level(), pos, null) : 0;
        } catch (Exception e) {
            LOGGER.error("Error calculating power at {}", pos, e);
            return 0;
        }
    }

    private static List<BlockPos> getNeighbors(BlockPos pos) {
        return List.of(
                pos.above(), pos.below(),
                pos.north(), pos.south(),
                pos.east(), pos.west()
        );
    }

    private static class RedstoneSnapshot {
        private final ServerLevel level;
        private final BlockPos sourcePos;
        private final Map<BlockPos, BlockState> stateMap = new ConcurrentHashMap<>();
        private final Set<RedstoneTask> tasks = ConcurrentHashMap.newKeySet();
        private final long creationTime;

        public RedstoneSnapshot(ServerLevel level, BlockPos sourcePos) {
            this.level = level;
            this.sourcePos = sourcePos;
            this.creationTime = System.nanoTime();

            for (int x = -REDSTONE_RADIUS; x <= REDSTONE_RADIUS; x++) {
                for (int y = -REDSTONE_RADIUS; y <= REDSTONE_RADIUS; y++) {
                    for (int z = -REDSTONE_RADIUS; z <= REDSTONE_RADIUS; z++) {
                        BlockPos pos = sourcePos.offset(x, y, z);
                        if (level.isLoaded(pos)) {
                            stateMap.put(pos, level.getBlockState(pos));
                        }
                    }
                }
            }
        }

        public boolean isValid() {
            return !stateMap.isEmpty();
        }

        public boolean isStillValid() {
            return System.nanoTime() - creationTime < TimeUnit.MILLISECONDS.toNanos(MAX_SNAPSHOT_AGE_MS);
        }

        public boolean containsPosition(BlockPos pos) {
            return stateMap.containsKey(pos);
        }

        public BlockState getBlockState(BlockPos pos) {
            return stateMap.get(pos);
        }

        public void assignTask(RedstoneTask task) {
            tasks.add(task);
        }

        public Set<RedstoneTask> assignedTasks() {
            return tasks;
        }

        public ServerLevel level() { return level; }
        public BlockPos sourcePos() { return sourcePos; }
    }

    private static class RedstoneTask {
        private final RedstoneSnapshot snapshot;
        private final List<RedstoneResult> results = new CopyOnWriteArrayList<>();
        private final AtomicBoolean completed = new AtomicBoolean(false);
        private final AtomicReference<Exception> error = new AtomicReference<>();

        public RedstoneTask(RedstoneSnapshot snapshot) {
            this.snapshot = snapshot;
            snapshot.assignTask(this);
        }

        public RedstoneSnapshot snapshot() { return snapshot; }
        public List<RedstoneResult> results() { return results; }
        public AtomicBoolean completed() { return completed; }
        public AtomicReference<Exception> error() { return error; }
    }

    private record RedstoneResult(BlockPos pos, int power) {}
}