package net.carbonmc.graphene.TickHelper;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

@Mod.EventBusSubscriber
public final class EntityTickHelper {
    private static volatile boolean enabled                 = true;
    private static volatile boolean tickRaidersInRaid       = true;
    private static volatile int horizontalRange             = 32;
    private static volatile int verticalRange               = 16;
    private static volatile boolean ignoreDeadEntities      = true; // 新增配置项

    private static final AtomicReference<Set<EntityType<?>>> WHITE_LIST = new AtomicReference<>(Collections.emptySet());
    private static final AtomicReference<Set<EntityType<?>>> BLACK_LIST = new AtomicReference<>(Collections.emptySet());
    private static final List<WildcardPattern> WHITE_PATTERNS = new ArrayList<>();
    private static final List<WildcardPattern> BLACK_PATTERNS = new ArrayList<>();

    static {
        reloadConfig();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.START) reloadConfig();
    }

    public static boolean shouldSkipTick(Entity entity) {
        if (!enabled) return false;
        if (!(entity instanceof LivingEntity living)) return false;

        // 死亡实体拥有绝对豁免权
        if (ignoreDeadEntities && !living.isAlive()) {
            return false;
        }

        if (!living.isAlive()) return true;

        EntityType<?> type = entity.getType();
        if (matchesWildcard(type, BLACK_PATTERNS) || BLACK_LIST.get().contains(type)) {
            return true;
        }
        if (matchesWildcard(type, WHITE_PATTERNS) || WHITE_LIST.get().contains(type)) {
            return false;
        }
        if (tickRaidersInRaid && isRaiderInRaid(living)) return false;
        return !isNearPlayer(living);
    }

    private static void reloadConfig() {
        enabled               = CoolConfig.optimizeEntities.get();
        tickRaidersInRaid     = CoolConfig.tickRaidersInRaid.get();
        horizontalRange       = CoolConfig.horizontalRange.get();
        verticalRange         = CoolConfig.verticalRange.get();
        ignoreDeadEntities    = CoolConfig.ignoreDeadEntities.get(); // 从配置加载
        List<? extends String> whiteRaw = CoolConfig.entityWhitelist.get();

        Set<EntityType<?>> whiteIds = Sets.newHashSet();
        WHITE_PATTERNS.clear();
        BLACK_PATTERNS.clear();

        whiteRaw.forEach(s -> parseEntry(s, whiteIds));
        WHITE_LIST.set(ImmutableSet.copyOf(whiteIds));
    }

    private static void parseEntry(String raw, Set<EntityType<?>> idTarget) {
        if (raw.contains("*") || raw.contains("?")) {
            EntityTickHelper.WHITE_PATTERNS.add(new WildcardPattern(raw));
        } else {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(raw));
            if (type != null) idTarget.add(type);
        }
    }

    private static boolean matchesWildcard(EntityType<?> type, List<WildcardPattern> list) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (id == null) return false;
        String str = id.toString();
        for (WildcardPattern p : list) if (p.matches(str)) return true;
        return false;
    }

    private static boolean isRaiderInRaid(LivingEntity e) {
        return e instanceof net.minecraft.world.entity.raid.Raider raider && raider.isAlive() && raider.hasActiveRaid();
    }

    private static boolean isNearPlayer(LivingEntity entity) {
        Level level = entity.level();
        if (!(level instanceof ServerLevel sl)) return true;
        BlockPos pos = entity.blockPosition();

        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        int radius = (horizontalRange >> 4) + 1;

        AABB box = new AABB(
                pos.getX() - horizontalRange,
                pos.getY() - verticalRange,
                pos.getZ() - horizontalRange,
                pos.getX() + horizontalRange,
                pos.getY() + verticalRange,
                pos.getZ() + horizontalRange
        );

        for (Player player : sl.players()) {
            if (!player.isAlive()) continue;
            BlockPos ppos = player.blockPosition();
            int pcx = ppos.getX() >> 4;
            int pcz = ppos.getZ() >> 4;
            if (Math.abs(pcx - cx) > radius || Math.abs(pcz - cz) > radius) continue;
            if (player.getBoundingBox().intersects(box)) return true;
        }
        return false;
    }

    private static final class WildcardPattern {
        private final Pattern regex;
        WildcardPattern(String raw) {
            String s = raw.replace("?", ".{1}").replace("*", ".*");
            this.regex = Pattern.compile("^" + s + "$");
        }
        boolean matches(String str) {
            return regex.matcher(str).matches();
        }
    }
}