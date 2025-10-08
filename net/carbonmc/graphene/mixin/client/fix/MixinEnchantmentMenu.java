package net.carbonmc.graphene.mixin.client.fix;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;


@Mixin(EnchantmentMenu.class)
public class MixinEnchantmentMenu {

    @Shadow @Final private Container enchantSlots;
    @ModifyVariable(
            method = "clickMenuButton",
            at = @At(
                    value = "STORE",
                    ordinal = 0
            ),
            ordinal = 0
    )
    private ItemStack consumeOneBook(ItemStack book) {
        if (!book.isEmpty()) {
            book = book.copy();
            book.shrink(1);
            if (book.isEmpty()) {
                book = ItemStack.EMPTY;
            }
            this.enchantSlots.setItem(1, book);
        }
        return book;
    }
}