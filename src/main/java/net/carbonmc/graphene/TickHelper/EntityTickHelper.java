package net.carbonmc.graphene.TickHelper;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.ImmutableSet;
import net.carbonmc.graphene.api.IOptimizableEntity;
import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber
public final class EntityTickHelper {
    private static final AtomicReference<Set<EntityType<?>>> ENTITY_WHITELIST = new AtomicReference<>(Collections.emptySet());
    private static final AtomicReference<Set<Item>> ITEM_WHITELIST = new AtomicReference<>(Collections.emptySet());
    private static final Cache<EntityType<?>, Boolean> BOSS_MOB_CACHE = Caffeine.newBuilder()
            .maximumSize(100)
            .executor(Runnable::run)
            .build();

    private static final Set<EntityType<?>> BOSS_TYPES = ImmutableSet.of(
            EntityType.ENDER_DRAGON,
            EntityType.WITHER,
            EntityType.ELDER_GUARDIAN,
            EntityType.WARDEN
    );

    private static final ExecutorService TICK_EXECUTOR = Executors.newWorkStealingPool();

    private static volatile boolean optimizeEntitiesEnabled = true;
    private static volatile boolean tickRaidersInRaid = true;
    private static volatile int horizontalRange = 32;
    private static volatile int verticalRange = 16;

    static {
        refreshWhitelists();
        updateConfigCache();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            updateConfigCache();
        }
    }

    private static void updateConfigCache() {
        optimizeEntitiesEnabled = CoolConfig.optimizeEntities.get();
        tickRaidersInRaid = CoolConfig.tickRaidersInRaid.get();
        horizontalRange = CoolConfig.horizontalRange.get();
        verticalRange = CoolConfig.verticalRange.get();
    }

    public static void refreshWhitelists() {
        CompletableFuture.runAsync(() -> {
            ENTITY_WHITELIST.set(ImmutableSet.copyOf(
                    CoolConfig.entityWhitelist.get().stream()
                            .map(s -> ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(s)))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet())
            ));

            ITEM_WHITELIST.set(ImmutableSet.copyOf(
                    CoolConfig.itemWhitelist.get().stream()
                            .map(s -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(s)))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet())
            ));
        }, TICK_EXECUTOR);
    }

    public static boolean shouldCancelTick(@NotNull Entity entity) {
        if (!optimizeEntitiesEnabled) {
            return false;
        }
        Level level = entity.level();
        if (level == null) {
            return false;
        }
        if (entity instanceof Player ||
                ENTITY_WHITELIST.get().contains(entity.getType()) ||
                entity instanceof Raider) {
            return false;
        }

        if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
            return false;
        }

        if (entity instanceof IOptimizableEntity opt && opt.shouldAlwaysTick()) {
            return false;
        }


        return performFullChecks(living, level);
    }

    private static boolean performFullChecks(LivingEntity entity, Level level) {
        BlockPos pos = entity.blockPosition();


        EntityType<?> type = entity.getType();
        if (isBossMob(type, entity)) {
            return false;
        }


        if (tickRaidersInRaid && isInRaid(entity, level, pos)) {
            return false;
        }

        return !checkNearbyPlayers(level, pos);
    }

    private static boolean isBossMob(EntityType<?> type, LivingEntity entity) {
        return BOSS_TYPES.contains(type) ||
                BOSS_MOB_CACHE.get(type, t -> entity instanceof Mob mob && mob.isNoAi());
    }

    private static boolean isInRaid(Entity entity, Level level, BlockPos pos) {
        return level instanceof ServerLevel serverLevel &&
                serverLevel.isRaided(pos) &&
                (entity instanceof Raider ||
                        (entity instanceof IOptimizableEntity opt && opt.shouldTickInRaid()));
    }


    private static boolean checkNearbyPlayers(Level level, BlockPos pos) {
        horizontalRange = Math.min(horizontalRange, 128);
        verticalRange   = Math.min(verticalRange, 64);
        if (level.players().isEmpty()) {
            return false;
        }

        AABB area = new AABB(
                pos.getX() - horizontalRange,
                pos.getY() - verticalRange,
                pos.getZ() - horizontalRange,
                pos.getX() + horizontalRange,
                pos.getY() + verticalRange,
                pos.getZ() + horizontalRange
        );


        List<Player> players = level.getEntitiesOfClass(Player.class, area);
        for (Player player : players) {
            if (player.isAlive()) {
                return true;
            }
        }
        return false;
    }

    public static void shutdown() {
        TICK_EXECUTOR.shutdownNow();
        try {
            if (!TICK_EXECUTOR.awaitTermination(1, TimeUnit.SECONDS)) {
                TICK_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            TICK_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}