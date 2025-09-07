//白渊同学唯一能写的？
package net.carbonmc.graphene.mixin.other;
import net.minecraftforge.eventbus.api.IEventListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.lang.reflect.Method;

@Mixin(targets = "net.minecraftforge.eventbus.api.EventListenerHelper")
public abstract class EventListenerTransformerMixin {
    /**
     * @author baiyuan
     */
    @Overwrite(remap = false)
    public static IEventListener createListener(Object target, Method method) {
        return event -> {
            try {
                method.invoke(target, event);
            } catch (Exception e) {
                throw new RuntimeException("Error invoking event handler", e);
            }
        };
    }
}