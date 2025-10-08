package net.carbonmc.graphene.util;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
public class ThreadManager {
    private static final ExecutorService executor = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors(),
        new ThreadFactory() {
            private final ThreadFactory factory = Executors.defaultThreadFactory();
            
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = factory.newThread(r);
                thread.setDaemon(true);
                thread.setPriority(Thread.MIN_PRIORITY);
                return thread;
            }
        }
    );
    
    private static final ExecutorService gameLogicExecutor = Executors.newFixedThreadPool(
        Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
        new ThreadFactory() {
            private final ThreadFactory factory = Executors.defaultThreadFactory();
            
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = factory.newThread(r);
                thread.setDaemon(true);
                thread.setPriority(Thread.NORM_PRIORITY);
                thread.setName("GameLogic-" + thread.getId());
                return thread;
            }
        }
    );
    
    private static final ExecutorService renderExecutor = Executors.newFixedThreadPool(
        Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
        new ThreadFactory() {
            private final ThreadFactory factory = Executors.defaultThreadFactory();
            
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = factory.newThread(r);
                thread.setDaemon(true);
                thread.setPriority(Thread.NORM_PRIORITY);
                thread.setName("Render-" + thread.getId());
                return thread;
            }
        }
    );
    
    private static final ExecutorService networkExecutor = Executors.newFixedThreadPool(
        4,
        new ThreadFactory() {
            private final ThreadFactory factory = Executors.defaultThreadFactory();
            
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = factory.newThread(r);
                thread.setDaemon(true);
                thread.setPriority(Thread.NORM_PRIORITY);
                thread.setName("Network-" + thread.getId());
                return thread;
            }
        }
    );
    
    private static final ExecutorService computeExecutor = Executors.newWorkStealingPool();
    private static volatile boolean enabled = true;
    private static final long MEMORY_CHECK_INTERVAL = 30000;
    private static final long DISK_CACHE_CHECK_INTERVAL = 60000;
    private static volatile long lastMemoryCheckTime = 0;
    private static volatile long lastDiskCacheCheckTime = 0;
    private static volatile long memoryThreshold = Runtime.getRuntime().maxMemory() * 70 / 100;
    private static volatile boolean memoryOverloaded = false;
    public static boolean isMemoryOverloaded() {
        return memoryOverloaded;
    }
    public static void submitTask(Runnable task) {
        if (enabled) {
            executor.submit(() -> {
                try {
                    task.run();
                } catch (Exception e) {
                    handleThreadException(e);
                }
            });
        } else {
            task.run();
        }
    }
    public static void submitGameLogicTask(Runnable task) {
        if (enabled) {
            gameLogicExecutor.submit(() -> {
                try {
                    task.run();
                } catch (Exception e) {
                    handleThreadException(e);
                }
            });
        } else {
            task.run();
        }
    }
    public static void submitRenderTask(Runnable task) {
        if (enabled) {
            renderExecutor.submit(() -> {
                try {
                    task.run();
                } catch (Exception e) {
                    handleThreadException(e);
                }
            });
        } else {
            task.run();
        }
    }
    public static void submitNetworkTask(Runnable task) {
        if (enabled) {
            networkExecutor.submit(() -> {
                try {
                    task.run();
                } catch (Exception e) {
                    handleThreadException(e);
                }
            });
        } else {
            task.run();
        }
    }

    public static void submitComputeTask(Runnable task) {
        if (enabled) {
            computeExecutor.submit(() -> {
                try {
                    task.run();
                } catch (Exception e) {
                    handleThreadException(e);
                }
            });
        } else {
            task.run();
        }
    }
    
    @OnlyIn(Dist.CLIENT)
    public static void submitClientTask(Runnable task) {
        if (FMLEnvironment.dist.isClient()) {
            submitTask(task);
        }
    }
    
    public static void submitServerTask(Runnable task) {
        if (!FMLEnvironment.dist.isClient()) {
            submitTask(task);
        }
    }
    
    public static void setEnabled(boolean enabled) {
        ThreadManager.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    public static void checkAndCleanDiskCache() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastDiskCacheCheckTime > DISK_CACHE_CHECK_INTERVAL) {
            lastDiskCacheCheckTime = currentTime;
            
            // 清理临时文件
            try {
                File tempDir = new File(System.getProperty("java.io.tmpdir"));
                File[] tempFiles = tempDir.listFiles();
                if (tempFiles != null) {
                    for (File file : tempFiles) {
                        if (file.getName().startsWith("mc-") && file.lastModified() < currentTime - 86400000) { // 清理超过1天的Minecraft临时文件
                            file.delete();
                        }
                    }
                }

System.out.println("已清理过期磁盘缓存");
            } catch (Exception e) {
System.err.println("磁盘缓存清理失败");
e.printStackTrace();
            }
        }
    }

    public static void checkMemoryUsage() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMemoryCheckTime > MEMORY_CHECK_INTERVAL) {
            lastMemoryCheckTime = currentTime;
            
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            
            if (usedMemory > memoryThreshold) {
                memoryOverloaded = true;

System.out.println("警告: 内存使用超过阈值，触发GC清理");
            } else {
                memoryOverloaded = false;
            }
        }
    }

    public static void setMemoryThreshold(int percent) {
        if (percent < 1 || percent > 100) {
            throw new IllegalArgumentException("内存阈值百分比必须在1-100之间");
        }
        memoryThreshold = Runtime.getRuntime().maxMemory() * percent / 100;
    }

    public static String getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        long used = total - free;
        
        return String.format("内存使用: %.2f/%.2fMB (%.1f%%)", 
            used / (1024.0 * 1024.0), 
            max / (1024.0 * 1024.0),
            (used * 100.0) / max);
    }

    private static void handleThreadException(Exception e) {
        if (FMLEnvironment.dist.isClient()) {
System.err.println("客户端线程任务执行异常");
e.printStackTrace();
        } else {
            System.err.println("服务器线程任务执行异常: " + e.getMessage());
        }
    }
}