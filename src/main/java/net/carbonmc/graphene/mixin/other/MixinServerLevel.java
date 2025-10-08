package net.carbonmc.graphene.mixin.other;

import net.carbonmc.graphene.config.CoolConfig;

import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerLevel.class)
public abstract class MixinServerLevel {
    @ModifyVariable(method = "tickChunk", at = @At("HEAD"), argsOnly = true)
    private int modifyChunkLoadRate(int chunks) {
        return Math.min(chunks, CoolConfig.chunkUnloadDelay.get());
    }
}