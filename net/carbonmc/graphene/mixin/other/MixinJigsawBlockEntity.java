package net.carbonmc.graphene.mixin.other;

import net.carbonmc.graphene.util.KryoNBTUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(JigsawBlockEntity.class)
public abstract class MixinJigsawBlockEntity {
    @Unique private CompoundTag kryo$cachedTag;

    @Inject(method = "saveAdditional", at = @At("HEAD"), cancellable = true)
    private void onSave(CompoundTag tag, CallbackInfo ci) {
        kryo$cachedTag = KryoNBTUtil.optimizeWrite(tag);
        if (kryo$cachedTag != tag) {
            Set<String> keys = kryo$cachedTag.getAllKeys();
            for (String key : keys) {
                if (!tag.contains(key)) {
                    tag.put(key, kryo$cachedTag.get(key));
                }
            }
            ci.cancel();
        }
    }

    @ModifyVariable(method = "load", at = @At("HEAD"), argsOnly = true)
    private CompoundTag onLoad(CompoundTag tag) {
        return KryoNBTUtil.optimizeRead(tag);
    }
}