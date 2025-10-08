package net.carbonmc.graphene.client.resources;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class ResourceManager implements ResourceManagerReloadListener {
    private static final int MAX_CACHED_RESOURCES = 1000;
    private static final Cache<String, byte[]> resourceCache = Caffeine.newBuilder()
            .maximumSize(MAX_CACHED_RESOURCES)
            .build();

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new ResourceManager());
    }

    @Override
    public void onResourceManagerReload(net.minecraft.server.packs.resources.ResourceManager resourceManager) {
        resourceCache.invalidateAll();
    }

    @Nullable
    public static byte[] getCompressedResource(String path) {
        Objects.requireNonNull(path, "Resource path cannot be null");
        if (path.isEmpty()) {
            throw new IllegalArgumentException("Resource path cannot be empty");
        }

        return resourceCache.get(path, k -> {
            try (InputStream input = ResourceManager.class.getResourceAsStream(path)) {
                if (input == null) {
                    throw new IOException("Resource not found: " + path);
                }
                return input.readAllBytes();
            } catch (Exception e) {
                throw new RuntimeException("Failed to load resource: " + path, e);
            }
        });
    }
}