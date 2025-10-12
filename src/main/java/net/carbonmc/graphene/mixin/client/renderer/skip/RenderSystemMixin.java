package net.carbonmc.graphene.mixin.client.renderer.skip;

import com.mojang.blaze3d.systems.RenderSystem;
import net.carbonmc.graphene.client.GrapheneClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {
    @Redirect(method = "flipFrame", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V"), remap = false)
    private static void redirectSwapBuffers(long window) {
        GrapheneClient.displayController.run();
    }
}