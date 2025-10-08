package net.carbonmc.graphene.gl;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface FramebufferFixer {
    void graphene$real();
    void graphene$des();
    default void close() {
        graphene$des();
        graphene$real();
    }
}