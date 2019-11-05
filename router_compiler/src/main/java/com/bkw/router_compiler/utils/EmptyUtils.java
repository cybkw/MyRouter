package com.bkw.router_compiler.utils;

import java.util.Collection;
import java.util.Map;

public class EmptyUtils {

    public static boolean isEmpty(final Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static boolean isEmpty(Collection<?> list) {
        return list == null || list.isEmpty();
    }


    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }
}
