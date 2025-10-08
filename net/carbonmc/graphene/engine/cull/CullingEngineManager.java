package net.carbonmc.graphene.engine.cull;

import net.minecraft.client.Minecraft;

public class CullingEngineManager {
    private static CullingEngine instance;

    public static CullingEngine getInstance() {
        if (instance == null) {
            instance = new CullingEngine(Minecraft.getInstance());
        }
        return instance;
    }

    public static void updateVisibleChunks(int renderDistance) {
        if (Minecraft.getInstance().level != null) {
            getInstance().updateVisibleChunks(Minecraft.getInstance().level, renderDistance);
        }
    }
}