package net.carbonmc.graphene.compat.mixin.distanthorizons;

import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.forge.ForgeClientProxy;
import com.seibel.distanthorizons.forge.ForgePluginPacketSender;
import loaderCommon.forge.com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftClientWrapper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ForgeClientProxy.class,remap = false)
public class ForgeClientProxyMixin {
    @Mutable
    @Shadow @Final private static ForgePluginPacketSender PACKET_SENDER;

    @Mutable
    @Shadow @Final private static IMinecraftClientWrapper MC;

    @Inject(method = "<clinit>",at = @At("TAIL"))
    private static void clinit(CallbackInfo ci){
        if(PACKET_SENDER==null) PACKET_SENDER = new ForgePluginPacketSender();
        if(MC==null) MC = MinecraftClientWrapper.INSTANCE;
    }
}