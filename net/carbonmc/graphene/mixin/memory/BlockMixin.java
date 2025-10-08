package net.carbonmc.graphene.mixin.memory;

import net.carbonmc.graphene.util.BlockStateIndexerBuilder;
import net.carbonmc.graphene.util.FastMapDuck;
import net.carbonmc.graphene.util.StateIndexer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mixin(Block.class)
public class BlockMixin {

    @Inject(method = "<init>", require = 1, at = @At("RETURN"))
    private void onConstructed(CallbackInfo ci) {
        Block self = (Block) (Object) this;
        List<BlockState> states = self.getStateDefinition().getPossibleStates();
        StateIndexer<BlockState> indexer = BlockStateIndexerBuilder.build(states);
        // 把索引器和索引写进每个 BlockState
        for (BlockState state : states) {
            FastMapDuck duck = (FastMapDuck) state;
            duck.fastmap$setIndexer(indexer);
            duck.fastmap$setIndex(indexer.indexOf(
                    state.getValues().entrySet().stream()
                            .collect(Collectors.toMap(e -> e.getKey().getName(),
                                    Map.Entry::getValue))));
        }
    }
}