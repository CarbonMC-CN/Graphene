package net.carbonmc.graphene.mixin.client.lang;

import net.carbonmc.graphene.AsyncHandler;
import net.carbonmc.graphene.flang;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LanguageSelectScreen;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
@AsyncHandler
@OnlyIn(Dist.CLIENT)
@Mixin(Minecraft.class)
public abstract class mcmixinx {
    @Shadow @Final private LanguageManager languageManager;

    @Shadow public abstract ResourceManager getResourceManager();

    @Shadow @Nullable public Screen screen;

    @Inject(method = "reloadResourcePacks()Ljava/util/concurrent/CompletableFuture;",at = @At("HEAD"),cancellable = true)
    public void reloadRes(CallbackInfoReturnable<CompletableFuture<Void>> cir){
        if(flang.langReload){
            this.languageManager.onResourceManagerReload(this.getResourceManager());
            flang.langReload = false;
            cir.setReturnValue(null);
            cir.cancel();
        }
    }

    @Inject(method = "setOverlay",at = @At("HEAD"),cancellable = true)
    public void reloadRes(Overlay p_91151_, CallbackInfo ci){
        if(this.screen instanceof LanguageSelectScreen || this.screen instanceof CraftingScreen) ci.cancel();
    }
}
