package net.carbonmc.graphene.mixin.client.renderer.reflex;

import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.opengl.GL32C.*;
import static org.lwjgl.opengl.GL33C.*;

@Mixin(GameRenderer.class)
public abstract class ReflexSchedulerMixin {

    @Shadow @Final private Minecraft minecraft;
    private static final Logger LOGGER = LogManager.getLogger("Graphene-Reflex");
    private static final long MAX_WAIT_NS = 2_000_000L;
    private static final double SMOOTH_ALPHA = 0.15;

    private final int[] queryIds = new int[2];
    private int queryIndex = 0;
    private boolean supported = false;

    private long lastGpuDoneNs = -1L;
    private double smoothedDeltaNs = 0.0;

    private void ensureInit() {
        if (!supported) {
            supported = GL.getCapabilities().GL_ARB_timer_query;
            if (supported) glGenQueries(queryIds);
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void reflex$onCpuStart(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        if (!CoolConfig.enableReflex.get()) return;
        ensureInit();
        if (!supported) return;

        long cpuNow = System.nanoTime();

        int prev = queryIndex ^ 1;
        if (supported && glIsQuery(queryIds[prev])) {
            int[] ready = new int[1];
            glGetQueryObjectiv(queryIds[prev], GL_QUERY_RESULT_AVAILABLE, ready);
            if (ready[0] != 0) {
                lastGpuDoneNs = glGetQueryObjecti64(queryIds[prev], GL_QUERY_RESULT);
            }
        }

        if (lastGpuDoneNs <= 0) return;

        long cpuElapsed = cpuNow - lastGpuDoneNs;
        smoothedDeltaNs = SMOOTH_ALPHA * cpuElapsed + (1.0 - SMOOTH_ALPHA) * smoothedDeltaNs;

        long waitNs = (long) (smoothedDeltaNs + CoolConfig.reflexOffsetNs.get());
        waitNs = Math.max(-MAX_WAIT_NS, Math.min(MAX_WAIT_NS, waitNs));

        if (waitNs > 0) {
            long target = cpuNow + waitNs;
            while (System.nanoTime() < target) Thread.onSpinWait();
        }

        if (CoolConfig.reflexDebug.get()) {
            LOGGER.debug("Reflex wait={} ns", waitNs);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void reflex$onCpuEnd(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        if (!supported || !CoolConfig.enableReflex.get()) return;

        glQueryCounter(queryIds[queryIndex], GL_TIMESTAMP);
        queryIndex ^= 1;
    }
}