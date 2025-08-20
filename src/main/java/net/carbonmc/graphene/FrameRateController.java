package net.carbonmc.graphene;

import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FrameRateController {
    public static int getActiveFrameRateLimit() {
        Options options = Minecraft.getInstance().options;

        return options.framerateLimit().get();
    }

    public static int getInactiveFrameRateLimit() {
        if (CoolConfig.REDUCE_FPS_WHEN_INACTIVE.get()) {
            return CoolConfig.INACTIVE_FPS_LIMIT.get();
        }
        return getActiveFrameRateLimit();
    }
}