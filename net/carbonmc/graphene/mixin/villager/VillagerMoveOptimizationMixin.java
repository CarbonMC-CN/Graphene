package net.carbonmc.graphene.mixin.villager;

import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerMoveOptimizationMixin {

    @Unique
    private boolean graphene$isFullyStopped = false;

    @Inject(method = "customServerAiStep", at = @At("HEAD"), cancellable = true)
    private void onCustomServerAiStep(CallbackInfo ci) {
        if (!CoolConfig.VILLAGER_MOVE_OPTIMIZE.get()) {
            return;
        }

        Villager villager = (Villager) (Object) this;
        if (!(villager.level() instanceof ServerLevel)) {
            return;
        }

        if (shouldCompletelyStopAI(villager)) {
            if (!graphene$isFullyStopped) {
                fullyStopVillagerAI(villager);
                graphene$isFullyStopped = true;
            }
            ci.cancel();
        } else if (graphene$isFullyStopped) {
            restoreVillagerAI(villager);
            graphene$isFullyStopped = false;
        }
    }

    @Unique
    private boolean shouldCompletelyStopAI(Villager villager) {
        return !isPlayerNearby(villager) && !isTrading(villager);
    }

    @Unique
    private void fullyStopVillagerAI(Villager villager) {
        Brain<Villager> brain = villager.getBrain();
        brain.stopAll((ServerLevel) villager.level(), villager);
        brain.setActiveActivityIfPossible(Activity.IDLE);
        villager.getNavigation().stop();
        villager.setYHeadRot(villager.getYRot());
    }

    @Unique
    private void restoreVillagerAI(Villager villager) {
        villager.getBrain().updateActivityFromSchedule(
                villager.level().getDayTime(),
                villager.level().getGameTime()
        );
    }

    @Unique
    private boolean isPlayerNearby(Villager villager) {
        return villager.level().players().stream()
                .anyMatch(player -> villager.distanceToSqr(player) < 256); // 16 格
    }

    @Unique
    private boolean isTrading(Villager villager) {
        return villager.getTradingPlayer() != null;
    }
}