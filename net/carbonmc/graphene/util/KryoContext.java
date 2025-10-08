package net.carbonmc.graphene.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;
import net.minecraft.nbt.CompoundTag;

public class KryoContext {
    private static final Pool<Kryo> KRYO_POOL = new Pool<Kryo>(true, false, 16) {
        @Override
        protected Kryo create() {
            Kryo kryo = new Kryo();
            kryo.register(CompoundTag.class);
            kryo.setReferences(true);
            kryo.setRegistrationRequired(false);
            return kryo;
        }
    };

    private static final Pool<Output> OUTPUT_POOL = new Pool<Output>(true, false, 32) {
        @Override
        protected Output create() {
            return new Output(1024, -1);
        }
    };

    private static final Pool<Input> INPUT_POOL = new Pool<Input>(true, false, 32) {
        @Override
        protected Input create() {
            return new Input(1024);
        }
    };

    public static byte[] serialize(CompoundTag tag) {
        Kryo kryo = KRYO_POOL.obtain();
        Output output = OUTPUT_POOL.obtain();
        try {
            output.reset();
            kryo.writeObject(output, tag);
            return output.toBytes();
        } finally {
            KRYO_POOL.free(kryo);
            OUTPUT_POOL.free(output);
        }
    }

    public static CompoundTag deserialize(byte[] data) {
        if (data == null || data.length == 0) return null;

        Kryo kryo = KRYO_POOL.obtain();
        Input input = INPUT_POOL.obtain();
        try {
            input.setBuffer(data);
            return kryo.readObject(input, CompoundTag.class);
        } finally {
            KRYO_POOL.free(kryo);
            INPUT_POOL.free(input);
        }
    }
}