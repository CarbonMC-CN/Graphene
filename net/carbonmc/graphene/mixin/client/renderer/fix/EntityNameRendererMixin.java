package net.carbonmc.graphene.mixin.client.renderer.fix;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityNameRendererMixin<T extends Entity> {

    @Inject(
            method = "shouldShowName",
            at = @At("HEAD"),
            cancellable = true,
            remap = true
    )
    private void onShouldShowName(T entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Player) {
            cir.setReturnValue(true);
            return;
        }
        if (entity.shouldShowName()) {
            cir.setReturnValue(true);
        }
    }
}