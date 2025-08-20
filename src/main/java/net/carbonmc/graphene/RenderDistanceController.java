package net.carbonmc.graphene;

import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RenderDistanceController {
    public static int getActiveRenderDistance() {
        Options options = Minecraft.getInstance().options;
        return options.renderDistance().get();
    }

    public static int getInactiveRenderDistance() {
        if (CoolConfig.REDUCE_RENDER_DISTANCE_WHEN_INACTIVE.get()) {
            return CoolConfig.INACTIVE_RENDER_DISTANCE.get();
        }
        return getActiveRenderDistance();
    }
}