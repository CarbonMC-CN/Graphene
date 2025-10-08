package net.carbonmc.graphene.async.resources;

import net.carbonmc.graphene.async.AsyncSystemInitializer;
import net.carbonmc.graphene.event.AsyncHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@AsyncHandler(threadPool = "io", fallbackToSync = false)
public class AsyncResourceLoader {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final BlockingQueue<ResourceTask> resourceQueue = new LinkedBlockingQueue<>(100);
    private static final ConcurrentLinkedQueue<ResourceTask> completedTasks = new ConcurrentLinkedQueue<>();
    private static final int MAX_LOAD_ATTEMPTS = 3;
    private static final Semaphore loadSemaphore = new Semaphore(20);

    public static void init() {
        LOGGER.info("Async Resource Loader initialized");
    }

    public static void shutdown() {
        resourceQueue.clear();
        completedTasks.clear();
        LOGGER.info("Async Resource Loader shutdown completed");
    }

    public static CompletableFuture<Void> loadResourcePackAsync(PackRepository repository, Path packPath) {
        if (repository == null || packPath == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid parameters"));
        }

        if (!loadSemaphore.tryAcquire()) {
            LOGGER.warn("Too many concurrent resource loads, skipping {}", packPath);
            return CompletableFuture.failedFuture(new IllegalStateException("Too many concurrent loads"));
        }

        ResourceTask task = new ResourceTask(repository, packPath);
        if (!resourceQueue.offer(task)) {
            loadSemaphore.release();
            LOGGER.warn("Resource queue full, skipping {}", packPath);
            return CompletableFuture.failedFuture(new IllegalStateException("Resource queue full"));
        }

        return task.future().whenComplete((r, e) -> loadSemaphore.release());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            processCompletedTasks();
            processNewTasks();
        }
    }

    private static void processCompletedTasks() {
        ResourceTask completedTask;
        while ((completedTask = completedTasks.poll()) != null) {
            try {
                if (completedTask.error().get() == null) {
                    PackResources resources = completedTask.resources().get();
                    if (resources != null) {
                        registerResourcePack(completedTask.repository(), completedTask.packPath(), resources);
                    }
                } else {
                    completedTask.future().completeExceptionally(completedTask.error().get());
                }
            } catch (Exception e) {
                LOGGER.error("Error processing completed resource task", e);
                completedTask.future().completeExceptionally(e);
            }
        }
    }

    private static void processNewTasks() {
        ResourceTask task;
        while ((task = resourceQueue.poll()) != null) {
            ResourceTask finalTask = task;
            AsyncSystemInitializer.getThreadPool("io").execute(() -> {
                try {
                    PackResources resources = loadWithRetry(finalTask.packPath());
                    finalTask.resources().set(resources);
                    completedTasks.add(finalTask);
                    finalTask.future().complete(null);
                } catch (Exception e) {
                    LOGGER.error("Failed to load resource pack: {}", finalTask.packPath(), e);
                    finalTask.error().set(e);
                    completedTasks.add(finalTask);
                    finalTask.future().completeExceptionally(e);
                }
            });
        }
    }

    private static PackResources loadWithRetry(Path packPath) throws IOException {
        IOException lastException = null;
        for (int i = 0; i < MAX_LOAD_ATTEMPTS; i++) {
            try {
                return loadPackResources(packPath);
            } catch (IOException e) {
                lastException = e;
                if (i < MAX_LOAD_ATTEMPTS - 1) {
                    try {
                        Thread.sleep(100 * (i + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted during retry", ie);
                    }
                }
            }
        }
        throw lastException;
    }

    private static void registerResourcePack(PackRepository repository, Path packPath, PackResources resources) {
        try {
            String packName = packPath.getFileName().toString();
            Pack pack = Pack.readMetaAndCreate(
                    packName,
                    Component.literal(packName),
                    true,
                    name -> resources,
                    PackType.SERVER_DATA,
                    Pack.Position.TOP,
                    PackSource.BUILT_IN
            );

            if (pack != null) {
                List<String> selectedIds = new ArrayList<>();
                for (Pack existing : repository.getSelectedPacks()) {
                    selectedIds.add(existing.getId());
                }
                selectedIds.add(pack.getId());

                repository.setSelected(selectedIds);
                repository.reload();
                LOGGER.info("Successfully loaded resource pack: {}", packName);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to register resource pack: " + packPath, e);
        }
    }

    private static PackResources loadPackResources(Path packPath) throws IOException {
        if (!Files.exists(packPath)) {
            throw new IOException("Resource pack not found: " + packPath);
        }
        return new net.minecraft.server.packs.PathPackResources(
                packPath.getFileName().toString(),
                packPath,
                false
        );
    }

    private static class ResourceTask {
        private final PackRepository repository;
        private final Path packPath;
        private final AtomicReference<PackResources> resources = new AtomicReference<>();
        private final AtomicReference<Exception> error = new AtomicReference<>();
        private final CompletableFuture<Void> future = new CompletableFuture<>();

        public ResourceTask(PackRepository repository, Path packPath) {
            this.repository = repository;
            this.packPath = packPath;
        }

        public PackRepository repository() { return repository; }
        public Path packPath() { return packPath; }
        public AtomicReference<PackResources> resources() { return resources; }
        public AtomicReference<Exception> error() { return error; }
        public CompletableFuture<Void> future() { return future; }
    }
}