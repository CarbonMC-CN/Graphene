package net.carbonmc.graphene.util;

import net.carbonmc.graphene.config.CoolConfig;
import org.lwjgl.glfw.GLFW;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Fpsu {
    public final Queue<Runnable> queue = new ConcurrentLinkedQueue<>();
    private final long window;
    private int targetUpdates;
    private long lastSwapTime;

    public Fpsu(long window) {
        this.window = window;
        GLFW.glfwSwapInterval(0);
        this.targetUpdates = CoolConfig.UDT.get();
        this.lastSwapTime = System.currentTimeMillis();
    }

    public void run() {
        long currentTime = System.currentTimeMillis();
        long interval = 1000 / targetUpdates;

        if (currentTime - lastSwapTime >= interval) {
            GLFW.glfwSwapBuffers(window);
            lastSwapTime = currentTime;
        }
    }
}