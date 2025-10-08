package net.carbonmc.graphene.compat.mixin.distanthorizons;

import com.seibel.distanthorizons.forge.ForgePluginPacketSender;
import com.seibel.distanthorizons.forge.ForgeServerProxy;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ForgeServerProxy.class,remap = false)
public class ForgeServerProxyMixin {
    @Mutable
    @Shadow @Final private static ForgePluginPacketSender PACKET_SENDER;

    @Inject(method = "<clinit>",at = @At("TAIL"))
    private static void clinit(CallbackInfo ci){
        if(PACKET_SENDER==null) PACKET_SENDER = new ForgePluginPacketSender();
    }
}