package net.carbonmc.graphene.gl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public final class CleanupAction implements Runnable {
    private final int fbo, color, depth;

    public CleanupAction(int fbo, int color, int depth) {
        this.fbo   = fbo;
        this.color = color;
        this.depth = depth;
    }

    @Override
    public void run() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (color > -1) TextureUtil.releaseTextureId(color);
            if (depth > -1) TextureUtil.releaseTextureId(depth);
            if (fbo   > -1) GlStateManager._glDeleteFramebuffers(fbo);
        });
    }
}