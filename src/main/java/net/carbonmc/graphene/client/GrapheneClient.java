package net.carbonmc.graphene.client;

import net.carbonmc.graphene.client.gui.ClothConfigScreenFactory;
import net.carbonmc.graphene.engine.cull.AsyncTracer;
import net.carbonmc.graphene.util.Fpsu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;

@OnlyIn(Dist.CLIENT)
public class GrapheneClient {
    public static Fpsu displayController;
    public static void init() {
        MinecraftForge.EVENT_BUS.register(ItemCountRenderer.class);
            ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory(
                            (mc, parent) -> ClothConfigScreenFactory.create(parent)
                    )
            );
    }
    public static void stop(){
        AsyncTracer.shutdown();
    }
}