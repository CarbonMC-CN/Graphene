package net.carbonmc.graphene.mixin.client.renderer.jump;

import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.stream.Collectors;

@Mixin(BlockModel.class)
public abstract class BlockFaceCullingMixin {

    @Inject(method = "getElements", at = @At("RETURN"), cancellable = true)
    private void graphene$filterBackFace(CallbackInfoReturnable<List<BlockElement>> cir) {
        List<BlockElement> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) return;

        cir.setReturnValue(original.stream()
                .filter(element ->
                        element.faces.keySet().stream()
                                .anyMatch(face -> face.getAxisDirection() != Direction.AxisDirection.NEGATIVE)
                )
                .collect(Collectors.toList()));
    }
}