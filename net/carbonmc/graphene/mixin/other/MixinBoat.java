package net.carbonmc.graphene.mixin.other;

import net.carbonmc.graphene.event.AsyncHandler;
import net.minecraft.world.entity.vehicle.Boat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@AsyncHandler
@Mixin(Boat.class)
public abstract class MixinBoat {

    @Inject(
        method = "checkFallDamage",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onCheckFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.BlockPos pos, CallbackInfo ci) {
        Boat boat = (Boat)(Object)this;
        boat.fallDistance = 0.0F;
        ci.cancel();
    }
}