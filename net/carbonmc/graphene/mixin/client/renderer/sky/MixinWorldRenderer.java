package net.carbonmc.graphene.mixin.client.renderer.sky;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(LevelRenderer.class)
public class MixinWorldRenderer {
    private final BlockPos.MutableBlockPos lastPos = new BlockPos.MutableBlockPos();
    private final AtomicBoolean wasUnderwater = new AtomicBoolean(false);
    private static final int CHECK_INTERVAL = 5;
    private int tickCounter = 0;

    @Inject(
            method = "renderSky",
            at = @At("HEAD"),
            cancellable = true
    )
    private void graphene$cancelSkyWhenUnderwater(
            PoseStack poseStack,
            Matrix4f projectionMatrix,
            float partialTick,
            Camera camera,
            boolean bl,
            Runnable runnable,
            CallbackInfo ci
    ) {
        if (++tickCounter % CHECK_INTERVAL == 0 || !lastPos.equals(camera.getBlockPosition())) {
            BlockPos current = camera.getBlockPosition();
            lastPos.set(current);
            FluidState fluid = camera.getEntity().level().getFluidState(current);
            wasUnderwater.set(fluid.is(FluidTags.WATER));
        }

        if (wasUnderwater.get()) {
            ci.cancel();
        }
    }
}