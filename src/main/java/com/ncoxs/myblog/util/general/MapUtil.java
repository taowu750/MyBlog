package com.ncoxs.myblog.util.general;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MapUtil {

    public static class Pair<K, V> {
        public final K key;
        public final V value;

        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    public static <K, V> Map<K, V> ofCap(int capacity) {
        return new HashMap<>((int) (capacity / 0.75) + 1);
    }

    public static <K, V> Pair<K, V> kv(K k, V v) {
        return new Pair<>(k, v);
    }

    public static <K, V> Map<K, V> mp(Pair<K, V> p1) {
        Objects.requireNonNull(p1);

        Map<K, V> result = ofCap(1);
        result.put(p1.key, p1.value);

        return result;
    }

    public static <K, V> Map<K, V> mp(Pair<K, V> p1, Pair<K, V> p2) {
        Objects.requireNonNull(p1);
        Objects.requireNonNull(p2);

        Map<K, V> result = ofCap(2);
        result.put(p1.key, p1.value);
        result.put(p2.key, p2.value);

        return result;
    }

    public static <K, V> Map<K, V> mp(Pair<K, V> p1, Pair<K, V> p2, Pair<K, V> p3) {
        Objects.requireNonNull(p1);
        Objects.requireNonNull(p2);
        Objects.requireNonNull(p3);

        Map<K, V> result = ofCap(3);
        result.put(p1.key, p1.value);
        result.put(p2.key, p2.value);
        result.put(p3.key, p3.value);

        return result;
    }

    public static <K, V> Map<K, V> mp(Pair<K, V>... pairs) {
        if (pairs.length == 0) {
            return new HashMap<>();
        }

        Map<K, V> result = ofCap(pairs.length);
        for (Pair<K, V> pair : pairs) {
            result.put(pair.key, pair.value);
        }

        return result;
    }

    public static <K, V> Map<K, V> mp(K key, V value) {
        Map<K, V> result = ofCap(1);
        result.put(key, value);

        return result;
    }

    public static <K, V> Map<K, V> mp(K k1, K k2, V v1, V v2) {
        Map<K, V> result = ofCap(2);
        result.put(k1, v1);
        result.put(k2, v2);

        return result;
    }

    public static <K, V> Map<K, V> mp(K k1, K k2, K k3, V v1, V v2, V v3) {
        Map<K, V> result = ofCap(3);
        result.put(k1, v1);
        result.put(k2, v2);
        result.put(k3, v3);

        return result;
    }

    public static <K, V> Map<K, V> mp(List<K> keys, List<V> values) {
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
}
