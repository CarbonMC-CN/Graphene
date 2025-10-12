package net.carbonmc.graphene.mixin.client.renderer.skip;

import net.carbonmc.graphene.client.GrapheneClient;
import net.carbonmc.graphene.util.Fpsu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(GameConfig gameConfig, CallbackInfo ci) {
        Minecraft client = (Minecraft) (Object) this;
        GrapheneClient.displayController = new Fpsu(
                client.getWindow().getWindow()
        );
    }
}