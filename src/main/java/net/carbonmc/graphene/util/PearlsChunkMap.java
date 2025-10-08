package net.carbonmc.graphene.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber
public final class PearlsChunkMap extends SavedData {

    private static final String DATA_NAME = "graphene_pearls";
    private static final Logger LOGGER = LogManager.getLogger();
    private final SetMultimap<UUID, ChunkPos> map = HashMultimap.create();
    public static PearlsChunkMap INSTANCE;
    public static PearlsChunkMap get(ServerLevel level) {
        return level.getDataStorage()
                .computeIfAbsent(PearlsChunkMap::load, PearlsChunkMap::new, DATA_NAME);
    }

    public void add(UUID owner, ChunkPos pos, ServerLevel level) {
        if (!CoolConfig.FIX_PEARL_LEAK.get()) return;
        get(level).map.put(owner, pos);
        level.setChunkForced(pos.x, pos.z, true);
        get(level).setDirty();
        if (CoolConfig.DEBUG_LOGGING.get()) {
            LOGGER.debug("Pearl forced chunk {} in {}", pos, level.dimension().location());
        }
    }

    public void remove(UUID owner, ServerLevel level) {
        PearlsChunkMap data = get(level);
        Set<ChunkPos> set = data.map.removeAll(owner);
        if (set != null) {
            set.forEach(p -> level.setChunkForced(p.x, p.z, false));
            data.setDirty();
            LOGGER.debug("Unforced {} chunks for {}", set.size(), owner);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone evt) {
        if (evt.isWasDeath() && evt.getEntity().level() instanceof ServerLevel sl) {
            UUID uuid = evt.getOriginal().getUUID();
            get(sl).remove(uuid, sl);
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent evt) {
        if (evt.phase == TickEvent.Phase.END && evt.level instanceof ServerLevel sl) {
            if (sl.getServer().getTickCount() % 6000 == 0) {
                get(sl).setDirty();
            }
        }
    }

    @Nonnull
    @Override
    public CompoundTag save(@Nonnull CompoundTag tag) {
        ListTag list = new ListTag();
        map.asMap().forEach((uuid, poses) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Owner", uuid);
            ListTag chunks = new ListTag();
            poses.forEach(p -> chunks.add(StringTag.valueOf(p.toString())));
            entry.put("Chunks", chunks);
            list.add(entry);
        });
        tag.put("Entries", list);
        return tag;
    }

    public static PearlsChunkMap load(CompoundTag tag) {
        PearlsChunkMap data = new PearlsChunkMap();
        ListTag list = tag.getList("Entries", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            UUID owner = entry.getUUID("Owner");
            ListTag chunks = entry.getList("Chunks", 8);
            for (int j = 0; j < chunks.size(); j++) {
                String s = chunks.getString(j);
                ChunkPos pos = new ChunkPos(Long.parseLong(s));
                data.map.put(owner, pos);
            }
        }
        return data;
    }

    public static void onServerStarted(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            PearlsChunkMap data = get(level);
            data.map.asMap().forEach((uuid, poses) ->
                    poses.forEach(p -> level.setChunkForced(p.x, p.z, true))
            );
            LOGGER.info("Graphene: restored {} forced chunks in {}",
                    data.map.size(), level.dimension().location());
        }
    }
}