package net.carbonmc.graphene.optimization;

import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
@OnlyIn(Dist.CLIENT)
public class MemoryCleaner {
    private long lastCleanTime = 0;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer().overworld() == null) return;
        
        long currentTime = System.currentTimeMillis();
        long interval = CoolConfig.MEMORY_CLEAN_INTERVAL.get() * 1000;
        
        if (currentTime - lastCleanTime > interval) {
            cleanupResources();
            lastCleanTime = currentTime;
        }
    }

    private void cleanupResources() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (CoolConfig.OPTIMIZE_ENTITY_CLEANUP.get()) {
            for (ServerLevel level : mc.getSingleplayerServer().getAllLevels()) {
                level.getAllEntities().forEach(entity -> {
                    if (!entity.isAlive() && entity.tickCount > 600) {
                        entity.discard();
                    }
                });
            }
        }

        if (CoolConfig.MEMORY_CLEAN_INTERVAL.get() > lastCleanTime) {

            cleanupResources();
            System.out.println("[Graphene] 内存清理完成");
        }
    }}
