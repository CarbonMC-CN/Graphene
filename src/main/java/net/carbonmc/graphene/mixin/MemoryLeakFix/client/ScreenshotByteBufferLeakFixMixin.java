package net.carbonmc.graphene.mixin.MemoryLeakFix.client;

import java.nio.ByteBuffer;

import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.platform.GlUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Unique;

@Mixin(value = Minecraft.class)
public class ScreenshotByteBufferLeakFixMixin {

    @Shadow @Final private static Logger LOGGER;
    @Unique
    private ByteBuffer graphene$buffer = null;

    @WrapOperation(
            method = "grabHugeScreenshot",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/GlUtil;allocateMemory(I)Ljava/nio/ByteBuffer;"
            )
    )
    private ByteBuffer graphene$wrapAllocateMemory(int size, Operation<ByteBuffer> original) {
        if (!CoolConfig.MemoryLeakFix_ScreenshotByteBufferLeakFix.get()) {
            return original.call(size);
        }

        ByteBuffer buffer = original.call(size);
        graphene$buffer = buffer;
        return buffer;
    }
    @Inject(method = "grabHugeScreenshot", at = @At("RETURN"),remap = true)
    private void graphene$freeOnReturn(CallbackInfoReturnable<Component> cir) {
        if (CoolConfig.MemoryLeakFix_ScreenshotByteBufferLeakFix.get()) {
            if (graphene$buffer != null) {
                GlUtil.freeMemory(graphene$buffer);
                graphene$buffer = null;
            }
        }
    }
}