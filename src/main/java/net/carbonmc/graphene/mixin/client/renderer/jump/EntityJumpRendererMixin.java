// culling.client.mixin.net.carbonmc.graphene.EntityRendererMixin.java
//1.6.0-更新，修复实体闪烁b🪩ug
package net.carbonmc.graphene.mixin.client.renderer.jump;

import com.mojang.blaze3d.vertex.PoseStack;
import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityJumpRendererMixin {
    @Inject(
            method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRender(Entity entity, double x, double y, double z, float yaw,
                          float partialTicks, PoseStack poseStack,
                          MultiBufferSource bufferSource, int packedLight,
                          CallbackInfo ci) {

        if (CoolConfig.skipOutlineWhenNoGlowing.get() &&
                bufferSource instanceof OutlineBufferSource &&
                !entity.isCurrentlyGlowing()) {
            ci.cancel();
        }
    }
}