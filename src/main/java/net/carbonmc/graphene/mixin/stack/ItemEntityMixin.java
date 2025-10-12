package net.carbonmc.graphene.mixin.stack;

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


@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    private static final int MERGE_COOLDOWN_TICKS = 5;
    private static final int DEFAULT_MAX_STACK = Integer.MAX_VALUE - 100;

    @Shadow public abstract ItemStack getItem();
    @Shadow public abstract void setItem(ItemStack stack);
    @Shadow public abstract void setExtendedLifetime();

    @Unique
    private int lastMergeTick = -1;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (!shouldProcess()) return;

        ItemEntity self = (ItemEntity)(Object)this;
        updateStackDisplay(self);

        if (shouldAttemptMerge(self)) {
            lastMergeTick = (int)self.level().getGameTime();
            tryMergeItems(self);
        }
    }

    @Unique
    private boolean shouldProcess() {
        return CoolConfig.OpenIO.get() && !((ItemEntity)(Object)this).level().isClientSide;
    }

    @Unique
    private boolean shouldAttemptMerge(ItemEntity self) {
        long gameTime = self.level().getGameTime();
        return lastMergeTick == -1 || gameTime - lastMergeTick >= MERGE_COOLDOWN_TICKS;
    }

    @Unique
    private void tryMergeItems(ItemEntity self) {
        if (!CoolConfig.OpenIO.get()) return;

        ItemStack stack = self.getItem();
        int maxStack = getEffectiveMaxStackSize();

        if (stack.getCount() >= maxStack) return;

        List<ItemEntity> nearby = findMergeableItems(self);
        if (nearby.isEmpty()) return;

        performMerge(self, stack, maxStack, nearby);
    }

    @Unique
    private int getEffectiveMaxStackSize() {
        int configMax = CoolConfig.maxStackSize.get();
        return configMax > 0 ? configMax : DEFAULT_MAX_STACK;
    }

    @Unique
    private List<ItemEntity> findMergeableItems(ItemEntity self) {
        double mergeDistance = CoolConfig.mergeDistance.get();
        int listMode = CoolConfig.listMode.get();
        List<? extends String> itemList = CoolConfig.itemList.get();
        ItemStack selfStack = self.getItem();

        List<ItemEntity> nearby = self.level().getEntitiesOfClass(
                ItemEntity.class,
                self.getBoundingBox().inflate(mergeDistance),
                e -> isValidMergeTarget(self, e, listMode, itemList)
        );

        nearby.sort(Comparator.comparingDouble(self::distanceToSqr));
        return nearby;
    }

    @Unique
    private void performMerge(ItemEntity self, ItemStack stack, int maxStack, List<ItemEntity> nearby) {
        int remainingSpace = maxStack - stack.getCount();

        for (ItemEntity other : nearby) {
            if (remainingSpace <= 0) break;

            ItemStack otherStack = other.getItem();
            int transfer = Math.min(otherStack.getCount(), remainingSpace);

            stack.grow(transfer);
            self.setItem(stack);
            self.setExtendedLifetime();

            handleOtherStackAfterTransfer(other, otherStack, transfer);
            remainingSpace -= transfer;
        }
    }

    @Unique
    private void handleOtherStackAfterTransfer(ItemEntity other, ItemStack otherStack, int transfer) {
        if (otherStack.getCount() == transfer) {
            other.discard();
        } else {
            otherStack.shrink(transfer);
            other.setItem(otherStack);
            updateStackDisplay(other);
        }
    }

    @Unique
    private void updateStackDisplay(ItemEntity entity) {
        if (!CoolConfig.OpenIO.get() || !CoolConfig.showStackCount.get()) {
            clearDisplay(entity);
            return;
        }

        ItemStack stack = entity.getItem();
        if (stack.getCount() > 1) {
            setStackCountDisplay(entity, stack.getCount());
        } else {
            clearDisplay(entity);
        }
    }

    @Unique
    private void setStackCountDisplay(ItemEntity entity, int count) {
        Component countText = Component.literal("×" + count)
                .withStyle(ChatFormatting.DARK_GREEN)
                .withStyle(ChatFormatting.BOLD);
        entity.setCustomName(countText);
        entity.setCustomNameVisible(true);
    }

    @Unique
    private void clearDisplay(ItemEntity entity) {
        entity.setCustomName(null);
        entity.setCustomNameVisible(false);
    }

    @Unique
    private boolean isValidMergeTarget(ItemEntity self, ItemEntity other, int listMode, List<? extends String> itemList) {
        if (self == other || other.isRemoved()) return false;

        ItemStack selfStack = self.getItem();
        ItemStack otherStack = other.getItem();

        return isSameItem(selfStack, otherStack) &&
                isMergeAllowed(otherStack, listMode, itemList) &&
                (!CoolConfig.lockMaxedStacks.get() || otherStack.getCount() < getEffectiveMaxStackSize());
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