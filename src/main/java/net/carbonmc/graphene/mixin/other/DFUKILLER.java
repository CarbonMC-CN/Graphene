package net.carbonmc.graphene.mixin.other;

import com.mojang.datafixers.DataFixerBuilder;
import net.carbonmc.graphene.lazy.LazyDataFixerBuilder;
import net.minecraft.util.datafix.DataFixers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DataFixers.class)
public class DFUKILLER {
    @Redirect(method = "createFixerUpper", at = @At(value = "NEW", target = "com/mojang/datafixers/DataFixerBuilder"))
private static DataFixerBuilder create$replaceBuilder(int dataVersion) {
        return new LazyDataFixerBuilder(dataVersion);

    }
}