package net.carbonmc.graphene.mixin.stack;

import net.carbonmc.graphene.AsyncHandler;
import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;
import java.util.List;

import static net.carbonmc.graphene.config.CoolConfig.OpenIO;

@AsyncHandler
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Shadow public abstract ItemStack getItem();
    @Shadow public abstract void setItem(ItemStack stack);
    @Shadow public abstract void setExtendedLifetime();

    @Unique
    private int lastMergeTick = -1;

    @Inject(method = "tick", at = @At("HEAD"))

    private void onTick(CallbackInfo ci) {
        if (!OpenIO.get()) {
            return;
        }
        ItemEntity self = (ItemEntity)(Object)this;
        if (self.level().isClientSide) return;
        updateStackDisplay(self);

        long gameTime = self.level().getGameTime();
        if (lastMergeTick == -1 || gameTime - lastMergeTick >= 5) {
            lastMergeTick = (int)gameTime;
            tryMergeItems(self);
        }
    }

    @Unique
    private void tryMergeItems(ItemEntity self) {
        if (!OpenIO.get()) {
            return;
        }
        double mergeDistance = CoolConfig.mergeDistance.get();
        int configMaxStack = CoolConfig.maxStackSize.get();
        int listMode = CoolConfig.listMode.get();
        List<? extends String> itemList = CoolConfig.itemList.get();

        if (!isMergeAllowed(self.getItem(), listMode, itemList)) return;

        ItemStack stack = self.getItem();
        int maxStack = configMaxStack > 0 ? configMaxStack : Integer.MAX_VALUE - 100;
        if (stack.getCount() >= maxStack) return;

        List<ItemEntity> nearby = self.level().getEntitiesOfClass(
                ItemEntity.class,
                self.getBoundingBox().inflate(mergeDistance),
                e -> isValidMergeTarget(self, e, listMode, itemList)
        );

        nearby.sort(Comparator.comparingDouble(self::distanceToSqr));
        int remainingSpace = maxStack - stack.getCount();

        for (ItemEntity other : nearby) {
            if (remainingSpace <= 0) break;

            ItemStack otherStack = other.getItem();
            int transfer = Math.min(otherStack.getCount(), remainingSpace);

            stack.grow(transfer);
            self.setItem(stack);
            self.setExtendedLifetime();

            if (otherStack.getCount() == transfer) {
                other.discard();
            } else {
                otherStack.shrink(transfer);
                other.setItem(otherStack);
                ((ItemEntityMixin)(Object)other).updateStackDisplay(other);
            }

            remainingSpace -= transfer;
        }
    }

    @Unique
    private void updateStackDisplay(ItemEntity entity) {
        if (!OpenIO.get()) {
            return;
        }
        if (!CoolConfig.showStackCount.get()) {
            entity.setCustomName(null);
            entity.setCustomNameVisible(false);
            return;
        }

        ItemStack stack = entity.getItem();
        if (stack.getCount() > 1) {
            Component countText = Component.literal("×" + stack.getCount())
                    .withStyle(ChatFormatting.DARK_GREEN)
                    .withStyle(ChatFormatting.BOLD);

            entity.setCustomName(countText);
            entity.setCustomNameVisible(true);
        } else {
            entity.setCustomName(null);
            entity.setCustomNameVisible(false);
        }
    }

    @Unique
    private boolean isValidMergeTarget(ItemEntity self, ItemEntity other, int listMode, List<? extends String> itemList) {
        int configMax = CoolConfig.maxStackSize.get() > 0 ? CoolConfig.maxStackSize.get() : Integer.MAX_VALUE;
        boolean lockMaxedStacks = CoolConfig.lockMaxedStacks.get();

        return self != other &&
                !other.isRemoved() &&
                isSameItem(self.getItem(), other.getItem()) &&
                isMergeAllowed(other.getItem(), listMode, itemList) &&
                (! lockMaxedStacks || other.getItem().getCount() < configMax);
    }

    @Unique
    private boolean isSameItem(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameTags(a, b);
    }

    @Unique
    private boolean isMergeAllowed(ItemStack stack, int listMode, List<? extends String> itemList) {

        if (listMode == 0) return true;

        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return false;

        boolean inList = itemList.contains(id.toString());
        return listMode == 1 ? inList : !inList;
    }
}