package net.carbonmc.graphene.optimization.chunk;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static net.carbonmc.graphene.config.CoolConfig.CTU;

public final class GlobalExecutorManager {
    private static final List<AutoCloseable> RESOURCES = new ArrayList<>();
    private static volatile boolean SHUTTING_DOWN = false;

    static {

        Runtime.getRuntime().addShutdownHook(new Thread(GlobalExecutorManager::shutdownAll, "Graphene-Shutdown-Hook"));
    }

    public static void registerResource(AutoCloseable resource) {
        if (!CTU.get()) {
            return;
        }
        if (SHUTTING_DOWN) {
            System.err.println("Warning: Registering resource during shutdown");
            return;
        }
        synchronized (RESOURCES) {
            RESOURCES.add(resource);
        }
    }
    public static void shutdownAll() {
        if (!CTU.get()) {
            return;
        }
        SHUTTING_DOWN = true;
        System.out.println("[Graphene] Shutting down all executors...");

        List<AutoCloseable> toShutdown;
        synchronized (RESOURCES) {
            toShutdown = new ArrayList<>(RESOURCES);
            RESOURCES.clear();
        }
        toShutdown.forEach(resource -> {
            try {
                if (resource instanceof ExecutorService) {
                    ((ExecutorService) resource).shutdown();
                } else {
                    resource.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        toShutdown.forEach(resource -> {
            try {
                if (resource instanceof ExecutorService es) {
                    if (!es.isTerminated()) {
                        List<Runnable> remaining = es.shutdownNow();
                        if (!remaining.isEmpty()) {
                            System.out.println("[Graphene] Forcefully terminated " + remaining.size() + " tasks");
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        System.out.println("[Graphene] Shutdown complete");
    }

}