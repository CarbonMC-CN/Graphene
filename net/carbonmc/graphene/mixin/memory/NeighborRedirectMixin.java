package net.carbonmc.graphene.mixin.memory;

import com.google.common.collect.Table;
import net.carbonmc.graphene.util.FastMapDuck;
import net.carbonmc.graphene.util.StateIndexer;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = StateHolder.class, priority = 800)
public abstract class NeighborRedirectMixin {

    @Redirect(method = {"setValue", "trySetValue"},
            at = @At(value = "INVOKE",
                    target = "Lcom/google/common/collect/Table;get(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                    remap = false),
            require = 1)
    private Object useFastMap(Table<?, ?, ?> table,
                              Object rowKey,
                              Object colKey) {
        FastMapDuck thiz = (FastMapDuck) this;
        StateIndexer<?> indexer = thiz.fastmap$getIndexer();
        int idx = thiz.fastmap$getIndex();
        int n = indexer.neighbor(idx,
                ((Property<?>) rowKey).getName(),
                colKey);
        return n < 0 ? null : indexer.value(n);
    }
}