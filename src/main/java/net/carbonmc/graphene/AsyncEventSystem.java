package net.carbonmc.graphene;

import net.carbonmc.graphene.config.CoolConfig;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 高性能异步事件处理系统，用于优化Minecraft Forge事件处理
 */
public final class AsyncEventSystem {
    public static final Logger LOGGER = LogManager.getLogger("Graph-Async");

    // 线程池配置常量
    private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();
    private static final int MIN_POOL_SIZE = 2;
    private static final int DEFAULT_CORE_POOL_SIZE = Math.min(4, Math.max(CoolConfig.maxCPUPro.get(), CPU_CORES));
    private static final int MAX_POOL_SIZE = Math.min(16, CPU_CORES * 2);
    private static final long KEEP_ALIVE_TIME = 30L;
    private static final int WORK_QUEUE_SIZE = 2000;
    private static final long POOL_ADJUST_INTERVAL_MS = 30000;
    private static final long SLOW_TASK_THRESHOLD_NS = TimeUnit.MILLISECONDS.toNanos(100);
    private static final int MAX_FAILURES_BEFORE_DISABLE = 3;
    private static final long FAILURE_COOLDOWN_MS = 60000;

    // 线程池实例
    private static final ThreadPoolExecutor ASYNC_EXECUTOR = createThreadPool();

    // 事件类型注册表
    private static final ConcurrentHashMap<Class<? extends Event>, EventTypeInfo> EVENT_TYPE_INFOS = new ConcurrentHashMap<>(64);

    // 系统状态跟踪
    private static volatile boolean initialized = false;
    private static final AtomicLong totalAsyncTasks = new AtomicLong(0);
    private static final AtomicLong failedAsyncTasks = new AtomicLong(0);
    private static final AtomicLong lastAdjustTime = new AtomicLong(System.currentTimeMillis());

    static {
        ASYNC_EXECUTOR.prestartAllCoreThreads();
    }

    /**
     * 创建并配置线程池
     */
    private static ThreadPoolExecutor createThreadPool() {
        return new ThreadPoolExecutor(
                DEFAULT_CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(WORK_QUEUE_SIZE),
                new AsyncEventThreadFactory(),
                new AsyncEventRejectedHandler()
        ) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                adjustPoolSize();
            }
        };
    }

    /**
     * 动态调整线程池大小
     */
    private static void adjustPoolSize() {
        long now = System.currentTimeMillis();
        if (now - lastAdjustTime.get() < POOL_ADJUST_INTERVAL_MS) {
            return;
        }

        int activeCount = ASYNC_EXECUTOR.getActiveCount();
        int queueSize = ASYNC_EXECUTOR.getQueue().size();
        int newCoreSize = calculateNewPoolSize(activeCount, queueSize);

        ASYNC_EXECUTOR.setCorePoolSize(newCoreSize);
        lastAdjustTime.set(now);
    }

    /**
     * 计算新的线程池大小
     */
    private static int calculateNewPoolSize(int activeCount, int queueSize) {
        return Math.min(MAX_POOL_SIZE, Math.max(MIN_POOL_SIZE, activeCount + (queueSize / 100)));
    }

    /**
     * 事件类型信息内部类
     */
    private static class EventTypeInfo {
        volatile boolean async;
        volatile boolean healthy = true;
        final AtomicInteger pendingTasks = new AtomicInteger(0);
        final AtomicInteger failedCount = new AtomicInteger(0);
        volatile boolean isClientEvent = false;
        volatile long lastFailureTime = 0;

        EventTypeInfo(boolean async) {
            this.async = async;
        }

        boolean shouldRetryAsync() {
            return async && healthy &&
                    (failedCount.get() < MAX_FAILURES_BEFORE_DISABLE ||
                            System.currentTimeMillis() - lastFailureTime > FAILURE_COOLDOWN_MS);
        }
    }

    /**
     * 自定义线程工厂
     */
    private static class AsyncEventThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "Graphene-Async-Worker-" + counter.incrementAndGet());
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            thread.setUncaughtExceptionHandler((t, e) -> {
                LOGGER.error("Uncaught exception in async thread", e);
            });
            return thread;
        }
    }

    /**
     * 拒绝策略处理器
     */
    private static class AsyncEventRejectedHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            LOGGER.warn("Queue full ({} tasks), executing on caller thread", executor.getQueue().size());
            if (!executor.isShutdown()) {
                r.run();
            }
        }
    }

    /**
     * 初始化异步事件系统
     */
    public static void initialize() {
        if (!CoolConfig.isEnabled() || initialized) {
            return;
        }

        registerCommonAsyncEvents();
        initialized = true;
        LOGGER.info("Initialized with core: {}, max: {}, queue: {}",
                DEFAULT_CORE_POOL_SIZE, MAX_POOL_SIZE, WORK_QUEUE_SIZE);
    }

    /**
     * 注册常见异步事件
     */
    private static void registerCommonAsyncEvents() {
        if (!CoolConfig.isEnabled()) {
            return;
        }

        // 异步事件类名列表
        String[] asyncEvents = {
                "net.minecraftforge.event.entity.player.PlayerEvent",
                "net.minecraftforge.event.entity.player.AdvancementEvent",
                "net.minecraftforge.event.entity.player.AnvilRepairEvent",
                "net.minecraftforge.event.entity.player.PlayerInteractEvent",
                "net.minecraftforge.event.entity.player.PlayerXpEvent",
                "net.minecraftforge.event.level.BlockEvent",
                "net.minecraftforge.event.level.ChunkEvent",
                "net.minecraftforge.event.level.ExplosionEvent",
                "net.minecraftforge.event.entity.EntityEvent",
                "net.minecraftforge.event.entity.EntityJoinLevelEvent",
                "net.minecraftforge.event.entity.EntityLeaveLevelEvent",
                "net.minecraftforge.event.entity.EntityMountEvent",
                "net.minecraftforge.event.entity.EntityTeleportEvent",
                "net.minecraftforge.event.entity.item.ItemEvent",
                "net.minecraftforge.event.entity.item.ItemExpireEvent",
                "net.minecraftforge.event.entity.item.ItemTossEvent",
                "net.minecraftforge.event.level.LevelEvent",
                "net.minecraftforge.event.level.BlockEvent",
                "net.minecraftforge.event.level.ChunkEvent",
                "net.minecraftforge.event.network.CustomPayloadEvent",
                "net.minecraftforge.event.CommandEvent",
                "net.minecraftforge.event.TagsUpdatedEvent",
                "net.minecraftforge.event.LootTableLoadEvent",
                "net.minecraftforge.event.RegisterCommandsEvent"
        };

        // 同步事件类名列表
        String[] syncEvents = {
                "net.minecraftforge.event.TickEvent",
                "net.minecraftforge.event.level.LevelTickEvent",
                "net.minecraftforge.event.entity.living.LivingEvent",
                "net.minecraftforge.event.entity.living.LivingAttackEvent",
                "net.minecraftforge.event.entity.living.LivingDamageEvent",
                "net.minecraftforge.event.entity.living.LivingDeathEvent",
                "net.minecraftforge.event.entity.living.LivingDropsEvent",
                "net.minecraftforge.event.entity.living.LivingExperienceDropEvent",
                "net.minecraftforge.event.entity.living.LivingHealEvent",
                "net.minecraftforge.event.entity.living.LivingKnockBackEvent",
                "net.minecraftforge.event.server.ServerStartingEvent",
                "net.minecraftforge.event.server.ServerStoppingEvent",
                "net.minecraftforge.event.server.ServerStartedEvent"
        };

        registerEvents(asyncEvents, true);
        registerEvents(syncEvents, false);

        LOGGER.info("Registered {} async event types", EVENT_TYPE_INFOS.size());
    }

    /**
     * 批量注册事件
     */
    private static void registerEvents(String[] classNames, boolean async) {
        for (String className : classNames) {
            try {
                Class<? extends Event> eventClass = loadEventClass(className);
                if (async && isClientOnlyEvent(eventClass)) {
                    LOGGER.debug("Skipping client event: {}", className);
                    continue;
                }
                if (async) {
                    registerAsyncEvent(eventClass);
                } else {
                    registerSyncEvent(eventClass);
                }
            } catch (ClassNotFoundException e) {
                handleEventRegistrationError(className, async, e);
            }
        }
    }

    /**
     * 加载事件类
     */
    @SuppressWarnings("unchecked")
    private static Class<? extends Event> loadEventClass(String className) throws ClassNotFoundException {
        try {
            Class<?> clazz = Class.forName(className);
            if (!Event.class.isAssignableFrom(clazz)) {
                throw new IllegalArgumentException("Class does not extend Event: " + className);
            }
            return (Class<? extends Event>) clazz;
        } catch (ClassNotFoundException e) {
            ClassLoader forgeLoader = Event.class.getClassLoader();
            Class<?> clazz = Class.forName(className, true, forgeLoader);
            return (Class<? extends Event>) clazz;
        }
    }

    /**
     * 处理事件注册错误
     */
    private static void handleEventRegistrationError(String className, boolean async, ClassNotFoundException e) {
        if (async) {
            LOGGER.warn("[Fallback] Failed to load async event: {}, falling back to SYNC", className);
            try {
                Class<? extends Event> eventClass = loadEventClass(className);
                registerSyncEvent(eventClass);
            } catch (ClassNotFoundException ex) {
                LOGGER.error("[Critical] Event class not found: {}", className);
            }
        } else {
            LOGGER.error("[Critical] Sync event class not found: {}", className);
        }
    }

    /**
     * 判断是否是客户端专用事件
     */
    public static boolean isClientOnlyEvent(Class<? extends Event> eventClass) {
        String name = eventClass.getName();
        return name.startsWith("client") || name.contains(".client.") || name.startsWith("net.minecraft.client.");
    }

    /**
     * 注册异步事件类型
     */
    public static void registerAsyncEvent(Class<? extends Event> eventType) {
        if (!CoolConfig.isEnabled()) return;

        EVENT_TYPE_INFOS.compute(eventType, (k, v) -> {
            EventTypeInfo info = (v == null) ? new EventTypeInfo(true) : v;
            info.async = true;
            info.healthy = true;
            info.failedCount.set(0);
            info.isClientEvent = isClientOnlyEvent(eventType);
            return info;
        });

        LOGGER.debug("Registered async event: {}", eventType.getName());
    }

    /**
     * 注册同步事件类型
     */
    public static void registerSyncEvent(Class<? extends Event> eventType) {
        if (!CoolConfig.isEnabled()) return;

        EVENT_TYPE_INFOS.compute(eventType, (k, v) -> {
            EventTypeInfo info = (v == null) ? new EventTypeInfo(false) : v;
            info.async = false;
            info.isClientEvent = isClientOnlyEvent(eventType);
            return info;
        });

        LOGGER.debug("Registered sync event: {}", eventType.getName());
    }

    /**
     * 判断事件是否应该异步处理
     */
    public static boolean shouldHandleAsync(Class<? extends Event> eventType) {
        EventTypeInfo info = EVENT_TYPE_INFOS.get(eventType);
        if (info == null) {
            return eventType.getSimpleName().contains("Async");
        }
        return !(info.isClientEvent && FMLEnvironment.dist.isDedicatedServer()) && info.async && info.healthy;
    }

    /**
     * 异步执行任务
     */
    public static CompletableFuture<Void> executeAsync(Class<? extends Event> eventType, Runnable task) {
        if (!CoolConfig.isEnabled() || shouldExecuteImmediately(eventType)) {
            task.run();
            return CompletableFuture.completedFuture(null);
        }

        totalAsyncTasks.incrementAndGet();
        EventTypeInfo info = getEventTypeInfo(eventType);

        if (!info.async || !info.healthy) {
            task.run();
            return CompletableFuture.completedFuture(null);
        }

        return submitAsyncTask(eventType, task, info);
    }

    /**
     * 提交异步任务
     */
    private static CompletableFuture<Void> submitAsyncTask(Class<? extends Event> eventType, Runnable task, EventTypeInfo info) {
        info.pendingTasks.incrementAndGet();
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();

        return CompletableFuture.runAsync(() -> {
            Thread.currentThread().setContextClassLoader(contextLoader);
            try {
                executeAndMonitorTask(eventType, task);
            } catch (Throwable t) {
                handleTaskFailure(eventType, info, t);
                throw t;
            } finally {
                info.pendingTasks.decrementAndGet();
            }
        }, ASYNC_EXECUTOR).exceptionally(ex -> {
            LOGGER.warn("Retrying {} synchronously", eventType.getSimpleName());
            task.run();
            return null;
        });
    }

    /**
     * 判断是否应该立即执行(同步)
     */
    private static boolean shouldExecuteImmediately(Class<? extends Event> eventType) {
        return (eventType.getName().contains("Client") && FMLEnvironment.dist.isDedicatedServer()) || !initialized;
    }

    /**
     * 获取事件类型信息
     */
    private static EventTypeInfo getEventTypeInfo(Class<? extends Event> eventType) {
        return EVENT_TYPE_INFOS.computeIfAbsent(
                eventType,
                k -> new EventTypeInfo(shouldHandleAsync(eventType))
        );
    }

    /**
     * 执行并监控任务性能
     */
    private static void executeAndMonitorTask(Class<? extends Event> eventType, Runnable task) {
        long startTime = System.nanoTime();
        task.run();
        long elapsed = System.nanoTime() - startTime;

        if (elapsed > SLOW_TASK_THRESHOLD_NS) {
            LOGGER.debug("Slow task {}: {}ms",
                    eventType.getSimpleName(),
                    TimeUnit.NANOSECONDS.toMillis(elapsed));
        }
    }

    /**
     * 处理任务失败
     */
    private static void handleTaskFailure(Class<? extends Event> eventType, EventTypeInfo info, Throwable t) {
        failedAsyncTasks.incrementAndGet();
        info.failedCount.incrementAndGet();
        info.lastFailureTime = System.currentTimeMillis();

        LOGGER.error("Task failed: {}", eventType.getSimpleName(), t);

        if (CoolConfig.DISABLE_ASYNC_ON_ERROR.get() || info.failedCount.get() >= MAX_FAILURES_BEFORE_DISABLE) {
            info.healthy = false;
            LOGGER.warn("Disabled async for {}", eventType.getSimpleName());
        }
    }

    /**
     * 关闭异步事件系统
     */
    public static void shutdown() {
        if (!CoolConfig.isEnabled()) return;
        if (!initialized) return;

        LOGGER.info("Shutting down async event system. Total tasks: {}, Failed: {}",
                totalAsyncTasks.get(), failedAsyncTasks.get());

        ASYNC_EXECUTOR.shutdown();
        try {
            if (!ASYNC_EXECUTOR.awaitTermination(10, TimeUnit.SECONDS)) {
                LOGGER.warn("Forcing async event executor shutdown");
                ASYNC_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            ASYNC_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 获取当前队列大小
     */
    public static int getQueueSize() {
        return ASYNC_EXECUTOR.getQueue().size();
    }

    /**
     * 获取活动线程数
     */
    public static int getActiveCount() {
        return ASYNC_EXECUTOR.getActiveCount();
    }

    /**
     * 获取当前线程池大小
     */
    public static int getPoolSize() {
        return ASYNC_EXECUTOR.getPoolSize();
    }

    /**
     * 获取最大线程池大小
     */
    public static int getMaxPoolSize() {
        return MAX_POOL_SIZE;
    }

    /**
     * 获取已注册的异步事件数量
     */
    public static int getAsyncEventCount() {
        return EVENT_TYPE_INFOS.size();
    }

    /**
     * 尝试通过消费者注册异步事件
     */
    public static void tryRegisterAsyncEvent(Consumer<?> consumer) {
        try {
            for (Type type : consumer.getClass().getGenericInterfaces()) {
                if (type instanceof ParameterizedType) {
                    ParameterizedType paramType = (ParameterizedType) type;
                    if (paramType.getRawType().equals(Consumer.class)) {
                        Type[] typeArgs = paramType.getActualTypeArguments();
                        if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                            Class<?> eventClass = (Class<?>) typeArgs[0];
                            if (Event.class.isAssignableFrom(eventClass)) {
                                @SuppressWarnings("unchecked")
                                Class<? extends Event> eventType = (Class<? extends Event>) eventClass;
                                registerAsyncEvent(eventType);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to determine event type for consumer: {}", consumer.getClass().getName(), e);
        }
    }

    /**
     * 重置事件类型的健康状态
     */
    public static void resetEventTypeHealth(Class<? extends Event> eventType) {
        EVENT_TYPE_INFOS.computeIfPresent(eventType, (k, v) -> {
            v.healthy = true;
            v.failedCount.set(0);
            return v;
        });
    }
}