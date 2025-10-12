package net.carbonmc.graphene.gl;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface FramebufferFixer {
    void graphene$cleanup();
    default void close() {
        graphene$cleanup();
    }
}