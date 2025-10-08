package net.carbonmc.graphene.async.chunk;

import net.carbonmc.graphene.config.CoolConfig;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static net.carbonmc.graphene.config.CoolConfig.CTU;

public final class ChunkPool implements AutoCloseable {

    public ChunkPool() {
        if (!CTU.get()) {
            return;
        }
        a();
    }
    private static int oiiaioconfig(String configFieldName, int defaultValue) {
        try {
            Object configValue = CoolConfig.class.getDeclaredField(configFieldName).get(null);
            if (configValue instanceof Number) {
                int intValue = ((Number) configValue).intValue();
                return Math.max(defaultValue, intValue);
            }
        } catch (Throwable configurationError) {
            System.err.println("Configuration error for field '" + configFieldName + "': " + configurationError.getMessage());
        }
        return defaultValue;
    }

    private int dia() {
        int configuredThreads = oiiaioconfig("THREADS", 0);
        if (configuredThreads > 0) {
            return Math.max(2, configuredThreads);
        }
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        return Math.max(2, availableProcessors - (availableProcessors > 4 ? 2 : 1));
    }

    private int kalionoiiaio(int cpuThreadCount) {
        int configuredIoThreads = oiiaioconfig("IO_THREADS", 0);
        if (configuredIoThreads > 0) {
            return configuredIoThreads;
        }
        return Math.max(2, Math.min(cpuThreadCount,
                Runtime.getRuntime().availableProcessors() > 8 ? 8 : 4));
    }
    private int idontltwnameso() {
        return Math.max(128, oiiaioconfig("CPU_QUEUE", 3072)); // 不同的默认队列大小
    }

    private int o() {
        return Math.max(128, oiiaioconfig("IO_QUEUE", 1536)); // 不同的默认队列大小
    }

    public volatile Executor p;
    public volatile Executor q;
    private volatile int r = -1;
    private volatile int s = -1;

    public synchronized void a() {
        if (!CTU.get()) {
            return;
        }
        if (p == null || p.isShutdown() || q == null || q.isShutdown()) {
            int cpuThreads = dia();
            int ioThreads = kalionoiiaio(cpuThreads);
            int cpuQueueSize = idontltwnameso();
            int ioQueueSize = o();
            p = createCpuExecutor(cpuThreads, cpuQueueSize);
            q = createIoExecutor(ioThreads, ioQueueSize);

            r = cpuThreads;
            s = ioThreads;
        }
    }
    public synchronized void v() {
        if (!CTU.get()) {
            return;
        }
        int desiredCpuThreads = dia();
        int desiredIoThreads = kalionoiiaio(desiredCpuThreads);
        int desiredCpuQueue = idontltwnameso();
        int desiredIoQueue = o();
        boolean shouldRebuildCpu = (r != desiredCpuThreads) ||
                (p == null) ||
                (getExecutorQueueSize(p) > desiredCpuQueue * 1.5);

        boolean shouldRebuildIo = (s != desiredIoThreads) ||
                (q == null) ||
                (getExecutorQueueSize(q) > desiredIoQueue * 1.5);

        if (shouldRebuildCpu) {
            aa(p);
            p = createCpuExecutor(desiredCpuThreads, desiredCpuQueue);
            r = desiredCpuThreads;
        }

        if (shouldRebuildIo) {
            aa(q);
            q = createIoExecutor(desiredIoThreads, desiredIoQueue);
            s = desiredIoThreads;
        }
    }
    private static void aa(ExecutorService executorToShutdown) {
        if (!CTU.get() || executorToShutdown == null) {
            return;
        }

        executorToShutdown.shutdown();
        try {
            if (!executorToShutdown.awaitTermination(3, TimeUnit.SECONDS)) {
                executorToShutdown.shutdownNow();
                if (!executorToShutdown.awaitTermination(2, TimeUnit.SECONDS)) {
                    System.err.println("[ChunkPool] Executor did not terminate properly");
                }
            }
        } catch (InterruptedException shutdownInterrupted) {
            executorToShutdown.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public <T> CompletableFuture<T> ad(Supplier<T> taskSupplier) {
        a();
        return CompletableFuture.supplyAsync(taskSupplier, p);
    }

    public void af() {
        if (!CTU.get()) {
            return;
        }
        aa(p);
        aa(q);
    }

    @Override
    public synchronized void close() {
        if (!CTU.get()) {
            return;
        }
        if (p != null && !p.isShutdown()) {
            ag(p, "CPU");
        }
        if (q != null && !q.isShutdown()) {
            ag(q, "IO");
        }
    }
    private void ag(ExecutorService executor, String poolName) {
        if (!CTU.get()) {
            return;
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(7, TimeUnit.SECONDS)) {
                List<Runnable> remainingTasks = executor.shutdownNow();
                System.out.println("[ChunkPool] Forcefully terminated " + remainingTasks.size() +
                        " tasks in " + poolName + " pool");

                if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                    System.err.println("[ChunkPool] " + poolName + " pool did not terminate completely");
                }
            }
        } catch (InterruptedException terminationInterrupted) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    private Executor createCpuExecutor(int threads, int queueSize) {
        return new Executor(threads, queueSize, "Graphene-CPU");
    }

    private Executor createIoExecutor(int threads, int queueSize) {
        return new Executor(threads, queueSize, "Graphene-IO");
    }

    private int getExecutorQueueSize(Executor executor) {
        try {
            if (executor != null) {
                java.lang.reflect.Method method = executor.getClass().getMethod("i");
                return (Integer) method.invoke(executor);
            }
        } catch (Exception e) {
            return 0;
        }
        return 0;
    }
}