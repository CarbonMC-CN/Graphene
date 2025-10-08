package net.carbonmc.graphene.mixin.math.random;

import net.carbonmc.graphene.optimization.math.random.Random;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.RandomSupport;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.carbonmc.graphene.config.CoolConfig.RandomOptimizeENABLED;

@Mixin(value = RandomSource.class,remap = false, priority = 1000)
public class MixinRandomA {

    @Unique
    private static final ThreadLocal<Random> graphene$TLS = ThreadLocal.withInitial(() -> new Random(0L));

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(graphene$TLS::remove));
    }

    @Inject(method = "createThreadSafe", at = @At("HEAD"), cancellable = true)
    private static void z(CallbackInfoReturnable<RandomSource> ci) {
        if (RandomOptimizeENABLED.get()) {
            graphene$TLS.set(new Random(RandomSupport.generateUniqueSeed()));
            ci.setReturnValue(graphene$TLS.get());
        }
    }

    @Inject(method = "create*", at = @At("HEAD"), cancellable = true)
    private static void y(long s, CallbackInfoReturnable<RandomSource> ci) {
        if (RandomOptimizeENABLED.get()) ci.setReturnValue(new Random(s));
    }

    @Inject(method = "createNewThreadLocalInstance", at = @At("HEAD"), cancellable = true)
    private static void x(CallbackInfoReturnable<RandomSource> ci) {
        if (RandomOptimizeENABLED.get()) ci.setReturnValue(new Random(RandomSupport.generateUniqueSeed()));
    }
}