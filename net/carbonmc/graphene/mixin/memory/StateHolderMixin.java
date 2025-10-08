package net.carbonmc.graphene.mixin.memory;

import net.carbonmc.graphene.util.FastMapDuck;
import net.carbonmc.graphene.util.StateIndexer;
import net.minecraft.world.level.block.state.StateHolder;
import org.spongepowered.asm.mixin.*;

@Mixin(StateHolder.class)
public abstract class StateHolderMixin implements FastMapDuck {

    @Unique
    private StateIndexer<?> fastmap$indexer;
    @Unique
    private int fastmap$index = -1;

    public StateIndexer<?> fastmap$getIndexer() { return fastmap$indexer; }
    public int           fastmap$getIndex()   { return fastmap$index; }
    public void          fastmap$setIndexer(StateIndexer<?> idx) { this.fastmap$indexer = idx; }
    public void          fastmap$setIndex(int ix) { this.fastmap$index = ix; }
}