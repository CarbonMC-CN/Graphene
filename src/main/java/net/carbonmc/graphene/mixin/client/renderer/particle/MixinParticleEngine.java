package net.carbonmc.graphene.mixin.client.renderer.particle;

import net.carbonmc.graphene.config.CoolConfig;
import net.carbonmc.graphene.util.VersionChecker;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Random;

@Mixin(ParticleEngine.class)
public abstract class MixinParticleEngine {
    @Unique
    private static final Random RANDOM = new Random();

    @Inject(method = "tickParticle", at = @At("HEAD"), cancellable = true)
    private void onTickParticle(Particle particle, CallbackInfo ci) {
        //由于1.19.4版本中此功能会导致mc崩溃，这里做了检测，1.19.4/forge版本为45就不启用功能
        if (VersionChecker.shouldDisableOptimizations() ||
                !CoolConfig.ENABLE_PARTICLE_OPTIMIZATION.get()) {
            return;
        }
        if (!CoolConfig.ENABLE_PARTICLE_OPTIMIZATION.get() ||
                !CoolConfig.ENABLE_PARTICLE_LOD.get()) {
            return;
        }

        if (shouldApplyLOD(particle)) {
            handleParticleLOD(particle);
        }
    }

    @Unique
    private void handleParticleLOD(Particle particle) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        double distanceSq = particle.getPos().distanceToSqr(camera.getPosition());
        double threshold = CoolConfig.LOD_DISTANCE_THRESHOLD.get();

        if (distanceSq > threshold * threshold) {
            if (RANDOM.nextDouble() > CoolConfig.LOD_REDUCTION_FACTOR.get()) {
                // 通过Accessor修改alpha值
                ((ParticleAccessor)particle).setAlphaAccessor(0.0F);
            } else {
                ((ParticleAccessor)particle).setAlphaAccessor(1.0F);
            }
        } else {
            ((ParticleAccessor)particle).setAlphaAccessor(1.0F);
        }
    }

    @Unique
    private boolean shouldApplyLOD(Particle particle) {
        String name = particle.getClass().getName().toLowerCase();
        List<? extends String> whitelist = CoolConfig.LOD_PARTICLE_WHITELIST.get();
        List<? extends String> blacklist = CoolConfig.LOD_PARTICLE_BLACKLIST.get();

        for (String black : blacklist) {
            if (name.contains(black.toLowerCase())) {
                return false;
            }
        }

        for (String white : whitelist) {
            if (name.contains(white.toLowerCase())) {
                return true;
            }
        }

        return isLowPriorityParticle(particle);
    }

    @Unique
    private boolean isLowPriorityParticle(Particle particle) {
        String name = particle.getClass().getName().toLowerCase();
        return name.contains("rain") || name.contains("snow") ||
                name.contains("cloud") || name.contains("ash") ||
                name.contains("drip") || name.contains("spore");
    }
}