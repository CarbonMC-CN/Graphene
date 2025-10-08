package net.carbonmc.graphene.async.entity;

import net.carbonmc.graphene.async.AsyncSystemInitializer;
import net.carbonmc.graphene.event.AsyncHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

@AsyncHandler(threadPool = "compute", fallbackToSync = true)
public class AsyncAIManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<PathfinderMob, PathTask> pathfindingTasks = new ConcurrentHashMap<>();
    private static final int MAX_CONCURRENT_TASKS = 1000;
    private static final Semaphore taskSemaphore = new Semaphore(MAX_CONCURRENT_TASKS);

    public static void init() {
        LOGGER.info("Async AI Manager initialized");
    }

    public static void shutdown() {
        pathfindingTasks.clear();
        LOGGER.info("Async AI Manager shutdown");
    }

    public static void computePathAsync(PathfinderMob mob, BlockPos target) {
        if (!mob.level().isLoaded(target)) {
            LOGGER.warn("Attempted to pathfind to unloaded position: {}", target);
            return;
        }

        if (pathfindingTasks.containsKey(mob)) {
            LOGGER.debug("Pathfinding already in progress for {}", mob.getName().getString());
            return;
        }

        if (!taskSemaphore.tryAcquire()) {
            LOGGER.warn("Too many concurrent pathfinding tasks, skipping for {}", mob.getName().getString());
            return;
        }

        PathTask task = new PathTask(mob, target);
        pathfindingTasks.put(mob, task);

        AsyncSystemInitializer.getThreadPool("compute").execute(() -> {
            try {
                if (!mob.isAlive() || mob.isRemoved()) {
                    LOGGER.debug("Entity removed during pathfinding: {}", mob.getName().getString());
                    return;
                }

                PathNavigation navigation = mob.getNavigation();
                Path path = navigation.createPath(target, 0);
                task.path().set(path);
            } catch (Exception e) {
                LOGGER.error("Pathfinding failed for {}", mob.getName().getString(), e);
                task.error().set(e);
            } finally {
                taskSemaphore.release();
            }
        });
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            pathfindingTasks.entrySet().removeIf(entry -> {
                PathfinderMob mob = entry.getKey();
                PathTask task = entry.getValue();

                if (!mob.isAlive() || mob.isRemoved()) {
                    LOGGER.debug("Removing pathfinding task for dead/removed entity");
                    return true;
                }

                if (task.path().get() != null || task.error().get() != null) {
                    if (task.path().get() != null) {
                        mob.getNavigation().moveTo(task.path().get(), 1.0);
                        LOGGER.debug("Path applied for {}", mob.getName().getString());
                    }
                    return true;
                }

                return false;
            });
        }
    }

    private static class PathTask {
        private final PathfinderMob mob;
        private final BlockPos target;
        private final AtomicReference<Path> path = new AtomicReference<>();
        private final AtomicReference<Exception> error = new AtomicReference<>();

        public PathTask(PathfinderMob mob, BlockPos target) {
            this.mob = mob;
            this.target = target;
        }

        public PathfinderMob mob() { return mob; }
        public BlockPos target() { return target; }
        public AtomicReference<Path> path() { return path; }
        public AtomicReference<Exception> error() { return error; }
    }
}