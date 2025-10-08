// FastEventWrapper.java
package net.carbonmc.graphene.optimization.event;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventListener;

import java.lang.reflect.Method;

public final class FastEventWrapper implements IEventListener {
    private final FastEventListener delegate;

    public FastEventWrapper(FastEventListener delegate) {
        this.delegate = delegate;
    }

    @Override
    public void invoke(Event event) {
        delegate.fastInvoke(event);
    }

    // 工厂方法
    public static FastEventWrapper create(Method method, Object instance) {
        return new FastEventWrapper((FastEventListener) FastEventFactory.createListener(method, instance));
    }
}