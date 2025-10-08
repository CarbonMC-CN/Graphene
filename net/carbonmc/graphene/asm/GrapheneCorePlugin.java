package net.carbonmc.graphene.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.function.Function;
//1.20.2以上不移植
public class GrapheneCorePlugin implements Function<ClassNode, ClassNode>, Opcodes {

    @Override
    public ClassNode apply(ClassNode node) {
        if (!"net/minecraft/world/entity/projectile/Projectile".equals(node.name)) {
            return node;
        }

        for (MethodNode mn : node.methods) {
            if ("tick".equals(mn.name) && "()V".equals(mn.desc)) {
                patchTick(mn);
            }
            if ("lerpMotion".equals(mn.name) && "(DDD)V".equals(mn.desc)) {
                patchLerpMotion(mn);
            }
        }
        return node;
    }

    private void patchTick(MethodNode mn) {
        for (AbstractInsnNode ain : mn.instructions) {
            if (ain instanceof FieldInsnNode fin
                    && fin.getOpcode() == GETFIELD
                    && fin.name.equals("lastRenderX")) {
                fin.name = "lastTrackedX";
                fin.desc = "D";
            }
            if (ain instanceof FieldInsnNode fin
                    && fin.getOpcode() == PUTFIELD
                    && fin.name.equals("lastRenderX")) {
                fin.name = "lastTrackedX";
                fin.desc = "D";
            }
            if (ain instanceof FieldInsnNode fin
                    && fin.getOpcode() == GETFIELD
                    && fin.name.equals("lastRenderY")) {
                fin.name = "lastTrackedY";
            }
            if (ain instanceof FieldInsnNode fin
                    && fin.getOpcode() == PUTFIELD
                    && fin.name.equals("lastRenderY")) {
                fin.name = "lastTrackedY";
            }
            if (ain instanceof FieldInsnNode fin
                    && fin.getOpcode() == GETFIELD
                    && fin.name.equals("lastRenderZ")) {
                fin.name = "lastTrackedZ";
            }
            if (ain instanceof FieldInsnNode fin
                    && fin.getOpcode() == PUTFIELD
                    && fin.name.equals("lastRenderZ")) {
                fin.name = "lastTrackedZ";
            }
        }
    }

    private void patchLerpMotion(MethodNode mn) {
        for (AbstractInsnNode ain : mn.instructions) {
            if (ain instanceof FieldInsnNode fin
                    && fin.getOpcode() == GETFIELD
                    && (fin.name.equals("lastRenderX")
                    || fin.name.equals("lastRenderY")
                    || fin.name.equals("lastRenderZ"))) {
                fin.name = fin.name.replace("lastRender", "lastTracked");
            }
        }
    }
}