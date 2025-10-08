package net.carbonmc.graphene.mixin.math.abs;

import net.carbonmc.graphene.config.CoolConfig;
import net.carbonmc.graphene.optimization.math.abs.FastAbs;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Mth.class)
public class MthMixin {
    @Overwrite
    public static int abs(int a) {
        if (CoolConfig.Absoptimize.get()) {
            return FastAbs.abs(a);
        }
        return Math.abs(a);
    }

    @Overwrite
    public static float abs(float a) {
        if (CoolConfig.Absoptimize.get()) {
            return FastAbs.abs(a);
        }
        return Math.abs(a);
    }
}