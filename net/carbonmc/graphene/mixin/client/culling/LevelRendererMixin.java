package net.carbonmc.graphene.mixin.client.culling;

import net.carbonmc.graphene.engine.cull.CullingEngineManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    private final Set<BlockEntity> graphene$culledBlockEntities = new HashSet<>();

    @Inject(method = "setupRender", at = @At("HEAD"))
    private void graphene$onSetupRender(CallbackInfo ci) {
        CullingEngineManager.updateVisibleChunks(
                Minecraft.getInstance().options.getEffectiveRenderDistance()
        );
    }

    @Redirect(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Set;iterator()Ljava/util/Iterator;"
            )
    )
    private Iterator<BlockEntity> graphene$filterBlockEntities(Set<BlockEntity> original) {
        graphene$culledBlockEntities.clear();
        for (BlockEntity be : original) {
            if (CullingEngineManager.getInstance().shouldRenderBlockEntity(be)) {
                graphene$culledBlockEntities.add(be);
            }
        }
        return graphene$culledBlockEntities.iterator();
    }
}