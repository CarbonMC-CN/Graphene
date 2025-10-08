package net.carbonmc.graphene.mixin.MemoryLeakFix.client;

import de.mari_023.ae2wtlib.AE2WTLibCreativeTab;
import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = AE2WTLibCreativeTab.class, remap = false)
public class AE2WTLibCreativeTabLeakFix {

    @Shadow
    @Final
    private static List<ItemStack> items;
    @Inject(method = "init", at = @At("HEAD"))
    private static void graphene$clearOnInit(CallbackInfo ci) {
        if(CoolConfig.MemoryLeakFix_AE2WTLibCreativeTabLeakFix.get()){
        synchronized (items) {
            items.clear();
        }
        }
    }
}