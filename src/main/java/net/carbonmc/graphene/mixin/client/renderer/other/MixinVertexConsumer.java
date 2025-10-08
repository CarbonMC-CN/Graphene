package net.carbonmc.graphene.mixin.client.renderer.other;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(VertexConsumer.class)
public interface MixinVertexConsumer {

    /**
     * @author Red flag with 5 stars--RedStar
     * @reason 避免临时 Vector4f
     */
    @Overwrite
    default VertexConsumer vertex(Matrix4f mat, float x, float y, float z) {
        float xt = mat.m00() * x + mat.m10() * y + mat.m20() * z + mat.m30();
        float yt = mat.m01() * x + mat.m11() * y + mat.m21() * z + mat.m31();
        float zt = mat.m02() * x + mat.m12() * y + mat.m22() * z + mat.m32();
        float wt = mat.m03() * x + mat.m13() * y + mat.m23() * z + mat.m33();

        if (wt != 1.0f) {
            float iw = 1.0f / wt;
            xt *= iw;
            yt *= iw;
            zt *= iw;
        }
        return ((VertexConsumer) this).vertex(xt, yt, zt);
    }

    /**
     * @author Baiyuan
     * @reason 避免临时 Vector3f
     */
    @Overwrite
    default VertexConsumer normal(Matrix3f mat, float x, float y, float z) {
        float nx = mat.m00 * x + mat.m10 * y + mat.m20 * z;
        float ny = mat.m01 * x + mat.m11 * y + mat.m21 * z;
        float nz = mat.m02 * x + mat.m12 * y + mat.m22 * z;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len != 0.0f) {
            len = 1.0f / len;
            nx *= len;
            ny *= len;
            nz *= len;
        }
        return ((VertexConsumer) this).normal(nx, ny, nz);
    }
}