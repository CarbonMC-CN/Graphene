package net.carbonmc.graphene.mixin.chunk.newthing;

import net.carbonmc.graphene.Graphene;
import net.carbonmc.graphene.config.CoolConfig;
import net.carbonmc.graphene.optimization.chunk.LightingContext;
import net.carbonmc.graphene.optimization.chunk.RE;
import net.minecraft.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.carbonmc.graphene.config.CoolConfig.CTU;

@Mixin(Util.class)
public abstract class ExecutorInterceptor {
    @Inject(method = "ioPool", at = @At("HEAD"), cancellable = true)
    private static void interceptIOPool(CallbackInfoReturnable<Object> callback) {
        if (graphene$shouldInterceptExecution(true)) {
            return;
        }
        try {
            if (Graphene.CKU != null && Graphene.CKU.p != null) {
                callback.setReturnValue(Graphene.CKU.p);
            } else {
                System.err.println("IO执行不可用，换用默认");
            }
        } catch (Exception ioExecutorException) {
            System.err.println("未能获取IO执行器： " + ioExecutorException.getMessage());
        }
    }
    @Unique
    private static boolean graphene$isExecutionInterceptionEnabled() {
        return Graphene.OpenC && CTU.get();
    }
    @Unique
    private static boolean graphene$isIOPoolRedirectionEnabled() {
        try {
            return Boolean.TRUE.equals(CoolConfig.CHUNK_REDIRECT_IO.get());
        } catch (Exception configurationException) {
            System.err.println("Failed to read IO redirection config: " + configurationException.getMessage());
            return true;
        }
    }
    @Unique
    private static boolean graphene$shouldBypassLightingRedirection() {
        try {
            return !Boolean.TRUE.equals(CoolConfig.CHUNk_REDIRECT_LIGHTING.get());
        } catch (Exception configurationException) {
            System.err.println("Failed to read lighting redirection config: " + configurationException.getMessage());
            return true;
        }
    }
    @Unique
    private static boolean graphene$shouldProcessCurrentContext() {
        boolean isLightingOperation = LightingContext.isLightingThread();
        boolean bypassLighting = graphene$shouldBypassLightingRedirection();
        return !(bypassLighting && isLightingOperation);
    }
    @Unique
    private static boolean graphene$shouldInterceptExecution(boolean requireIoRedirection) {
        if (!graphene$isExecutionInterceptionEnabled()) {
            return true;
        }
        if (requireIoRedirection && !graphene$isIOPoolRedirectionEnabled()) {
            return true;
        }
        return !graphene$shouldProcessCurrentContext();
    }
    @Inject(method = "backgroundExecutor", at = @At("HEAD"), cancellable = true)
    private static void interceptBackgroundExecutor(CallbackInfoReturnable<Object> callback) {
        if (graphene$shouldInterceptExecution(false)) {
            return;
        }
        try {
            callback.setReturnValue(RE.cpuService());
        } catch (Exception executorException) {
            System.err.println("无法获取CPU执行器服务:" + executorException.getMessage());
        }
    }
}