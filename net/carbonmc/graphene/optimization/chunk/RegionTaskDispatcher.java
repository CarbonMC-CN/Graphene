package net.carbonmc.graphene.optimization.chunk;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static net.carbonmc.graphene.config.CoolConfig.CTU;

public class RegionTaskDispatcher implements AutoCloseable {
    private final Function<Long, ExecutorService> executorProvider;
    private final ConcurrentMap<Long, ManagedExecutor> regionExecutors = new ConcurrentHashMap<>();
    private final RejectedExecutionHandler rejectionHandler;
    private final DispatcherMetrics metrics;
    private final ExecutorService cleanupExecutor;
    private volatile boolean active = true;

    public RegionTaskDispatcher(Function<Long, ExecutorService> executorProvider) {
        this.metrics = new DispatcherMetrics();
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "RegionDispatcher-Cleanup");
            t.setDaemon(true);
            return t;
        });
        this.executorProvider = executorProvider;
        this.rejectionHandler = new AdaptiveRejectionPolicy();
        startCleanupTask();
        GlobalExecutorManager.registerResource(this);
    }

    public void dispatch(long regionId, Runnable task) {
        if (!CTU.get() || !active) {
            task.run();
            return;
        }

        metrics.recordDispatchAttempt();

        try {
            ManagedExecutor executor = getOrCreateExecutor(regionId);
            if (executor == null) {
                handleExecutorCreationFailure(regionId, task);
                return;
            }

            if (executor.isQueueFull()) {
                rejectionHandler.rejectedExecution(task, executor.getThreadPoolExecutor());
                return;
            }

            executor.execute(createTrackedTask(task, regionId));
            metrics.recordSuccessfulDispatch();

        } catch (Exception e) {
            handleDispatchException(regionId, task, e);
        }
    }
    private Runnable createTrackedTask(Runnable originalTask, long regionId) {
        return new TrackedTask(originalTask, regionId);
    }
    private void handleExecutorCreationFailure(long regionId, Runnable task) {
        System.err.println("Executor creation failed for region " + regionId + ", executing in caller thread");
        metrics.recordDispatchFailure();
        task.run();
    }
    private void handleDispatchException(long regionId, Runnable task, Exception exception) {
        System.err.println("Dispatch failed for region " + regionId + ": " + exception.getMessage());
        metrics.recordDispatchFailure();
        rejectionHandler.rejectedExecution(task, null);
    }
    public interface RegionExecutor {
        long getRegionId();
    }
    private ManagedExecutor getOrCreateExecutor(long regionId) {
        return regionExecutors.computeIfAbsent(regionId, id -> {
            try {
                ExecutorService backend = executorProvider.apply(id);
                return new ManagedExecutor(backend, metrics);
            } catch (Exception e) {
                System.err.println("Failed to create executor for region " + id + ": " + e.getMessage());
                return null;
            }
        });
    }
    private static class AdaptiveRejectionPolicy implements RejectedExecutionHandler {
        private final Map<Long, Long> lastWarningTime = new ConcurrentHashMap<>();
        private static final long WARNING_INTERVAL_MS = 3000;

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            long regionId = extractRegionId(executor);
            long currentTime = System.currentTimeMillis();
            Long lastWarn = lastWarningTime.get(regionId);

            if (lastWarn == null || currentTime - lastWarn > WARNING_INTERVAL_MS) {
                System.err.println("[RegionDispatcher] Task rejected for region " + regionId +
                        (executor != null ? ", queue size: " + executor.getQueue().size() : "") +
                        ", active threads: " + (executor != null ? executor.getActiveCount() : "N/A"));
                lastWarningTime.put(regionId, currentTime);
            }
            if (!Thread.currentThread().isInterrupted()) {
                r.run();
            }
        }

        private long extractRegionId(ThreadPoolExecutor executor) {
            if (executor instanceof RegionExecutor) {
                return ((RegionExecutor) executor).getRegionId();
            }
            return -1L;
        }
    }

    @Override
    public void close() {
        if (!CTU.get()) {
            return;
        }
        gracefulShutdown();
    }
    public void shutdown() {
        if (!CTU.get()) {
            return;
        }
        this.active = false;
        metrics.recordShutdown();

        regionExecutors.values().forEach(exec -> {
            if (exec != null) {
                exec.shutdown();
            }
        });
        regionExecutors.clear();

        cleanupExecutor.shutdown();
    }

    public List<Runnable> shutdownNow() {
        this.active = false;
        List<Runnable> allPending = new ArrayList<>();

        regionExecutors.values().forEach(exec -> {
            if (exec != null) {
                allPending.addAll(exec.shutdownNow());
            }
        });
        regionExecutors.clear();

        metrics.recordForcedShutdown();
        return allPending;
    }

    public boolean isShutdown() {
        return !this.active && regionExecutors.values().stream()
                .allMatch(exec -> exec == null || exec.isShutdown());
    }

    public boolean isTerminated() {
        return regionExecutors.values().stream()
                .allMatch(exec -> exec == null || exec.isTerminated());
    }
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);

        for (ManagedExecutor exec : regionExecutors.values()) {
            if (exec != null) {
                long remaining = Math.max(0, deadline - System.nanoTime());
                if (!exec.awaitTermination(remaining)) {
                    return false;
                }
            }
        }
        return true;
    }
    private void startCleanupTask() {
        ((ScheduledExecutorService) cleanupExecutor).scheduleAtFixedRate(() -> {
            if (!active) return;

            long now = System.currentTimeMillis();
            Iterator<Map.Entry<Long, ManagedExecutor>> iterator = regionExecutors.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<Long, ManagedExecutor> entry = iterator.next();
                ManagedExecutor executor = entry.getValue();

                if (executor != null && executor.shouldCleanup(now)) {
                    iterator.remove();
                    executor.shutdown();
                    System.out.println("Cleaned up inactive executor for region " + entry.getKey());
                }
            }
        }, 2, 2, TimeUnit.MINUTES);
    }

    private static class DispatcherMetrics {
        private final AtomicLong dispatchAttempts = new AtomicLong();
        private final AtomicLong successfulDispatches = new AtomicLong();
        private final AtomicLong dispatchFailures = new AtomicLong();
        private final AtomicLong totalRegions = new AtomicLong();
        private final AtomicLong shutdownCount = new AtomicLong();
        void recordDispatchAttempt() { dispatchAttempts.incrementAndGet(); }
        void recordSuccessfulDispatch() { successfulDispatches.incrementAndGet(); }
        void recordDispatchFailure() { dispatchFailures.incrementAndGet(); }
        void recordNewRegion() { totalRegions.incrementAndGet(); }
        void recordShutdown() { shutdownCount.incrementAndGet(); }
        void recordForcedShutdown() {}

    }
    private static class ManagedExecutor {
        private final ExecutorService delegate;
        private final AtomicLong lastActivityTime = new AtomicLong(System.currentTimeMillis());
        private final AtomicInteger queueSize = new AtomicInteger(0);
        ManagedExecutor(ExecutorService delegate, DispatcherMetrics metrics) {
            this.delegate = delegate;
            metrics.recordNewRegion();
        }

        void execute(Runnable task) {
            queueSize.incrementAndGet();
            lastActivityTime.set(System.currentTimeMillis());

            delegate.execute(() -> {
                try {
                    task.run();
                } finally {
                    queueSize.decrementAndGet();
                    lastActivityTime.set(System.currentTimeMillis());
                }
            });
        }

        boolean isQueueFull() {
            return queueSize.get() >= 2500;
        }

        boolean shouldCleanup(long currentTime) {
            return currentTime - lastActivityTime.get() > TimeUnit.MINUTES.toMillis(5); // 5分钟无活动
        }
        ThreadPoolExecutor getThreadPoolExecutor() {
            return delegate instanceof ThreadPoolExecutor ? (ThreadPoolExecutor) delegate : null;
        }
        boolean isShutdown() { return delegate.isShutdown(); }
        boolean isTerminated() { return delegate.isTerminated(); }
        boolean awaitTermination(long timeout) throws InterruptedException {
            return delegate.awaitTermination(timeout, TimeUnit.NANOSECONDS);
        }
        List<Runnable> shutdownNow() { return delegate.shutdownNow(); }
        void shutdown() { delegate.shutdown(); }
    }

    private static class TrackedTask implements Runnable {
        private final Runnable delegate;
        private final long regionId;
        private final long submissionTime;

        TrackedTask(Runnable delegate, long regionId) {
            this.delegate = delegate;
            this.regionId = regionId;
            this.submissionTime = System.nanoTime();
        }

        @Override
        public void run() {
            try {
                delegate.run();
                long duration = System.nanoTime() - submissionTime;
                if (duration > TimeUnit.SECONDS.toNanos(10)) {
                    System.err.println("Long running task in region " + regionId +
                            ": " + TimeUnit.NANOSECONDS.toMillis(duration) + "ms");
                }
            } catch (Exception e) {
                System.err.println("Task failed in region " + regionId + ": " + e.getMessage());
                throw e;
            }
        }
    }
    private void gracefulShutdown() {
        shutdown();
        try {
            if (!awaitTermination(10, TimeUnit.SECONDS)) {
                shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            shutdownNow();
        }
    }
}