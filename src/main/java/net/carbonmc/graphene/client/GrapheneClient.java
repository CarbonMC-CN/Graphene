// GrapheneClient.java - 修复后的完整文件
package net.carbonmc.graphene.client;

import net.carbonmc.graphene.client.gui.ClothConfigScreenFactory;
import net.carbonmc.graphene.client.gui.GUIEN;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;

@OnlyIn(Dist.CLIENT)
public class GrapheneClient {
    public static void init() {
        MinecraftForge.EVENT_BUS.register(ItemCountRenderer.class);
        String language = Minecraft.getInstance().options.languageCode;
        if ("zh_cn".equals(language)) {
            ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory(
                            (mc, parent) -> ClothConfigScreenFactory.create(parent)
                    )
            );
        }
        else {
            ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory(
                            (mc, parent) -> GUIEN.create(parent)
                    )
            );
        }
    }
}