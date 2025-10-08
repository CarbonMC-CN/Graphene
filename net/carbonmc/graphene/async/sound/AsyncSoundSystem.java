package net.carbonmc.graphene.async.sound;

import net.carbonmc.graphene.async.AsyncSystemInitializer;
import net.carbonmc.graphene.event.AsyncHandler;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

@AsyncHandler(threadPool = "default", fallbackToSync = true)
public class AsyncSoundSystem {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final BlockingQueue<SoundTask> soundQueue = new LinkedBlockingQueue<>(1000);
    private static final Map<UUID, SoundTask> pendingSounds = new ConcurrentHashMap<>();
    private static final int MAX_SOUNDS_PER_TICK = 50;
    private static final Semaphore soundSemaphore = new Semaphore(500);

    public static void init() {
        LOGGER.info("Async Sound System initialized");
    }

    public static void shutdown() {
        soundQueue.clear();
        pendingSounds.clear();
        LOGGER.info("Async Sound System shutdown completed");
    }

    public static void playSoundIfInRangeAsync(ServerPlayer player, SoundEvent sound,
                                               SoundSource category, Vec3 pos,
                                               float volume, float pitch) {
        if (player == null || sound == null || pos == null) {
            LOGGER.warn("Invalid parameters for sound playback");
            return;
        }

        if (player.isRemoved() || !player.isAlive()) {
            LOGGER.debug("Attempted to play sound for removed/inactive player");
            return;
        }

        if (!soundSemaphore.tryAcquire()) {
            LOGGER.warn("Too many concurrent sound requests, skipping sound for {}", player.getName().getString());
            return;
        }

        SoundTask task = new SoundTask(player, sound, category, pos, volume, pitch);
        if (soundQueue.offer(task)) {
            pendingSounds.put(player.getUUID(), task);
        } else {
            soundSemaphore.release();
            LOGGER.warn("Sound queue full, skipping sound for {}", player.getName().getString());
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            processSoundTasks();
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            processPlayerSounds(player);
        }
    }

    private static void processSoundTasks() {
        int processed = 0;
        while (processed < MAX_SOUNDS_PER_TICK && !soundQueue.isEmpty()) {
            SoundTask task = soundQueue.poll();
            if (task != null) {
                processed++;
                AsyncSystemInitializer.getThreadPool("default").execute(() -> {
                    try {
                        checkSoundRange(task);
                    } finally {
                        soundSemaphore.release();
                    }
                });
            }
        }
    }

    private static void checkSoundRange(SoundTask task) {
        try {
            ServerPlayer player = task.player();
            if (player.isRemoved() || !player.isAlive()) {
                LOGGER.debug("Player not available for sound playback");
                return;
            }

            double distanceSqr = player.distanceToSqr(task.pos().x, task.pos().y, task.pos().z);
            if (distanceSqr <= (task.volume() * task.volume()) * 256.0) {
                task.withinRange().set(true);
            }
        } catch (Exception e) {
            LOGGER.error("Sound range check failed", e);
        }
    }

    private static void processPlayerSounds(ServerPlayer player) {
        SoundTask task = pendingSounds.get(player.getUUID());
        if (task != null && task.withinRange().get()) {
            try {
                player.connection.send(new ClientboundSoundPacket(
                        Holder.direct(task.sound()),
                        task.category(),
                        task.pos().x, task.pos().y, task.pos().z,
                        task.volume(), task.pitch(),
                        player.level().getRandom().nextLong()
                ));
                LOGGER.debug("Sound played for {}: {}",
                        player.getName().getString(),
                        BuiltInRegistries.SOUND_EVENT.getKey(task.sound()));
            } catch (Exception e) {
                LOGGER.error("Failed to send sound packet to {}", player.getName().getString(), e);
            } finally {
                pendingSounds.remove(player.getUUID());
            }
        }
    }

    private static class SoundTask {
        private final ServerPlayer player;
        private final SoundEvent sound;
        private final SoundSource category;
        private final Vec3 pos;
        private final float volume;
        private final float pitch;
        private final AtomicBoolean withinRange = new AtomicBoolean(false);

        public SoundTask(ServerPlayer player, SoundEvent sound, SoundSource category,
                         Vec3 pos, float volume, float pitch) {
            this.player = player;
            this.sound = sound;
            this.category = category;
            this.pos = pos;
            this.volume = volume;
            this.pitch = pitch;
        }

        public ServerPlayer player() { return player; }
        public SoundEvent sound() { return sound; }
        public SoundSource category() { return category; }
        public Vec3 pos() { return pos; }
        public float volume() { return volume; }
        public float pitch() { return pitch; }
        public AtomicBoolean withinRange() { return withinRange; }
    }
}