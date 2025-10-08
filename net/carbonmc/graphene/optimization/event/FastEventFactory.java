// FastEventFactory.java
package net.carbonmc.graphene.optimization.event;

import net.minecraftforge.eventbus.api.IEventListener;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class FastEventFactory {
    private static final ConcurrentMap<MethodKey, IEventListener> CACHE = new ConcurrentHashMap<>();
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private FastEventFactory() {}

    public static IEventListener createListener(Method method, Object instance) {
        MethodKey key = new MethodKey(method, instance);
        return CACHE.computeIfAbsent(key, k -> createLambdaListener(method, instance));
    }

    private static IEventListener createLambdaListener(Method method, Object instance) {
        try {
            boolean isStatic = Modifier.isStatic(method.getModifiers());
            MethodHandle listenerFactory = createListenerFactory(LOOKUP, method, isStatic, instance);

            return (IEventListener) (isStatic ?
                    listenerFactory.invokeExact() :
                    listenerFactory.invokeExact(instance));

        } catch (Throwable t) {
            System.err.println("Failed to create lambda event listener for method: " + method.getName());
            t.printStackTrace();
            return event -> {}; // 返回空监听器作为降级
        }
    }

    private static MethodHandle createListenerFactory(MethodHandles.Lookup lookup, Method callback,
                                                      boolean isStatic, Object instance) throws Exception {
        MethodHandle handle = lookup.unreflect(callback);

        // 创建工厂方法类型
        MethodType factoryType = isStatic ?
                MethodType.methodType(IEventListener.class) :
                MethodType.methodType(IEventListener.class, instance.getClass());

        // 创建目标方法类型 (Event event) -> void
        MethodType targetMethodType = MethodType.methodType(void.class, handle.type().parameterType(isStatic ? 0 : 1));

        MethodHandle factoryHandle = LambdaMetafactory.metafactory(
                lookup,
                "invoke", // IEventListener接口的方法名
                factoryType,
                MethodType.methodType(void.class, Object.class), //  erased方法签名
                handle,
                targetMethodType
        ).getTarget();

        return isStatic ? factoryHandle :
                factoryHandle.asType(factoryType.changeParameterType(0, Object.class));
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public static int getCacheSize() {
        return CACHE.size();
    }

    private static record MethodKey(Method method, Object instance) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodKey methodKey = (MethodKey) o;
            return method.equals(methodKey.method) &&
                    (instance == methodKey.instance ||
                            (instance != null && instance.equals(methodKey.instance)));
        }

        @Override
        public int hashCode() {
            return 31 * method.hashCode() + (instance != null ? instance.hashCode() : 0);
        }
    }
}