package net.carbonmc.graphene.gl;
public final class CleanupAction implements Runnable {
    private final Runnable cleanupTask;

    public CleanupAction(Runnable cleanupTask) {
        this.cleanupTask = cleanupTask;
    }

    @Override
    public void run() {
        cleanupTask.run();
    }
}