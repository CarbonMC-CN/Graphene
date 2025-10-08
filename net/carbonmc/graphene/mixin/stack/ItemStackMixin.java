package net.carbonmc.graphene.mixin.stack;

import net.carbonmc.graphene.config.CoolConfig;
import net.carbonmc.graphene.event.AsyncHandler;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.carbonmc.graphene.config.CoolConfig.OpenIO;

@AsyncHandler
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Shadow public abstract Item getItem();
    @Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
    private void onGetMaxStackSize(CallbackInfoReturnable<Integer> cir) {
        if (!OpenIO.get()) {
            return;
        }
        int configMax = CoolConfig.maxStackSize.get();
        if (configMax > 0) {
            int vanillaMax = this.getItem().getMaxStackSize();
            cir.setReturnValue(Math.min(configMax, vanillaMax));
        }
    }
    @Inject(method = "isStackable", at = @At("HEAD"), cancellable = true)
    private void onIsStackable(CallbackInfoReturnable<Boolean> cir) {
        if (!OpenIO.get()) {
            return;
        }
        int configMax = CoolConfig.maxStackSize.get();
        if (configMax == 0) {
            cir.setReturnValue(this.getItem().getMaxStackSize() > 1);
        }
    }
}