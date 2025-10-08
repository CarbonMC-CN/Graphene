package net.carbonmc.graphene.event;

import net.carbonmc.graphene.config.CoolConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public abstract class AsyncEvent extends Event {
    private final boolean async;
    private final EventPriority priority;
    private volatile boolean completed = false;
    private volatile boolean success = false;
    private Throwable failureCause = null;

    protected AsyncEvent(boolean async) {
        this(async, EventPriority.NORMAL);
    }

    protected AsyncEvent(boolean async, EventPriority priority) {
        this.async = async;
        this.priority = priority;
    }

    public boolean isAsync() {
        return async;
    }

    public EventPriority getPriority() {
        return priority;
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean isSuccess() {
        return success;
    }

    public Throwable getFailureCause() {
        return failureCause;
    }

    public void post() {
        if (!CoolConfig.isEnabled()) {
            return;
        }

        if (async) {
            postAsync();
        } else {
            postSync();
        }
    }

    private void postAsync() {
        CompletableFuture<Void> future = AsyncEventSystem.executeAsync(getClass(), () -> {
            long startTime = System.currentTimeMillis();
            try {
                MinecraftForge.EVENT_BUS.post(this);
                success = true;
                logDuration(startTime, true);
            } catch (Throwable t) {
                handleError(t);
                throw t;
            } finally {
                completed = true;
            }
        });

        if (CoolConfig.WAIT_FOR_ASYNC_EVENTS.get()) {
            waitForFuture(future);
        }
    }

    private void postSync() {
        long startTime = System.currentTimeMillis();
        try {
            MinecraftForge.EVENT_BUS.post(this);
            success = true;
            logDuration(startTime, false);
        } catch (Throwable t) {
            handleError(t);
            throw t;
        } finally {
            completed = true;
        }
    }

    private void logDuration(long startTime, boolean isAsync) {
        long duration = System.currentTimeMillis() - startTime;
        if (duration > (isAsync ? 100 : 50)) {
            AsyncEventSystem.LOGGER.warn("{} event {} took {}ms",
                    isAsync ? "Async" : "Sync",
                    getClass().getSimpleName(),
                    duration);
        }
    }

    private void handleError(Throwable t) {
        failureCause = t;
        success = false;
        AsyncEventSystem.LOGGER.error("Event handling failed: {}", getClass().getSimpleName(), t);
    }

    private void waitForFuture(CompletableFuture<Void> future) {
        try {
            future.get(CoolConfig.ASYNC_EVENT_TIMEOUT.get(), TimeUnit.SECONDS);
        } catch (Exception e) {
            AsyncEventSystem.LOGGER.warn("Async event timed out: {}", getClass().getSimpleName(), e);
        }
    }

    public void registerToBus(IEventBus bus) {
        bus.addListener(priority, false, getClass(), this::handleEventWrapper);
    }

    private void handleEventWrapper(Event event) {
        if (!CoolConfig.isEnabled() || !(event instanceof AsyncEvent)) {
            return;
        }

        AsyncEvent asyncEvent = (AsyncEvent) event;
        try {
            handleEvent(asyncEvent);
        } catch (Throwable t) {
            AsyncEventSystem.LOGGER.error("Handler failed for {}", event.getClass().getName(), t);
            if (CoolConfig.DISABLE_ASYNC_ON_ERROR.get()) {
                AsyncEventSystem.registerSyncEvent(event.getClass());
            }
            throw t;
        }
    }

    protected abstract void handleEvent(AsyncEvent event);

    public static void waitForCompletion(AsyncEvent event) {
        if (!CoolConfig.isEnabled() || event == null || !event.isAsync()) {
            return;
        }

        while (!event.isCompleted()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public static void waitForCompletion(AsyncEvent... events) {
        if (!CoolConfig.isEnabled() || events == null) {
            return;
        }

        for (AsyncEvent event : events) {
            waitForCompletion(event);
        }
    }
}