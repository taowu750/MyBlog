package com.ncoxs.myblog.util.model;

import java.util.Objects;

public class ArrayUtil {

    public static <E> boolean contains(E[] array, E element) {
        Objects.requireNonNull(array);

        if (element == null) {
            for (E e : array) {
                if (e == null)
                    return true;
            }
        } else {
            for (E e : array) {
                if (element.equals(e))
                    return true;
            }
        }

        return false;
    }
}
