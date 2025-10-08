package net.carbonmc.graphene.api;

public interface IOptimizableEntity {
    boolean shouldAlwaysTick();
    void setAlwaysTick(boolean value);
    boolean shouldTickInRaid();
    void setTickInRaid(boolean value);
}