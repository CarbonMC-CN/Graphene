package net.carbonmc.graphene.mixin.client.renderer.skip;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow @Final private Minecraft minecraft;
    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;bindWrite(Z)V",
                    ordinal = 0,
                    shift = At.Shift.BEFORE
            )
    )
    private void graphene$fastBlit(float f, long l, boolean bl, CallbackInfo ci) {
        if (CoolConfig.fpsoo.get() == Boolean.FALSE) {
            return;
        }
        RenderTarget main = this.minecraft.getMainRenderTarget();

        int srcFbo = main.frameBufferId;
        int dstFbo = 0;
        int w = main.viewWidth;
        int h = main.viewHeight;
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, srcFbo);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, dstFbo);

        GL30.glBlitFramebuffer(
                0, 0, w, h,
                0, 0, w, h,
                GL30.GL_COLOR_BUFFER_BIT,
                GL30.GL_NEAREST
        );

        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }
}