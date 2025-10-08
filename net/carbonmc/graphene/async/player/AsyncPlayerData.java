package net.carbonmc.graphene.async.player;

import net.carbonmc.graphene.async.AsyncSystemInitializer;
import net.carbonmc.graphene.event.AsyncHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@AsyncHandler(threadPool = "io", fallbackToSync = false)
public class AsyncPlayerData {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final BlockingQueue<SaveTask> saveQueue = new LinkedBlockingQueue<>(1000);
    private static final ConcurrentMap<UUID, LoadTask> loadTasks = new ConcurrentHashMap<>();
    private static final int MAX_RETRIES = 3;
    private static final Path TEMP_DIR = Path.of("playerdata_tmp");

    public static class PlayerData implements Serializable {
        private static final long serialVersionUID = 1L;
        private final UUID playerId;
        private final String playerName;
        private final long lastModified;

        public PlayerData(ServerPlayer player) {
            this.playerId = player.getUUID();
            this.playerName = player.getName().getString();
            this.lastModified = System.currentTimeMillis();
        }

        public UUID getPlayerId() { return playerId; }
        public String getPlayerName() { return playerName; }
        public long getLastModified() { return lastModified; }
    }

    public static void init() {
        try {
            Files.createDirectories(TEMP_DIR);
            LOGGER.info("Async Player Data Manager initialized");
        } catch (IOException e) {
            LOGGER.error("Failed to initialize player data manager", e);
            throw new RuntimeException("Player data initialization failed", e);
        }
    }

    public static void shutdown() {
        saveQueue.clear();
        loadTasks.clear();
        LOGGER.info("Async Player Data Manager shutdown");
    }

    public static void savePlayerDataAsync(ServerPlayer player, Path dataDir) {
        if (player == null || dataDir == null) {
            LOGGER.warn("Invalid parameters for savePlayerDataAsync");
            return;
        }

        if (!saveQueue.offer(new SaveTask(player, dataDir))) {
            LOGGER.warn("Player data save queue full, data not saved for {}", player.getName().getString());
        }
    }

    public static CompletableFuture<PlayerData> loadPlayerDataAsync(UUID playerId, Path dataDir) {
        if (playerId == null || dataDir == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid parameters"));
        }

        LoadTask existingTask = loadTasks.get(playerId);
        if (existingTask != null) {
            return existingTask.future();
        }

        LoadTask newTask = new LoadTask(playerId, dataDir);
        loadTasks.put(playerId, newTask);

        AsyncSystemInitializer.getThreadPool("io").execute(() -> {
            try {
                Path playerFile = dataDir.resolve(playerId.toString() + ".dat");
                PlayerData data = loadWithRetry(playerFile);
                newTask.result().set(data);
            } catch (Exception e) {
                LOGGER.error("Failed to load player data for {}", playerId, e);
                newTask.error().set(e);
            } finally {
                newTask.completed().set(true);
            }
        });

        return newTask.future();
    }

    private static PlayerData loadWithRetry(Path file) throws IOException, ClassNotFoundException {
        if (!Files.exists(file)) {
            return null;
        }

        IOException lastException = null;
        for (int i = 0; i < MAX_RETRIES; i++) {
            try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(file))) {
                return (PlayerData) ois.readObject();
            } catch (IOException e) {
                lastException = e;
                if (i < MAX_RETRIES - 1) {
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

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            processSaveTasks();
            processLoadTasks();
        }
    }

    private static void processSaveTasks() {
        int processed = 0;
        while (processed < 50 && !saveQueue.isEmpty()) {
            SaveTask task = saveQueue.poll();
            if (task != null) {
                processed++;
                AsyncSystemInitializer.getThreadPool("io").execute(() -> saveWithRetry(task));
            }
        }
    }

    private static void saveWithRetry(SaveTask task) {
        ServerPlayer player = task.player();
        if (player == null || player.isRemoved()) {
            LOGGER.warn("Player not available for data save");
            return;
        }

        UUID playerId = player.getUUID();
        Path tempFile = TEMP_DIR.resolve(playerId + ".tmp");
        Path finalFile = task.dataDir().resolve(playerId + ".dat");

        try {
            Files.createDirectories(task.dataDir());

            // Write to temp file first
            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(tempFile))) {
                oos.writeObject(new PlayerData(player));
            }

            // Atomic move to final location
            Files.move(tempFile, finalFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            LOGGER.debug("Player data saved for {}", player.getName().getString());
        } catch (Exception e) {
            LOGGER.error("Failed to save player data for {}", player.getName().getString(), e);
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ioException) {
                LOGGER.warn("Failed to delete temp file", ioException);
            }
        }
    }

    private static void processLoadTasks() {
        loadTasks.entrySet().removeIf(entry -> {
            LoadTask task = entry.getValue();
            if (task.completed().get()) {
                if (task.error().get() != null) {
                    task.future().completeExceptionally(task.error().get());
                } else {
                    task.future().complete(task.result().get());
                }
                return true;
            }
            return false;
        });
    }

    private static class SaveTask {
        private final ServerPlayer player;
        private final Path dataDir;

        public SaveTask(ServerPlayer player, Path dataDir) {
            this.player = player;
            this.dataDir = dataDir;
        }

        public ServerPlayer player() { return player; }
        public Path dataDir() { return dataDir; }
    }

    private static class LoadTask {
        private final UUID playerId;
        private final Path dataDir;
        private final CompletableFuture<PlayerData> future = new CompletableFuture<>();
        private final AtomicReference<PlayerData> result = new AtomicReference<>();
        private final AtomicBoolean completed = new AtomicBoolean(false);
        private final AtomicReference<Exception> error = new AtomicReference<>();

        public LoadTask(UUID playerId, Path dataDir) {
            this.playerId = playerId;
            this.dataDir = dataDir;
        }

        public CompletableFuture<PlayerData> future() { return future; }
        public AtomicReference<PlayerData> result() { return result; }
        public AtomicBoolean completed() { return completed; }
        public AtomicReference<Exception> error() { return error; }
    }
}