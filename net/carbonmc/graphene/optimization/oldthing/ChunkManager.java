package net.carbonmc.graphene.optimization.oldthing;

import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;

public class ChunkManager {
    private static final Map<ChunkPos, Long> chunkAccessTimes = new HashMap<>();
    
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !CoolConfig.aggressiveChunkUnloading.get()) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        long unloadThreshold = CoolConfig.chunkUnloadDelay.get() * 1000L;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            level.getPlayers(player -> {
                ChunkPos pos = new ChunkPos(player.chunkPosition().x, player.chunkPosition().z);
                chunkAccessTimes.put(pos, currentTime);
                return false;
            });
        }

        chunkAccessTimes.entrySet().removeIf(entry -> {
            if (currentTime - entry.getValue() > unloadThreshold) {
                for (ServerLevel level : event.getServer().getAllLevels()) {
                    if (level.getChunkSource().hasChunk(entry.getKey().x, entry.getKey().z)) {
                        level.getChunkSource().tick(() -> true, true);
                    }
                }
                return true;
            }
            return false;
        });
    }
}