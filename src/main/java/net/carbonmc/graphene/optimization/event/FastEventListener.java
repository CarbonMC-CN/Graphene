// FastEventListener.java
package net.carbonmc.graphene.optimization.event;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventListener;

@FunctionalInterface
public interface FastEventListener extends IEventListener {
    void fastInvoke(Event event);

    @Override
    default void invoke(Event event) {
        fastInvoke(event);
    }

    static FastEventListener empty() {
        return event -> {};
    }
}