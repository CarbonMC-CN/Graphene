package net.carbonmc.graphene.mixin.other;

import net.carbonmc.graphene.optimization.event.FastEventWrapper;
import net.minecraftforge.eventbus.ASMEventHandler;
import net.minecraftforge.eventbus.IEventListenerFactory;
import net.minecraftforge.eventbus.api.IEventListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Method;

@Mixin(value = ASMEventHandler.class, remap = false)
public class MixinASMEventHandler {

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/eventbus/IEventListenerFactory;create(Ljava/lang/reflect/Method;Ljava/lang/Object;)Lnet/minecraftforge/eventbus/api/IEventListener;"
            )
    )
    private IEventListener replaceWithFastListener(IEventListenerFactory instance, Method method, Object target) {
        return FastEventWrapper.create(method, target);
    }
}