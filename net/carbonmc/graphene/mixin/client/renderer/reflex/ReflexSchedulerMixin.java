package net.carbonmc.graphene.mixin.client.renderer.reflex;

import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL33.*;

@OnlyIn(Dist.CLIENT)
@Mixin(GameRenderer.class)
public abstract class ReflexSchedulerMixin {

    // 计时模式常量
    private static final int MODE_DISABLED = 0;
    private static final int MODE_TIMESTAMP = 1; // 使用 glQueryCounter (GL_ARB_timer_query)
    private static final int MODE_ELAPSED = 2;   // 使用 GL_TIME_ELAPSED (兼容模式)

    @Shadow @Final private Minecraft minecraft;
    private static final Logger LOGGER = LogManager.getLogger("Graphene-Reflex");
    private static final long MAX_WAIT_NS = 2_000_000L;
    private static final long MIN_FRAME_NS = 1_000_000L;
    private static final double SMOOTH_ALPHA = 0.15;

    private int timingMode = MODE_DISABLED;
    private final int[] queryIds = new int[2];
    private int queryIndex = 0;

    private long lastGpuDoneNs = -1L;
    private long lastFrameEndNs = -1L;
    private double smoothedDeltaNs = 0.0;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void reflex$init(Minecraft p_234219_, ItemInHandRenderer p_234220_, ResourceManager p_234221_, RenderBuffers p_234222_, CallbackInfo ci) {
        // 检测最佳可用计时方式
        if (GL.getCapabilities().GL_ARB_timer_query) {
            timingMode = MODE_TIMESTAMP;
            glGenQueries(queryIds);
            LOGGER.info("Using high-precision timestamp queries");
        } else if (GL.getCapabilities().GL_EXT_timer_query ||
                GL.getCapabilities().GL_ARB_occlusion_query) {
            timingMode = MODE_ELAPSED;
            glGenQueries(queryIds);
            LOGGER.info("Using elapsed time queries (compatibility mode)");
        } else {
            LOGGER.warn("No supported GPU timing method available, Reflex disabled");
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void reflex$onCpuStart(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        if (!CoolConfig.enableReflex.get() || timingMode == MODE_DISABLED) return;

        final long cpuNow = System.nanoTime();

        // 1. 获取 GPU 完成时间
        long gpuDone = -1;
        switch (timingMode) {
            case MODE_TIMESTAMP:
                gpuDone = getGpuTimestamp(cpuNow);
                break;
            case MODE_ELAPSED:
                gpuDone = getGpuElapsedTime(cpuNow);
                break;
        }

        if (gpuDone > 0 && gpuDone < cpuNow) {
            lastGpuDoneNs = gpuDone;

            // 2. 计算并执行等待
            long cpuElapsed = cpuNow - lastGpuDoneNs;
            smoothedDeltaNs = SMOOTH_ALPHA * cpuElapsed + (1.0 - SMOOTH_ALPHA) * smoothedDeltaNs;

            long waitNs = (long) (smoothedDeltaNs + CoolConfig.reflexOffsetNs.get());
            waitNs = Math.max(-MAX_WAIT_NS, Math.min(MAX_WAIT_NS, waitNs));

            if (waitNs > 0) {
                smartWait(cpuNow, waitNs);
            }
        }

        // 3. 帧率控制
        int maxFps = CoolConfig.MAX_FPS.get();
        if (maxFps > 0 && lastFrameEndNs > 0) {
            long targetFrameTime = 1_000_000_000L / maxFps;
            long elapsed = cpuNow - lastFrameEndNs;
            long remaining = targetFrameTime - elapsed;

            if (remaining > MIN_FRAME_NS) {
                smartWait(cpuNow, remaining);
            }
        }

        if (CoolConfig.reflexDebug.get()) {
            LOGGER.debug("Reflex stats - Mode: {}, GPU: {}ns, CPU: {}ns, Delta: {}ns",
                    timingModeToString(), lastGpuDoneNs, lastFrameEndNs, smoothedDeltaNs);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void reflex$onCpuEnd(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        if (timingMode == MODE_DISABLED || !CoolConfig.enableReflex.get()) return;

        switch (timingMode) {
            case MODE_TIMESTAMP:
                glQueryCounter(queryIds[queryIndex], GL_TIMESTAMP);
                break;
            case MODE_ELAPSED:
                glBeginQuery(GL_TIME_ELAPSED, queryIds[queryIndex]);
                glEndQuery(GL_TIME_ELAPSED);
                break;
        }
        queryIndex ^= 1;
        lastFrameEndNs = System.nanoTime();
    }

    private long getGpuTimestamp(long cpuNow) {
        int prev = queryIndex ^ 1;
        if (!glIsQuery(queryIds[prev])) return -1;

        int[] ready = {0};
        glGetQueryObjectiv(queryIds[prev], GL_QUERY_RESULT_AVAILABLE, ready);
        if (ready[0] == 0) return -1;

        long gpuTime = glGetQueryObjecti64(queryIds[prev], GL_QUERY_RESULT);
        return (gpuTime > 0 && gpuTime < cpuNow) ? gpuTime : -1;
    }

    private long getGpuElapsedTime(long cpuNow) {
        int prev = queryIndex ^ 1;
        if (!glIsQuery(queryIds[prev])) return -1;

        int[] ready = {0};
        glGetQueryObjectiv(queryIds[prev], GL_QUERY_RESULT_AVAILABLE, ready);
        if (ready[0] == 0) return -1;

        int[] timeNs = {0};
        glGetQueryObjectiv(queryIds[prev], GL_QUERY_RESULT, timeNs);

        // 使用上一帧结束时间作为基准
        return (lastFrameEndNs > 0) ? lastFrameEndNs + timeNs[0] * 1_000_000L : -1;
    }

    private void smartWait(long startTime, long waitNs) {
        long endTime = startTime + waitNs;
        long currentTime;

        // 初始忙等待
        while ((currentTime = System.nanoTime()) < endTime - 100_000L) {
            Thread.onSpinWait();
        }

        // 最后阶段更精确的等待
        while (System.nanoTime() < endTime) {
            try {
                Thread.sleep(0, 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private String timingModeToString() {
        switch (timingMode) {
            case MODE_TIMESTAMP: return "TIMESTAMP";
            case MODE_ELAPSED: return "ELAPSED";
            default: return "DISABLED";
        }
    }
}