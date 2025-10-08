package net.carbonmc.graphene.optimization.oldthing;

import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public class EntityOptimizer {
    private static final Map<Entity, Long> inactiveEntities = new WeakHashMap<>();
    
    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (!CoolConfig.disableEntityCollisions.get()) return;
        
        Entity entity = event.getEntity();
        entity.setNoGravity(true);
        entity.noPhysics = false;
        inactiveEntities.put(entity, System.currentTimeMillis());
    }
    
    @SubscribeEvent
    public void onEntityLeave(EntityLeaveLevelEvent event) {
        inactiveEntities.remove(event.getEntity());
    }
    
    public static void processInactiveEntities() {
        if (!CoolConfig.disableEntityCollisions.get()) return;
        
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Entity, Long>> iterator = inactiveEntities.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<Entity, Long> entry = iterator.next();
            Entity entity = entry.getKey();
            if (!entity.isAlive()) {
                iterator.remove();
                continue;
            }
            if (now - entry.getValue() > 10000) {
                entity.setDeltaMovement(Vec3.ZERO);
                entity.setPos(entity.getX(), entity.getY(), entity.getZ());
            }
        }
    }
}