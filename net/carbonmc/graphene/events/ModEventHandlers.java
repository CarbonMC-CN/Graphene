package net.carbonmc.graphene.events;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.carbonmc.graphene.event.AsyncHandler;
import net.carbonmc.graphene.particles.AsyncParticleHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.concurrent.TimeUnit;

@AsyncHandler
public class ModEventHandlers {
    private static final Cache<String, Boolean> INIT_CACHE = Caffeine.newBuilder()
            .maximumSize(1)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    public static void register(IEventBus modEventBus, IEventBus forgeEventBus) {
        if (INIT_CACHE.getIfPresent("registered") != null) {
            return;
        }

        modEventBus.addListener(ModEventHandlers::onCommonSetup);
        forgeEventBus.addListener(AsyncParticleHandler::onServerTick);
        INIT_CACHE.put("registered", true);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        if (INIT_CACHE.getIfPresent("initialized") != null) {
            return;
        }

        AsyncParticleHandler.init();
        INIT_CACHE.put("initialized", true);
    }
}