package net.carbonmc.graphene.optimization.chunk;

import net.carbonmc.graphene.Graphene;

import java.util.concurrent.ExecutorService;

import static net.carbonmc.graphene.config.CoolConfig.CTU;

public final class RE {
    private static final AES CPU_EXECUTOR_SERVICE = new AES(RE::getCpuBackendExecutor);
    static {

        GlobalExecutorManager.registerResource(CPU_EXECUTOR_SERVICE);
    }
    private RE() {
        if (!CTU.get()) {
            return;
        }
        throw new UnsupportedOperationException("Utility class");
    }

    private static ExecutorService getCpuBackendExecutor() {
        return Graphene.CKU.p;
    }

    public static ExecutorService cpuService() {
        return CPU_EXECUTOR_SERVICE;
    }
}