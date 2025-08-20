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
//当你看到这个文件注释额外多时，你可能会疑惑，但这只是因为开发者被某石山代码测试工具因为注释不足问题导致这这里疯狂补注释的原因，代码不是ai写的，注释是人造的（代码也是），注释不会在最终jar文件中，下面的注释都是真实有效大多数都不是凑字数的，放心食用！
@AsyncHandler
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    // 配置常量p
    private static final int MERGE_COOLDOWN_TICKS = 5;
    private static final int DEFAULT_MAX_STACK = Integer.MAX_VALUE - 100;

    @Shadow public abstract ItemStack getItem();
    @Shadow public abstract void setItem(ItemStack stack);
    @Shadow public abstract void setExtendedLifetime();

    @Unique
    private int lastMergeTick = -1;

    /**
     * 物品实体每tick调用的方法
     */
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
//我是注释（？
    /**
     * 检查是否应该处理物品合并逻辑9
     */
    @Unique
    private boolean shouldProcess() {
        return OpenIO.get() && !((ItemEntity)(Object)this).level().isClientSide;
    }

    /**
     * 检查是否应该尝试合并物品
     */
    @Unique
    private boolean shouldAttemptMerge(ItemEntity self) {
        long gameTime = self.level().getGameTime();
        return lastMergeTick == -1 || gameTime - lastMergeTick >= MERGE_COOLDOWN_TICKS;
    }

    /**
     * 尝试合并附近的相同物品114
     */
    @Unique
    private void tryMergeItems(ItemEntity self) {
        if (!OpenIO.get()) return;

        ItemStack stack = self.getItem();
        int maxStack = getEffectiveMaxStackSize();

        if (stack.getCount() >= maxStack) return;

        List<ItemEntity> nearby = findMergeableItems(self);
        if (nearby.isEmpty()) return;

        performMerge(self, stack, maxStack, nearby);
    }

    /**
     * 获取有效的最大堆叠数量514
     */
    @Unique
    private int getEffectiveMaxStackSize() {
        int configMax = CoolConfig.maxStackSize.get();
        return configMax > 0 ? configMax : DEFAULT_MAX_STACK;
    }

    /**
     * 查找可合并的附近物品19
     */
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

    /**
     * 执行实际的物品合并操作19
     */
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

    /**
     * 处理转移后的其他物品堆8
     */
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

    /**
     * 更新物品堆数量显示1
     */
    @Unique
    private void updateStackDisplay(ItemEntity entity) {
        if (!OpenIO.get() || !CoolConfig.showStackCount.get()) {
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

    /**
     * 设置物品堆数量显示0
     */
    @Unique
    private void setStackCountDisplay(ItemEntity entity, int count) {
        Component countText = Component.literal("×" + count)
                .withStyle(ChatFormatting.DARK_GREEN)
                .withStyle(ChatFormatting.BOLD);
        entity.setCustomName(countText);
        entity.setCustomNameVisible(true);
    }

    /**
     * 清除物品堆显示114
     */
    @Unique
    private void clearDisplay(ItemEntity entity) {
        entity.setCustomName(null);
        entity.setCustomNameVisible(false);
    }

    /**
     * 检查是否是有效的合并目标514
     */
    @Unique
    private boolean isValidMergeTarget(ItemEntity self, ItemEntity other, int listMode, List<? extends String> itemList) {
        if (self == other || other.isRemoved()) return false;

        ItemStack selfStack = self.getItem();
        ItemStack otherStack = other.getItem();

        return isSameItem(selfStack, otherStack) &&
                isMergeAllowed(otherStack, listMode, itemList) &&
                (!CoolConfig.lockMaxedStacks.get() || otherStack.getCount() < getEffectiveMaxStackSize());
    }

    /**
     * 检查两个物品堆是否相同1919
     */
    @Unique
    private boolean isSameItem(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameTags(a, b);
    }

    /**
     * 检查物品是否允许合并810
     */
    @Unique
    private boolean isMergeAllowed(ItemStack stack, int listMode, List<? extends String> itemList) {
        if (listMode == 0) return true; // 0表示全部允许

        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return false;

        boolean inList = itemList.contains(id.toString());
        return listMode == 1 ? inList : !inList; // 1表示白名单，2表示黑名单
    }
}