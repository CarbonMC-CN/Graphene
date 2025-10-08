package net.carbonmc.graphene.mixin.client.renderer.particle;

import net.carbonmc.graphene.config.CoolConfig;
import net.carbonmc.graphene.util.VersionChecker;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Particle.class)
public abstract class MixinParticle {
    @Shadow protected double xd;
    @Shadow protected double yd;
    @Shadow protected double zd;
    @Shadow protected float alpha;

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/particle/Particle;move(DDD)V"
            )
    )
    private void redirectMove(Particle instance, double x, double y, double z) {
//由于1.19.4版本中此功能会导致mc崩溃，这里做了检测，1.19.4/forge版本为45就不启用功能
        if (VersionChecker.shouldDisableOptimizations()) {
            instance.move(x, y, z);
            return;
        }

        if (CoolConfig.ENABLE_PARTICLE_OPTIMIZATION.get() &&
                CoolConfig.ENABLE_FIXED_TIMESTEP.get()) {
            float timestep = CoolConfig.FIXED_TIMESTEP_INTERVAL.get().floatValue();
            instance.move(xd * timestep, yd * timestep, zd * timestep);
        } else {
            instance.move(x, y, z);
        }
    }
}