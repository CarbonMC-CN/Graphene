package net.carbonmc.graphene.util;

import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.*;

public final class BlockStateIndexerBuilder {

    public static <O, S extends StateHolder<O, S>>
    StateIndexer<S> build(Collection<S> states) {
        if (states.isEmpty()) throw new IllegalStateException("empty states");

        S any = states.iterator().next();
        List<Property<?>> props = new ArrayList<>(any.getProperties());

        List<PropertyAttribute> attrs = props.stream()
                .map(PropertyAttribute::new)
                .toList();

        Map<Map<String, ?>, S> data = new HashMap<>();
        for (S s : states) {
            Map<String, Comparable<?>> map = new LinkedHashMap<>();
            for (Property<?> p : props) {
                map.put(p.getName(), s.getValue(p));
            }
            data.put(map, s);
        }
        return new StateIndexer<>(attrs, data,true);
    }

    /* ---------------- 内部实现 ---------------- */
    private static final class PropertyAttribute
            implements StateIndexer.Attribute<Comparable<?>> {

        private final Property<?> prop;
        private final List<Comparable<?>> values;

        PropertyAttribute(Property<?> prop) {
            this.prop = prop;
            this.values = new ArrayList<>(prop.getPossibleValues());
        }

        public String name() { return prop.getName(); }
        public int valueCount() { return values.size(); }

        public int toOrdinal(Object value) {
            int idx = values.indexOf(value);
            return idx < 0 ? -1 : idx;
        }

        public Comparable<?> toValue(int ordinal) {
            return values.get(ordinal);
        }
    }
}