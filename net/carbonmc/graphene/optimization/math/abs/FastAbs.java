package net.carbonmc.graphene.optimization.math.abs;

import sun.misc.Unsafe;
import java.lang.reflect.Field;

public final class FastAbs {
    private static final FastAbs INSTANCE = new FastAbs();
    private static final Unsafe UNSAFE;
    private static final long INT_ARRAY_OFFSET;
    private static final long FLOAT_ARRAY_OFFSET;

    static {
        Unsafe unsafeInstance = null;
        long intArrayOffset = 0;
        long floatArrayOffset = 0;

        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafeInstance = (Unsafe) field.get(null);
            intArrayOffset = unsafeInstance.arrayBaseOffset(int[].class);
            floatArrayOffset = unsafeInstance.arrayBaseOffset(float[].class);
        } catch (Exception e) {
            System.err.println("Unsafe initialization failed");
        }

        UNSAFE = unsafeInstance;
        INT_ARRAY_OFFSET = intArrayOffset;
        FLOAT_ARRAY_OFFSET = floatArrayOffset;
    }

    private FastAbs() {}

    public static FastAbs getInstance() {
        return INSTANCE;
    }

    public static int abs(int x) {
        int mask = x >> 31;
        return (x + mask) ^ mask;
    }

    public static float abs(float x) {
        return Float.intBitsToFloat(Float.floatToRawIntBits(x) & 0x7FFFFFFF);
    }


    }
