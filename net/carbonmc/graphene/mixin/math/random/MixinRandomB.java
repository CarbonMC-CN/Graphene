package net.carbonmc.graphene.mixin.math.random;

import net.carbonmc.graphene.optimization.math.random.Random;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.carbonmc.graphene.config.CoolConfig.RandomOptimizeENABLED;

@Mixin(WorldgenRandom.class)
abstract class MixinRandomB {

    @Unique
    @Final @Mutable
    private RandomSource graphene$a;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void $init(RandomSource ignored, CallbackInfo ci) {
        if (RandomOptimizeENABLED.get()) this.graphene$a = new Random(0L);
    }
}