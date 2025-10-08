package net.carbonmc.graphene.optimization.chunk;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import static net.carbonmc.graphene.config.CoolConfig.CTU;
public final class AES extends AbstractExecutorService implements AutoCloseable {
    private final Supplier<ExecutorService> executorProvider;
    private final RegionTaskDispatcher taskDispatcher;
    private final ExecutionMetrics metrics;
    private volatile boolean active;
    public AES(Supplier<ExecutorService> executorProvider) {
        this.executorProvider = executorProvider;
        this.metrics = new ExecutionMetrics();
        this.active = true;
        this.taskDispatcher = new RegionTaskDispatcher(this::getRegionExecutor);

        GlobalExecutorManager.registerResource(this);
        registerShutdownHook();
    }
    @Override
    public <T> @NotNull Future<T> submit(@NotNull Callable<T> task) {
        if (!active) {
            throw new RejectedExecutionException("Executor has been shutdown");
        }
        return super.submit(task);
    }

    @Override
    public <T> @NotNull Future<T> submit(@NotNull Runnable task, T result) {
        if (!active) {
            throw new RejectedExecutionException("Executor has been shutdown");
        }
        return super.submit(task, result);
    }

    @Override
    public @NotNull Future<?> submit(@NotNull Runnable task) {
        if (!active) {
            throw new RejectedExecutionException("Executor has been shutdown");
        }
        return super.submit(task);
    }

    @Override
    public <T> @NotNull List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        if (!active) {
            throw new RejectedExecutionException("Executor has been shutdown");
        }
        return super.invokeAll(tasks);
    }
    @Override
    public void execute(@NotNull Runnable command) {
        if (!CTU.get() || !active) {

            command.run();
            return;
        }

        metrics.recordSubmission();
        Long currentRegionId = ChunkOptif.currentRegion();

        if (currentRegionId == null) {
            executeWithoutRegionContext(command);
        } else {
            executeWithRegionContext(currentRegionId, command);
        }
    }
    private void gracefulShutdown() {
        this.active = false;
        metrics.recordShutdown();
        try {

            if (!awaitTermination(5, TimeUnit.SECONDS)) {
                shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            shutdownNow();
        }
    }
    private void configureRegionExecutor(ThreadPoolExecutor executor, Long regionId) {

        executor.setKeepAliveTime(25L, TimeUnit.SECONDS);
        executor.setRejectedExecutionHandler(new RegionAwareRejectionPolicy(regionId));
    }

    private ExecutorService getBackendExecutor() {
        return this.executorProvider.get();
    }

    @Override
    public void close() {
        if (!CTU.get()) {
            return;
        }
        gracefulShutdown();
    }
    private void handleTaskException(Exception exception, Long regionId) {
        System.err.println("Task execution failed" +
                (regionId != null ? " in region " + regionId : "") +
                ": " + exception.getMessage());
        metrics.recordFailure();
    }
    private void ensureCleanup(boolean taskSuccess) {
        try {
            ChunkOptif.cleanup();
        } catch (Exception cleanupException) {
            if (taskSuccess) {
                System.err.println("Cleanup failed after successful task execution: " + cleanupException.getMessage());
            }
        }
    }
    private ExecutorService getRegionExecutor(Long regionId) {
        ExecutorService backend = executorProvider.get();
        if (backend instanceof ThreadPoolExecutor) {
            configureRegionExecutor((ThreadPoolExecutor) backend, regionId);
        }
        return backend;
    }
    private void executeWithoutRegionContext(Runnable command) {
        this.getBackendExecutor().execute(createTrackedTask(command, null));
    }

    private void executeWithRegionContext(Long regionId, Runnable command) {
        this.taskDispatcher.dispatch(regionId, createTrackedTask(command, regionId));
    }

    private Runnable createTrackedTask(Runnable originalTask, Long regionId) {
        return () -> {
            final long startTime = System.nanoTime();
            boolean success = false;

            try {
                originalTask.run();
                success = true;
                metrics.recordCompletion(System.nanoTime() - startTime);
            } catch (Exception e) {
                handleTaskException(e, regionId);
                throw e;
            } finally {
                ensureCleanup(success);
            }
        };
    }

    @Override
    public void shutdown() {
        if (!CTU.get()) {
            return;
        }
        this.active = false;
        this.getBackendExecutor().shutdown();
        this.taskDispatcher.shutdown();
        metrics.recordShutdown();
    }
    @Override
    public @NotNull List<Runnable> shutdownNow() {
        this.active = false;
        List<Runnable> pendingTasks = this.taskDispatcher.shutdownNow();
        pendingTasks.addAll(this.getBackendExecutor().shutdownNow());
        metrics.recordForcedShutdown();
        return pendingTasks;
    }
    @Override
    public boolean isShutdown() {
        return !this.active &&
                this.getBackendExecutor().isShutdown() &&
                this.taskDispatcher.isShutdown();
    }
    @Override
    public boolean isTerminated() {
        return this.getBackendExecutor().isTerminated() &&
                this.taskDispatcher.isTerminated();
    }
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);

        boolean backendDone = this.getBackendExecutor().awaitTermination(timeout, unit);
        long remaining = Math.max(0, deadline - System.nanoTime());

        boolean dispatcherDone = this.taskDispatcher.awaitTermination(
                remaining, TimeUnit.NANOSECONDS);

        return backendDone && dispatcherDone;
    }

    @Override
    public <T> @NotNull List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks,
                                                  long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        if (!active) {
            throw new RejectedExecutionException("Executor has been shutdown");
        }
        return super.invokeAll(tasks, timeout, unit);
    }
    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::gracefulShutdown,
                "AES-Shutdown-Hook"));
    }
    private static class ExecutionMetrics {
        private final AtomicLong submissions = new AtomicLong();
        private final AtomicLong completions = new AtomicLong();
        private final AtomicLong failures = new AtomicLong();
        private final AtomicLong totalExecutionTime = new AtomicLong();

        void recordSubmission() { submissions.incrementAndGet(); }
        void recordCompletion(long durationNanos) {
            completions.incrementAndGet();
            totalExecutionTime.addAndGet(durationNanos);
        }
        void recordFailure() { failures.incrementAndGet(); }
        void recordShutdown() { }
        void recordForcedShutdown() { }
    }

    private record RegionAwareRejectionPolicy(Long regionId) implements RejectedExecutionHandler {

        @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                System.err.println("Task rejected for region " + regionId +
                        ", queue size: " + executor.getQueue().size());
                if (!executor.isShutdown()) {
                    r.run();
                }
            }
        }
}