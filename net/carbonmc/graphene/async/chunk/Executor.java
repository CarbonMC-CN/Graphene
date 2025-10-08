package net.carbonmc.graphene.async.chunk;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import static net.carbonmc.graphene.config.CoolConfig.CTU;

public final class Executor extends ThreadPoolExecutor {
    private final LongAdder submittedTaskCounter = new LongAdder();
    private final LongAdder completedTaskCounter = new LongAdder();
    private final AtomicLong lastTaskTimestamp = new AtomicLong();

    public Executor(int corePoolSize,
                    int maximumQueueCapacity,
                    String threadNamePrefix) {

        super(corePoolSize,
                corePoolSize,
                45L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(Math.max(8, maximumQueueCapacity)),
                new ThreadFactoryImpl(threadNamePrefix),
                new AdaptiveRejectionPolicy());
        allowCoreThreadTimeOut(true);
    }
    @Override
    public void execute(@NotNull Runnable command) {
        if (!CTU.get()) {

            command.run();
            return;
        }
        submittedTaskCounter.increment();
        lastTaskTimestamp.set(System.currentTimeMillis());
        super.execute(new TrackedTask(command));
    }
    public int i() {
        return getQueue().size();
    }
    private static final class ThreadFactoryImpl implements ThreadFactory {
        private final ThreadFactory defaultFactory = Executors.defaultThreadFactory();
        private final String namePrefix;
        private final AtomicLong threadCounter = new AtomicLong(0);

        ThreadFactoryImpl(String prefix) {
            this.namePrefix = prefix != null ? prefix : "Graphene-Worker";
        }
        @Override
        public Thread newThread(@NotNull Runnable runnable) {
            Thread thread = defaultFactory.newThread(runnable);
            thread.setName(namePrefix + "-T" + threadCounter.incrementAndGet());
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        }
    }

    private final class TrackedTask implements Runnable {
        private final Runnable originalTask;
        TrackedTask(Runnable task) {
            this.originalTask = task;
        }

        @Override
        public void run() {
            System.nanoTime();
            try {
                originalTask.run();
            } catch (Throwable throwable) {

                System.err.println("Task execution failed: " + throwable.getMessage());
                throwable.printStackTrace();
            } finally {
                completedTaskCounter.increment();
            }
        }
    }


    private static final class AdaptiveRejectionPolicy implements RejectedExecutionHandler {
        private final RejectedExecutionHandler fallback = new ThreadPoolExecutor.CallerRunsPolicy();
        private volatile long lastWarningTime = 0;
        private static final long WARNING_INTERVAL = TimeUnit.MINUTES.toMillis(1);
        @Override
        public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
            if (executor.isShutdown()) {
                throw new RejectedExecutionException("Executor has been shutdown");
            }
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastWarningTime > WARNING_INTERVAL) {
                System.err.println("Thread pool queue is full. Queue size: " + executor.getQueue().size());
                lastWarningTime = currentTime;
            }
            fallback.rejectedExecution(runnable, executor);
        }
    }

    @Override
    protected void beforeExecute(Thread thread, Runnable runnable) {
        if (CTU.get()) {
            thread.setContextClassLoader(getClass().getClassLoader());
        }
    }

    @Override
    protected void afterExecute(Runnable runnable, Throwable throwable) {
        if (throwable != null && CTU.get()) {
            System.err.println("Task completed with exception: " + throwable.getMessage());
        }
    }

    @Override
    public void shutdown() {
        if (CTU.get()) {
            System.out.println("Shutting down executor: " + this);
        }
        super.shutdown();
    }
}