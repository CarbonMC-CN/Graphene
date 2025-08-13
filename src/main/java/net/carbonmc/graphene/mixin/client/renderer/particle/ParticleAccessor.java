package net.carbonmc.graphene.mixin.client.renderer.particle;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessor {
    @Accessor("alpha")
    float getAlphaAccessor();

    @Accessor("alpha")
    void setAlphaAccessor(float alpha);
}