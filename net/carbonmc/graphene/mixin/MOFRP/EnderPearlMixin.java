package net.carbonmc.graphene.mixin.MOFRP;

import net.carbonmc.graphene.config.CoolConfig;
import net.carbonmc.graphene.util.PearlsChunkMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ThrownEnderpearl.class)
public class EnderPearlMixin {
    //1.20.2以上不移植
    @Inject(method = "tick", at = @At("HEAD"))
    private void graphene$onTick(CallbackInfo ci) {
        if (!CoolConfig.FIX_PEARL_LEAK.get()) return;
        ThrownEnderpearl pearl = (ThrownEnderpearl) (Object) this;
        if (!pearl.level().isClientSide && pearl.level() instanceof ServerLevel sl) {
            BlockPos pos = pearl.blockPosition();
            if (pearl.level().getBlockState(pos).is(Blocks.END_STONE)) {
                PearlsChunkMap.INSTANCE.add(pearl.getUUID(), new ChunkPos(pos), sl);
            }
        }
    }

    @Inject(method = "onHit", at = @At("RETURN"))
    private void graphene$onHit(HitResult result, CallbackInfo ci) {
        if (!CoolConfig.FIX_PEARL_LEAK.get()) return;

        ThrownEnderpearl pearl = (ThrownEnderpearl) (Object) this;

        if (pearl.level().isClientSide()) return;

        ServerLevel level = (ServerLevel) pearl.level();
        UUID owner = pearl.getOwner() != null ? pearl.getOwner().getUUID() : null;
        if (owner != null) {
            PearlsChunkMap.get(level).remove(owner, level);
        }
    }
}