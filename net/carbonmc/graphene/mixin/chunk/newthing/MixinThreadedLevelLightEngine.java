package net.carbonmc.graphene.mixin.chunk.newthing;

import net.carbonmc.graphene.optimization.chunk.LightingContext;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.carbonmc.graphene.config.CoolConfig.CTU;

@Mixin(ThreadedLevelLightEngine.class)
public class MixinThreadedLevelLightEngine {

    @Inject(method = "runUpdate", at = @At("HEAD"), require = 1)
    private void onEnter(CallbackInfo ci) {
        if (!CTU.get()) {
            return;
        }
        LightingContext.enter();
    }

    @Inject(method = "runUpdate", at = @At("RETURN"), require = 1)
    private void onExit(CallbackInfo ci) {
        if (!CTU.get()) {
            return;
        }
        LightingContext.exit();
    }
}