package net.carbonmc.graphene.async.entity;

import net.carbonmc.graphene.async.AsyncSystemInitializer;
import net.carbonmc.graphene.event.AsyncHandler;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@AsyncHandler(threadPool = "compute", fallbackToSync = true)
public class AsyncCollisionSystem {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final BlockingQueue<CollisionTask> collisionQueue = new LinkedBlockingQueue<>(1000);
    private static final Map<UUID, CollisionTask> activeTasks = new ConcurrentHashMap<>();
    private static final int MAX_BATCH_SIZE = 50;
    private static final Semaphore processingSemaphore = new Semaphore(MAX_BATCH_SIZE);

    public static void init() {
        LOGGER.info("Async Collision System initialized");
    }

    public static void shutdown() {
        collisionQueue.clear();
        activeTasks.clear();
        LOGGER.info("Async Collision System shutdown");
    }

    public static void checkCollisionsAsync(Collection<Entity> entities) {
        if (entities.isEmpty()) return;

        if (!processingSemaphore.tryAcquire()) {
            LOGGER.warn("Too many collision tasks queued, skipping this batch");
            return;
        }

        CollisionTask task = new CollisionTask(new ArrayList<>(entities));
        if (collisionQueue.offer(task)) {
            activeTasks.put(task.taskId(), task);
        } else {
            LOGGER.warn("Collision queue full, skipping task");
            processingSemaphore.release();
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            processCollisionTasks();
            applyCollisionResults();
        }
    }

    private static void processCollisionTasks() {
        int processed = 0;
        while (processed < 100 && !collisionQueue.isEmpty()) {
            CollisionTask task = collisionQueue.poll();
            if (task != null && activeTasks.containsKey(task.taskId())) {
                AsyncSystemInitializer.getThreadPool("compute").execute(() -> {
                    try {
                        List<CollisionResult> results = new ArrayList<>();
                        Entity[] entityArray = task.entities().toArray(new Entity[0]);

                        for (int j = 0; j < entityArray.length; j++) {
                            Entity e1 = entityArray[j];
                            if (e1.isRemoved() || !e1.isAlive()) continue;

                            for (int k = j + 1; k < entityArray.length; k++) {
                                Entity e2 = entityArray[k];
                                if (e2.isRemoved() || !e2.isAlive()) continue;

                                if (e1.getBoundingBox().intersects(e2.getBoundingBox())) {
                                    results.add(new CollisionResult(e1, e2));
                                }
                            }
                        }

                        task.results().addAll(results);
                        task.completed().set(true);
                    } catch (Exception e) {
                        LOGGER.error("Collision detection failed", e);
                        task.error().set(e);
                    } finally {
                        processingSemaphore.release();
                    }
                });
                processed++;
            }
        }
    }

    private static void applyCollisionResults() {
        activeTasks.entrySet().removeIf(entry -> {
            CollisionTask task = entry.getValue();
            if (task.completed().get()) {
                if (task.error().get() == null) {
                    for (CollisionResult result : task.results()) {
                        if (!result.entity1().isRemoved() && result.entity1().isAlive() &&
                                !result.entity2().isRemoved() && result.entity2().isAlive()) {

                            result.entity1().push(result.entity2());
                            result.entity2().push(result.entity1());
                        }
                    }
                    LOGGER.debug("Applied {} collision results", task.results().size());
                }
                return true;
            }
            return false;
        });
    }

    private static class CollisionTask {
        private final UUID taskId = UUID.randomUUID();
        private final List<Entity> entities;
        private final List<CollisionResult> results = new CopyOnWriteArrayList<>();
        private final AtomicBoolean completed = new AtomicBoolean(false);
        private final AtomicReference<Exception> error = new AtomicReference<>();

        public CollisionTask(List<Entity> entities) {
            this.entities = Collections.unmodifiableList(entities);
        }

        public UUID taskId() { return taskId; }
        public List<Entity> entities() { return entities; }
        public List<CollisionResult> results() { return results; }
        public AtomicBoolean completed() { return completed; }
        public AtomicReference<Exception> error() { return error; }
    }

    private record CollisionResult(Entity entity1, Entity entity2) {}
}