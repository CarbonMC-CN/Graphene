package net.carbonmc.graphene.util;

import java.util.*;
import jdk.internal.vm.annotation.Contended;

public final class StateIndexer<V> {

    /* ---------------- 常量 ---------------- */
    private static final int MAX_STATES_32 = 1 << 24;   // 位宽压缩阈值
    private static final int CACHE_LINE    = 64 / 4;    // int 个数

    /* ---------------- 字段 ---------------- */
    @Contended
    private final int[] strides;          // 64 B 对齐
    private final int[] valueCounts;
    private final long[] masks;           // 预掩码
    private final V[] values;
    private final int stateCount;
    private final int attrCount;
    private final boolean use32;          // 是否压缩成 32-bit

    /* ---------------- 构造 ---------------- */
    @SuppressWarnings("unchecked")
    public StateIndexer(List<? extends Attribute<?>> attrs,
                        Map<Map<String, ?>, V> stateValues,
                        boolean compress) {
        this.attrCount = attrs.size();
        int product = 1;
        for (Attribute<?> a : attrs) product *= a.valueCount();
        this.use32 = compress && (product <= MAX_STATES_32);
        this.stateCount = product;

        int strideLen = (attrCount + CACHE_LINE - 1) & ~(CACHE_LINE - 1);
        strides      = new int[strideLen];
        valueCounts  = new int[strideLen];
        masks        = new long[strideLen];

        for (int i = attrCount - 1, p = 1; i >= 0; i--) {
            int vc = attrs.get(i).valueCount();
            strides[i]     = p;
            valueCounts[i] = vc;
            masks[i]       = ~((long) (vc - 1) * p);
            p *= vc;
        }

        int valueLen = (product + CACHE_LINE - 1) & ~(CACHE_LINE - 1);
        values = (V[]) new Object[valueLen];
        stateValues.forEach((assign, v) -> values[indexOf(assign)] = v);
    }

    /* ---------------- API：0 分支 neighbor ---------------- */
    /** 保证 newOrdinal 合法（预掩码）→ 0 判断、0 异常 */
    public int neighbor(int stateIndex, String attrName, int newOrdinal) {
        int i = findAttr(attrName);
        return (int) ((stateIndex & masks[i]) | ((long) newOrdinal * strides[i]));
    }

    public V value(int stateIndex) { return values[stateIndex]; }

    public void setValue(int stateIndex, V v) { values[stateIndex] = v; }

    public int indexOf(Map<String, ?> assign) {
        int idx = 0;
        for (int i = 0; i < attrCount; i++) {
            int ord = attributes.get(i).toOrdinal(assign.get(attributes.get(i).name()));
            if (ord < 0) throw new IllegalArgumentException("非法值");
            idx += ord * strides[i];
        }
        return idx;
    }
    public int stateCount() { return stateCount; }

    public boolean isCompressed32() { return use32; }

    /* ---------------- 属性查找 ---------------- */
    final List<? extends Attribute<?>> attributes = List.of();

    int findAttr(String name) {
        for (int i = 0; i < attrCount; i++)
            if (attributes.get(i).name().equals(name)) return i;
        throw new IllegalArgumentException("Unknown property: " + name);
    }

    /* ---------------- Attribute 接口（不变） ---------------- */
    public interface Attribute<T> {
        String name();
        int valueCount();
        int toOrdinal(Object value);
        T toValue(int ordinal);
    }
}