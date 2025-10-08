package net.carbonmc.graphene.gl;

import com.mojang.blaze3d.systems.RenderSystem;

public class CleanerRunnable implements Runnable {

    private final FramebufferFixer fixer;

    CleanerRunnable(FramebufferFixer fixer) {
        this.fixer = fixer;
    }

    @Override
    public void run() {
        RenderSystem.assertOnRenderThreadOrInit();
        try {
            fixer.graphene$des();
            fixer.graphene$real();
        } catch (Exception e) {
            throw new FramebufferCleanupException("Failed to cleanup framebuffer", e);
        }
    }
}