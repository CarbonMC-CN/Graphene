package net.carbonmc.graphene.optimization.oldthing;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;

public class EntityActivator {
    private static final Map<Entity, Boolean> activeEntities = new HashMap<>();
    
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (event.getServer().getTickCount() % 200 == 0) {
            activeEntities.keySet().removeIf(e -> !e.isAlive());
        }
    }
    
    public static boolean isEntityActive(Entity entity) {
        if (!activeEntities.containsKey(entity)) {
            updateEntityActivity(entity);
        }
        return activeEntities.get(entity);
    }
    
    private static void updateEntityActivity(Entity entity) {
        boolean active = false;

        for (Player player : entity.level().players()) {
            if (player.distanceToSqr(entity) < 1024) {
                active = true;
                break;
            }
        }
        if (!active) {
            active = entity.level().getNearestPlayer(entity, 64) != null;
        }
        
        activeEntities.put(entity, active);
    }
}