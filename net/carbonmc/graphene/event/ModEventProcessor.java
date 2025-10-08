package net.carbonmc.graphene.event;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.carbonmc.graphene.Graphene;
import net.carbonmc.graphene.config.CoolConfig;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ModEventProcessor {
    private static final Type SUBSCRIBE_EVENT = Type.getType(SubscribeEvent.class);
    private static final String CLIENT_ONLY_WARNING = "Skipping client-side class loading on server: ";

    private static final Cache<String, Boolean> CLASS_BLACKLIST_CACHE = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    private static final Cache<String, Boolean> MOD_BLACKLIST_CACHE = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    private static final Cache<String, Boolean> PROCESSED_CLASSES = Caffeine.newBuilder()
            .maximumSize(5000)
            .expireAfterWrite(6, TimeUnit.HOURS)
            .build();

    public static void initialize() {
        try {
            loadBlacklists();
        } catch (Exception e) {
            if (CoolConfig.DEBUG_LOGGING.get()) {
            logError("Failed to initialize ModEventProcessor", e);}
        }
    }

    public static void processModEvents() {
        if (!FMLEnvironment.dist.isClient()) {
            if (CoolConfig.DEBUG_LOGGING.get()) {
                AsyncEventSystem.LOGGER.info("Server environment detected - skipping client event processing");
            }return;
        }

        try {
            List<ModFileScanData> allScanData = ModList.get().getAllScanData();
            Set<String> eventMethods = new HashSet<>();

            for (ModFileScanData scanData : allScanData) {
                processScanData(scanData, eventMethods);
            }
            if (CoolConfig.DEBUG_LOGGING.get()) {
                AsyncEventSystem.LOGGER.info("Found {} event methods in mods", eventMethods.size());
            } processEventMethods(eventMethods);
        } catch (Exception e) {
            logError("Critical error in processModEvents", e);
        }
    }

    private static void processScanData(ModFileScanData scanData, Set<String> eventMethods) {
        String modId = getModIdFromScanData(scanData);
        if (modId == null || MOD_BLACKLIST_CACHE.getIfPresent(modId) != null) {
            if (CoolConfig.DEBUG_LOGGING.get()) {
                AsyncEventSystem.LOGGER.debug("Skipping blacklisted or invalid mod: {}", modId);
            } return;
        }

        for (ModFileScanData.AnnotationData ad : scanData.getAnnotations()) {
            if (SUBSCRIBE_EVENT.equals(ad.annotationType())) {
                processAnnotationData(ad, eventMethods);
            }
        }
    }

    private static String getModIdFromScanData(ModFileScanData scanData) {
        try {
            if (!scanData.getIModInfoData().isEmpty()) {
                return scanData.getIModInfoData().get(0).getMods().get(0).getModId();
            }

            if (scanData.getTargets() != null && !scanData.getTargets().isEmpty()) {
                return scanData.getTargets().keySet().iterator().next();
            }
        } catch (Exception e) {
            if (CoolConfig.DEBUG_LOGGING.get()) {logError("Failed to get modId from scan data", e);}
        }
        return null;
    }

    private static void processAnnotationData(ModFileScanData.AnnotationData ad, Set<String> eventMethods) {
        try {
            String className = ad.clazz().getClassName();

            if (PROCESSED_CLASSES.getIfPresent(className) != null) {
                return;
            }

            if (isBlacklisted(className)) {
                if (CoolConfig.DEBUG_LOGGING.get()) { AsyncEventSystem.LOGGER.debug("Skipping blacklisted class: {}", className);}
                PROCESSED_CLASSES.put(className, true);
                return;
            }

            if (isClientOnlyClass(className)) {
                AsyncEventSystem.LOGGER.debug(CLIENT_ONLY_WARNING + className);
                PROCESSED_CLASSES.put(className, true);
                return;
            }

            if (CoolConfig.isStrictClassCheckingEnabled()) {
                try {
                    Class.forName(className, false, ModEventProcessor.class.getClassLoader());
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    if (CoolConfig.DEBUG_LOGGING.get()) { AsyncEventSystem.LOGGER.debug("Class loading failed: {}", className);}
                    PROCESSED_CLASSES.put(className, true);
                    return;
                }
            }

            eventMethods.add(className + "#" + ad.memberName());
            PROCESSED_CLASSES.put(className, true);
        } catch (Exception e) {
            logError("Failed to process annotation data", e);
        }
    }

    private static void processEventMethods(Set<String> eventMethods) {
        for (String methodInfo : eventMethods) {
            try {
                String[] parts = methodInfo.split("#");
                if (parts.length != 2) continue;

                String className = parts[0];
                String methodName = parts[1];

                if (isClientOnlyClass(className)) {
                    AsyncEventSystem.LOGGER.warn(CLIENT_ONLY_WARNING + className);
                    continue;
                }

                Class<?> clazz = Class.forName(className);
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.getName().equals(methodName)) {
                        processMethod(method);
                    }
                }
            } catch (NoClassDefFoundError e) {
                handleClassLoadingError(e);
            } catch (Exception e) {
                logError("Failed to process event method " + methodInfo, e);
            }
        }
    }

    private static void processMethod(Method method) {
        Class<?>[] params = method.getParameterTypes();
        if (params.length == 1 && Event.class.isAssignableFrom(params[0])) {
            @SuppressWarnings("unchecked")
            Class<? extends Event> eventType = (Class<? extends Event>) params[0];

            if (!isBlacklisted(eventType.getName())) {
                AsyncEventSystem.registerAsyncEvent(eventType);
                AsyncEventSystem.LOGGER.debug("Registered async event: {}", eventType.getName());
            }
        }
    }

    public static void loadBlacklists() {
        try {
            CLASS_BLACKLIST_CACHE.invalidateAll();
            MOD_BLACKLIST_CACHE.invalidateAll();

            CoolConfig.getAsyncEventClassBlacklist().forEach(cls ->
                    CLASS_BLACKLIST_CACHE.put(cls, true));
            CoolConfig.getAsyncEventModBlacklist().forEach(mod ->
                    MOD_BLACKLIST_CACHE.put(mod, true));

            AsyncEventSystem.LOGGER.info("Loaded blacklists: {} classes, {} mods",
                    CLASS_BLACKLIST_CACHE.estimatedSize(), MOD_BLACKLIST_CACHE.estimatedSize());
        } catch (Exception e) {
            logError("Failed to load blacklists", e);
        }
    }

    private static boolean isBlacklisted(String className) {
        if (CLASS_BLACKLIST_CACHE.getIfPresent(className) != null) {
            return true;
        }

        // 处理通配符模式
        for (String pattern : CoolConfig.getAsyncEventClassBlacklist()) {
            if (pattern.endsWith(".*") && className.startsWith(pattern.substring(0, pattern.length() - 1))) {
                CLASS_BLACKLIST_CACHE.put(className, true);
                return true;
            }
        }

        return false;
    }

    private static boolean isClientOnlyClass(String className) {
        return className.startsWith("net.minecraft.client.") ||
                className.contains(".client.") ||
                className.endsWith("ClientEvents");
    }

    private static void handleClassLoadingError(NoClassDefFoundError e) {
        if (e.getMessage() != null && e.getMessage().contains("client/renderer")) {
            AsyncEventSystem.LOGGER.warn(CLIENT_ONLY_WARNING + e.getMessage());
        } else {
            logError("Class loading failed", e);
        }
    }

    private static void logError(String message, Throwable e) {
        AsyncEventSystem.LOGGER.error(message, e);
        try {
            FileUtils.writeStringToFile(Graphene.Graphene_EVENTS_LOG,
                    "\n" + message + ": " + e + "\n", true);
            for (StackTraceElement ste : e.getStackTrace()) {
                FileUtils.writeStringToFile(Graphene.Graphene_EVENTS_LOG, ste.toString() + "\n", true);
            }
        } catch (IOException ioEx) {
            AsyncEventSystem.LOGGER.error("Failed to write to error log", ioEx);
        }
    }
}