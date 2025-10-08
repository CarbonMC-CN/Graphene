package net.carbonmc.graphene.util;

import net.minecraft.nbt.CompoundTag;

public class KryoNBTUtil {
    public static final String KRYO_MARKER = "KryoData";

    public static CompoundTag optimizeWrite(CompoundTag original) {
        if (original == null || original.isEmpty())
            return original;

        byte[] kryoData = KryoContext.serialize(original);
        CompoundTag optimized = new CompoundTag();
        optimized.putByteArray(KRYO_MARKER, kryoData);
        return optimized;
    }

    public static CompoundTag optimizeRead(CompoundTag tag) {
        if (tag == null || !tag.contains(KRYO_MARKER))
            return tag;

        byte[] kryoData = tag.getByteArray(KRYO_MARKER);
        return KryoContext.deserialize(kryoData);
    }
}