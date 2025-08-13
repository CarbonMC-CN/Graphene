package net.carbonmc.graphene.util;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;

public class VersionChecker {
    private static final String MC_VERSION = FMLLoader.versionInfo().mcVersion();
    private static final String FORGE_VERSION = ModList.get().getModContainerById("forge")
            .orElseThrow().getModInfo().getVersion().toString();

    public static boolean shouldDisableOptimizations() {
        return MC_VERSION.equals("1.19.4") ||
                FORGE_VERSION.startsWith("45.");
    }
}