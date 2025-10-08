package net.carbonmc.graphene.client;

import net.carbonmc.graphene.event.AsyncHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
@OnlyIn(Dist.CLIENT)
@AsyncHandler
public class ItemCountRenderer {
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onNameTagRender(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof ItemEntity itemEntity)) {
            return;
        }

        ItemStack stack = itemEntity.getItem();
        if (stack.getCount() <= 1) {
            return;
        }

        Component customName = itemEntity.getCustomName();
        if (customName != null) {
            event.setContent(customName);
        }
    }
}