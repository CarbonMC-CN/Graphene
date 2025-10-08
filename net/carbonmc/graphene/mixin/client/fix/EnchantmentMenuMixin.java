package net.carbonmc.graphene.mixin.client.fix;

import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnchantmentMenu.class)
public class EnchantmentMenuMixin {
    @Redirect(
            method = "slotsChanged",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;isEnchantable()Z"
            )
    )
    public boolean slot(ItemStack instance) {
        boolean hasDurability = instance.getMaxDamage() > 0;
        return (instance.getMaxStackSize() == 1 && hasDurability) || instance.isEnchantable();
    }
}