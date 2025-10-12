package net.carbonmc.graphene.mixin.stack;

import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
@Mixin(Inventory.class)
public abstract class InventoryMixin {

    @ModifyVariable(
            method = "add(Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"),
            argsOnly = true
    )
    private ItemStack onAddStack(ItemStack stack) {
        int configMax = CoolConfig.maxStackSize.get();
        if (configMax > 0 && stack.getCount() > configMax) {
            stack.setCount(configMax);
        }
        return stack;
    }
}