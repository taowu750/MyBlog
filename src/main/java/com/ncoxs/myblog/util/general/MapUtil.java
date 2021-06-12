package com.ncoxs.myblog.util.general;

import java.util.*;

import static java.util.Arrays.asList;

public class MapUtil {

    public static <K, V> Map<K, V> of(K key, V value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);

        Map<K, V> result = new HashMap<>(2);
        result.put(key, value);

        return result;
    }

    public static <K, V> Map<K, V> of(List<K> keys, List<V> values) {
        Objects.requireNonNull(keys);
        Objects.requireNonNull(values);

        if (keys.size() != values.size())
            throw new IllegalArgumentException("Number of keys and values do not match");
        if (keys.size() == 0)
            throw new IllegalArgumentException("The number of key-value pairs cannot be equal to 0");

        Map<K, V> result = new HashMap<>((int) (keys.size() / 0.75) + 1);
        for (int i = 0; i < keys.size(); i++) {
            result.put(keys.get(i), values.get(i));
        }
        if (result.size() != keys.size())
            throw new IllegalArgumentException("There are duplicate keys");

        return result;
    }

    public static <K, V> Map<K, V> of(K k1, K k2, V v1, V v2) {
        return of(asList(k1, k2), asList(v1, v2));
    }

    public static <K, V> Map<K, V> of(K k1, K k2, K k3, V v1, V v2, V v3) {
        return of(asList(k1, k2, k3), asList(v1, v2, v3));
    }

    public static <K, V> Map<K, V> create(int capacity) {
        return new HashMap<>((int) (capacity / 0.75) + 1);
    }
}
