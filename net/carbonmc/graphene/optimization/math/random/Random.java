package net.carbonmc.graphene.optimization.math.random;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.BitRandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

import org.jetbrains.annotations.NotNull;
import sun.misc.Unsafe;
import java.lang.reflect.Field;

public final class Random implements BitRandomSource, RandomSource {
    private static final Unsafe U;
    private static final long S;
    private long s0, s1;
    private static final long G = 0x9e3779b97f4a7c15L;

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            U = (Unsafe) f.get(null);
            S = U.objectFieldOffset(Random.class.getDeclaredField("s0"));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public Random(long seed) {
        seed ^= G;
        s0 = mix(seed);
        s1 = mix(seed + G);
    }

    private static long mix(long x) {
        x ^= x >>> 32;
        x *= 0xbea225f9eb34556dL;
        x ^= x >>> 29;
        return x;
    }

    public @NotNull RandomSource fork() {
        return new Random(nextLong());
    }

    public @NotNull PositionalRandomFactory forkPositional() {
        return new LegacyRandomSource.LegacyPositionalRandomFactory(nextLong());
    }

    public void setSeed(long seed) {
        seed ^= G;
        U.putLong(this, S, mix(seed));
        s1 = mix(seed + G);
    }

    public int next(int bits) {
        long z = s0 + s1;
        s1 = Long.rotateLeft(s0, 55);
        s0 = Long.rotateLeft((z ^ (z >>> 14) ^ (s1 << 17)), 36);
        return (int) (z >>> (64 - bits));
    }

    public int nextInt() {
        return next(32);
    }

    public int nextInt(int bound) {
        int m = bound - 1;
        if ((bound & m) == 0) return (int) ((bound * (long) next(31)) >> 31);
        int r;
        do r = next(31);
        while (r - (r % bound) + m < 0);
        return r % bound;
    }

    public long nextLong() {
        return ((long) next(32) << 32) ^ next(32);
    }

    public boolean nextBoolean() {
        return next(1) != 0;
    }

    public float nextFloat() {
        return next(24) * 0x1.0p-24f;
    }

    public double nextDouble() {
        return (((long) next(26) << 27) + next(27)) * 0x1.0p-53d;
    }

    public double nextGaussian() {
        return Double.NaN; // drop Gaussian for speed
    }

    public void consumeCount(int n) {
        for (int i = 0; i < n; i++) nextLong();
    }
}