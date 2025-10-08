package net.carbonmc.graphene.async;

import net.carbonmc.graphene.async.entity.AsyncAIManager;
import net.carbonmc.graphene.async.entity.AsyncCollisionSystem;
import net.carbonmc.graphene.async.player.AsyncPlayerData;
import net.carbonmc.graphene.async.redstone.AsyncRedstone;
import net.carbonmc.graphene.async.resources.AsyncResourceLoader;
import net.carbonmc.graphene.async.sound.AsyncSoundSystem;
import net.carbonmc.graphene.async.world.StructureGenAsync;
import net.carbonmc.graphene.config.CoolConfig;
import net.carbonmc.graphene.event.AsyncHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@AsyncHandler
public class AsyncSystemInitializer {
    private static final Logger LOGGER = LogManager.getLogger("AsyncSystemInitializer");
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);

    private static ExecutorService defaultThreadPool;
    private static ExecutorService ioThreadPool;
    private static ExecutorService computationThreadPool;

    public static void init(FMLCommonSetupEvent event) {
        if (!initialized.compareAndSet(false, true)) {
            LOGGER.warn("Async system already initialized");
            return;
        }

        try {
            defaultThreadPool = createThreadPool("Async-Default", 4);
            ioThreadPool = createThreadPool("Async-IO", 2);
            computationThreadPool = createThreadPool("Async-Compute", Runtime.getRuntime().availableProcessors() / 2);
            safeInit(StructureGenAsync::init, "StructureGenAsync");
            safeInit(AsyncAIManager::init, "AsyncAIManager");
            safeInit(AsyncCollisionSystem::init, "AsyncCollisionSystem");
            safeInit(AsyncPlayerData::init, "AsyncPlayerData");
            safeInit(AsyncResourceLoader::init, "AsyncResourceLoader");
            safeInit(AsyncRedstone::init, "AsyncRedstone");
            safeInit(AsyncSoundSystem::init, "AsyncSoundSystem");
            registerEventHandlers();
            Runtime.getRuntime().addShutdownHook(new Thread(AsyncSystemInitializer::shutdown, "AsyncSystem-ShutdownHook"));
            if (CoolConfig.DEBUG_LOGGING.get()) {
                LOGGER.info("Async system initialized successfully");
            }
        } catch (Exception e) {
            if (CoolConfig.DEBUG_LOGGING.get()) {
                LOGGER.error("Failed to initialize async system", e);
            }
            shutdown();
            throw new RuntimeException("Async system initialization failed", e);
        }
    }

    private static ExecutorService createThreadPool(String name, int coreSize) {
        return new ThreadPoolExecutor(
                coreSize,
                coreSize * 2,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                r -> new Thread(r, name + "-" + System.identityHashCode(r)),
                new ThreadPoolExecutor.CallerRunsPolicy() // Fallback to caller thread if queue is full
        );
    }

    private static void safeInit(Runnable initTask, String systemName) {
        try {
            initTask.run();
            if (CoolConfig.DEBUG_LOGGING.get()) {
                LOGGER.debug("{} initialized successfully", systemName);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to initialize {}", systemName, e);
            if (AsyncHandler.class.getAnnotation(AsyncHandler.class).fallbackToSync()) {
                if (CoolConfig.DEBUG_LOGGING.get()) {
                    LOGGER.warn("Falling back to synchronous mode for {}", systemName);
                }
            } else {
                throw new RuntimeException("Critical failure in " + systemName, e);
            }
        }
    }

    private static void registerEventHandlers() {
        MinecraftForge.EVENT_BUS.register(StructureGenAsync.class);
        MinecraftForge.EVENT_BUS.register(AsyncAIManager.class);
        MinecraftForge.EVENT_BUS.register(AsyncCollisionSystem.class);
        MinecraftForge.EVENT_BUS.register(AsyncPlayerData.class);
        MinecraftForge.EVENT_BUS.register(AsyncResourceLoader.class);
        MinecraftForge.EVENT_BUS.register(AsyncRedstone.class);
        MinecraftForge.EVENT_BUS.register(AsyncSoundSystem.class);
    }

    public static ExecutorService getThreadPool(String poolType) {
        if (shutdownInProgress.get()) {
            throw new IllegalStateException("Async system is shutting down");
        }

        return switch (poolType) {
            case "io" -> ioThreadPool;
            case "compute" -> computationThreadPool;
            default -> defaultThreadPool;
        };
    }

    public static void shutdown() {
        if (!initialized.get() || !shutdownInProgress.compareAndSet(false, true)) {
            return;
        }

        LOGGER.info("Shutting down async system...");
        safeShutdown(AsyncSoundSystem::shutdown, "AsyncSoundSystem");
        safeShutdown(AsyncRedstone::shutdown, "AsyncRedstone");
        safeShutdown(AsyncResourceLoader::shutdown, "AsyncResourceLoader");
        safeShutdown(AsyncPlayerData::shutdown, "AsyncPlayerData");
        safeShutdown(AsyncCollisionSystem::shutdown, "AsyncCollisionSystem");
        safeShutdown(AsyncAIManager::shutdown, "AsyncAIManager");
        safeShutdown(StructureGenAsync::shutdown, "StructureGenAsync");
        shutdownThreadPool(defaultThreadPool, "DefaultThreadPool");
        shutdownThreadPool(ioThreadPool, "IOThreadPool");
        shutdownThreadPool(computationThreadPool, "ComputationThreadPool");

        LOGGER.info("Async system shutdown complete");
    }

    private static void safeShutdown(Runnable shutdownTask, String systemName) {
        try {
            shutdownTask.run();
            LOGGER.debug("{} shutdown successfully", systemName);
        } catch (Exception e) {
            LOGGER.error("Failed to shutdown {}", systemName, e);
        }
    }

    private static void shutdownThreadPool(ExecutorService pool, String poolName) {
        if (pool == null) return;

        try {
            pool.shutdown();
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                if (CoolConfig.DEBUG_LOGGING.get()) {
                    LOGGER.warn("Had to force shutdown {}", poolName);
                }
            } else {
                if (CoolConfig.DEBUG_LOGGING.get()) {
                LOGGER.debug("{} shutdown gracefully", poolName);}
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
            LOGGER.warn("Interrupted while shutting down {}", poolName);
        }
    }
}