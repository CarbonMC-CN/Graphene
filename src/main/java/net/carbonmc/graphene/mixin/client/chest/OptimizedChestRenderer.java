package net.carbonmc.graphene.mixin.client.chest;

import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class OptimizedChestRenderer {

    private BlockPos lastPlayerPos;
    private long lastCheckTime;

    @Inject(method = "getRenderer", at = @At("HEAD"), cancellable = true)
    private <E extends BlockEntity> void handleRendering(
            E entity,
            CallbackInfoReturnable<BlockEntityRenderer<E>> cir) {

        if (!shouldOptimizeRendering(entity)) {
            return;
        }

        try {
            BlockPos pos = entity.getBlockPos();
            Level level = entity.getLevel();

            if (!isValidPosition(level, pos)) {
                logDebug("无效的箱子位置或世界: " + pos);
                return;
            }
            if (!shouldRenderChest(level, pos)) {
                cir.setReturnValue(null);
            }
        } catch (Exception e) {
            logDebug("优化箱子渲染时出错: " + e.getMessage());
        }
    }

    private <E extends BlockEntity> boolean shouldOptimizeRendering(E entity) {
        return CoolConfig.ENABLE_OPTIMIZATION.get() &&
                entity != null &&
                !entity.isRemoved() &&
                entity.getLevel() != null &&
                (entity instanceof ChestBlockEntity ||
                        (entity instanceof EnderChestBlockEntity && CoolConfig.OPTIMIZE_ENDER_CHESTS.get()));
    }

    private boolean isValidPosition(Level level, BlockPos pos) {
        return level != null && pos != null;
    }

    private boolean shouldRenderChest(Level level, BlockPos chestPos) {
        BlockPos playerPos = getCachedPlayerPosition(level, chestPos);
        if (playerPos == null) {
            return false;
        }
        return !isBeyondRenderDistance(chestPos, playerPos);
    }

    private BlockPos getCachedPlayerPosition(Level level, BlockPos chestPos) {
        long currentTime = System.currentTimeMillis();
        if (lastPlayerPos == null || currentTime - lastCheckTime > 500) {
            Player player = getNearestPlayerSafely(level, chestPos);
            lastPlayerPos = player != null ? player.blockPosition() : null;
            lastCheckTime = currentTime;
        }
        return lastPlayerPos;
    }

    private Player getNearestPlayerSafely(Level level, BlockPos pos) {
        try {
            return level.getNearestPlayer(
                    pos.getX(), pos.getY(), pos.getZ(),
                    CoolConfig.MAX_RENDER_DISTANCE.get() * 16,
                    false
            );
        } catch (Exception e) {
            logDebug("获取最近玩家失败: " + e.getMessage());
            return null;
        }
    }

    private boolean isBeyondRenderDistance(BlockPos chestPos, BlockPos playerPos) {
        double maxDistance = CoolConfig.MAX_RENDER_DISTANCE.get() * 16;
        double distanceSq = chestPos.distSqr(playerPos);
        boolean beyond = distanceSq > (maxDistance * maxDistance);

        if (beyond) {
            logDebug("跳过远处箱子: " + chestPos + " 距离: " + Math.sqrt(distanceSq));
        }
        return beyond;
    }
    private void logDebug(String message) {
        if (CoolConfig.DEBUG_LOGGING.get()) {
            System.out.println("[Graphene] " + message);
        }
    }
}