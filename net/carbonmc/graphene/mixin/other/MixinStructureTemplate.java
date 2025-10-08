package net.carbonmc.graphene.mixin.other;

import net.carbonmc.graphene.util.KryoNBTUtil;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StructureTemplate.class)
public abstract class MixinStructureTemplate {

    // 方案1：修改保存时的NBT变量
    @ModifyVariable(
            method = "save",
            at = @At("HEAD"),
            argsOnly = true
    )
    private CompoundTag onSave(CompoundTag original) {
        return KryoNBTUtil.optimizeWrite(original);
    }

    // 方案3：备用加载入口
    @Inject(
            method = "load(Lnet/minecraft/core/HolderGetter;Lnet/minecraft/nbt/CompoundTag;)V",
            at = @At("HEAD")
    )
    private void onLoadStart(
            HolderGetter<Block> blockGetter,
            CompoundTag tag,
            CallbackInfo ci
    ) {
        if (tag != null) {
            KryoNBTUtil.optimizeRead(tag);
        }
    }
}