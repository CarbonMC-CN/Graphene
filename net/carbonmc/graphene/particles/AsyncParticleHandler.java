package net.carbonmc.graphene.particles;

import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.*;

public class AsyncParticleHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int MAX_PARTICLES_PER_TICK = 1000;
    private static volatile ExecutorService executorService;
    private static final BlockingQueue<ParticleTask> particleQueue = new LinkedBlockingQueue<>();
    private static final ThreadLocal<BlockPos.MutableBlockPos> mutablePosCache = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

    public static void init() {
        if (CoolConfig.ASYNC_PARTICLES.get()) {
            int threads = Math.max(1, Math.min(CoolConfig.maxthreads.get(), Runtime.getRuntime().availableProcessors()));
            executorService = new ThreadPoolExecutor(
                    threads, threads,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(),
                    r -> {
                        Thread t = new Thread(r, "Async particle Worker");
                        t.setDaemon(true);
                        return t;
                    }
            );

            LOGGER.info("Async particle System initialized with {} threads", threads);
        }
    }

    public static void shutdown() {
        if (executorService != null) {
            ExecutorService es = executorService;
            executorService = null;

            es.shutdown();
            try {
                if (!es.awaitTermination(3, TimeUnit.SECONDS)) {
                    es.shutdownNow();
                }
            } catch (InterruptedException e) {
                es.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void addParticle(Level level, ParticleOptions particle, double x, double y, double z,
                                   double xSpeed, double ySpeed, double zSpeed) {
        if (!CoolConfig.ASYNC_PARTICLES.get() || level.isClientSide) {
            level.addParticle(particle, x, y, z, xSpeed, ySpeed, zSpeed);
            return;
        }

        if (particleQueue.remainingCapacity() > 0) {
            particleQueue.offer(new ParticleTask(level, particle, x, y, z, xSpeed, ySpeed, zSpeed));
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.side == LogicalSide.SERVER) {
            processParticles();
        }
    }

    private static void processParticles() {
        int processed = 0;
        ParticleTask task;

        while (processed < MAX_PARTICLES_PER_TICK && (task = particleQueue.poll()) != null) {
            final ParticleTask finalTask = task;
            executorService.execute(() -> {
                ServerLevel serverLevel = (ServerLevel) finalTask.level;
                if (serverLevel != null) {
                    BlockPos.MutableBlockPos pos = mutablePosCache.get().set(finalTask.x(), finalTask.y(), finalTask.z());
                    if (serverLevel.isLoaded(pos)) {
                        serverLevel.sendParticles(
                                finalTask.particle(),
                                finalTask.x(), finalTask.y(), finalTask.z(),
                                1,
                                finalTask.xSpeed(), finalTask.ySpeed(), finalTask.zSpeed(),
                                1.0
                        );
                    }
                }
            });
            processed++;
        }
    }

    private record ParticleTask(
            Level level,
            ParticleOptions particle,
            double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed
    ) {
    }
}