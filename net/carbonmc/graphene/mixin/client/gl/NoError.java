package net.carbonmc.graphene.mixin.client.gl;

import com.mojang.blaze3d.platform.Window;
import net.carbonmc.graphene.config.CoolConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Window.class)
public class NoError {

    @Inject(
            method = "defaultErrorCallback",
            at = @At("HEAD"),
            cancellable = true
    )
    private void graphene$noerror(int error, long description, CallbackInfo ci) {
        if (CoolConfig.NoOpenGLError.get()) {
            ci.cancel();
        }
    }
}