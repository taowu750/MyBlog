package com.ncoxs.myblog.util.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CollectionUtil {

    public static <T> List<T> iterator2list(Iterator<T> iterator) {
        List<T> result = new ArrayList<>();
        while (iterator.hasNext()) {
            result.add(iterator.next());
        }

        return result;
    }

    public static <T> T getFirst(List<T> list) {
        if (list == null) {
            return null;
        } else {
            return list.get(0);
        }
    }
}
