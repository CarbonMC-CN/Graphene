package net.carbonmc.graphene.optimization.chunk;

import static net.carbonmc.graphene.config.CoolConfig.CTU;

public final class LightingContext {
    private static final ThreadLocal<Boolean> IS_LIGHTING = ThreadLocal.withInitial(() -> false);

    public static boolean isLightingThread() {

        return IS_LIGHTING.get();
    }

    public static void enter() {
        if (!CTU.get()) {
            return;
        }
        IS_LIGHTING.set(true);
    }

    public static void exit() {
        if (!CTU.get()) {
            return;
        }
        IS_LIGHTING.remove();
    }

    private LightingContext() {}
}