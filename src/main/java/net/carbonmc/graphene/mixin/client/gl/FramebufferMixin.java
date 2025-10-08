package net.carbonmc.graphene.mixin.client.gl;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.carbonmc.graphene.gl.CleanupAction;
import net.carbonmc.graphene.gl.FramebufferFixer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.lang.ref.Cleaner;

@Mixin(RenderTarget.class)
public abstract class FramebufferMixin implements FramebufferFixer {
    @Shadow protected int colorTextureId;
    @Shadow protected int depthBufferId;
    @Shadow public    int frameBufferId;

    private static final Cleaner CLEANER = Cleaner.create();
    private final Cleaner.Cleanable cleanable;

    public FramebufferMixin() {
        cleanable = CLEANER.register(this,
                new CleanupAction(frameBufferId, colorTextureId, depthBufferId));
    }

    @Override
    public void graphene$des() {
        RenderSystem.assertOnRenderThreadOrInit();
        GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, 0);
    }

    @Override
    public void graphene$real() {
        RenderSystem.assertOnRenderThreadOrInit();
        if (this.colorTextureId > -1) {
            TextureUtil.releaseTextureId(this.colorTextureId);
            this.colorTextureId = -1;
        }
        if (this.depthBufferId > -1) {
            TextureUtil.releaseTextureId(this.depthBufferId);
            this.depthBufferId = -1;
        }
        if (this.frameBufferId > -1) {
            GlStateManager._glDeleteFramebuffers(this.frameBufferId);
            this.frameBufferId = -1;
        }
    }
}