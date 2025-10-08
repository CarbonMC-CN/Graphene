package net.carbonmc.graphene.mixin.MemoryLeakFix.client;

import java.nio.ByteBuffer;

import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.platform.GlUtil;

import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Unique;

/*
 * Graphene's Memory Leak Fix(Not a mod with the same name)
 * -------------------------------------------
 * Captures the ByteBuffer returned by GlUtil.allocateMemory() during
 * grabHugeScreenshot() and guarantees its release when the screenshot
 * fails (disk full, GL error, etc.), preventing native memory leaks.
 *
 * This mixin is client-only and active on MC 1.17.0+ (1.17 … 1.21+).
 * It does not affect the success path performance—only adds a single
 * nullable field assignment and an conditional free on failure.
 *
 * @author CarbonMC
 * @reason Prevent off-heap leak on screenshot failure
 * @see GlUtil#allocateMemory
 * @see GlUtil#freeMemory
 * @since 1.17.0+
 */
@Mixin(value = Minecraft.class)
public class ScreenshotByteBufferLeakFixMixin {

    @Shadow @Final private static Logger LOGGER;
    @Unique
    private ByteBuffer graphene$buffer = null;

    @ModifyVariable(
            method = "grabHugeScreenshot",
            at = @At(value = "INVOKE_ASSIGN",
                    target = "Lcom/mojang/blaze3d/platform/GlUtil;allocateMemory(I)Ljava/nio/ByteBuffer;")
    )
    private ByteBuffer graphene$cacheBuffer(ByteBuffer buf) {
        graphene$buffer = buf;
        return buf;
    }
    //无论如何，立即释放
    @Inject(method = "grabHugeScreenshot", at = @At("RETURN"))
    private void graphene$freeOnReturn(CallbackInfoReturnable<Component> cir) {
        if (graphene$buffer != null) {
            if (CoolConfig.DEBUG_LOGGING.get()) {
                LOGGER.debug("[Graphene] 截图内存已立即释放 ByteBuffer@0x{}",
                        Integer.toHexString(System.identityHashCode(graphene$buffer)));
            }
            GlUtil.freeMemory(graphene$buffer);
            graphene$buffer = null;
        }
    }
}