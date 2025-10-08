package net.carbonmc.graphene.mixin.client.culling;

import com.mojang.blaze3d.vertex.PoseStack;
import net.carbonmc.graphene.engine.cull.RenderOptimizer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(T entity, float yaw, float tickDelta,
                          PoseStack poseStack, MultiBufferSource buffer,
                          int light, CallbackInfo ci) {
        if (RenderOptimizer.shouldSkipEntity(entity)) {
            ci.cancel();
        }
    }
}