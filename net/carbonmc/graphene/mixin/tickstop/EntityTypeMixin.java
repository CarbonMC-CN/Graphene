package net.carbonmc.graphene.mixin.tickstop;

import net.carbonmc.graphene.api.IOptimizableEntity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
@Mixin(EntityType.class)
public abstract class EntityTypeMixin implements IOptimizableEntity {
    @Unique private boolean alwaysTick;
    @Unique private boolean tickInRaid;

    @Override
    public boolean shouldAlwaysTick() {
        return this.alwaysTick;
    }

    @Override
    public void setAlwaysTick(boolean value) {
        this.alwaysTick = value;
    }

    @Override
    public boolean shouldTickInRaid() {
        return this.tickInRaid;
    }

    @Override
    public void setTickInRaid(boolean value) {
        this.tickInRaid = value;
    }
}