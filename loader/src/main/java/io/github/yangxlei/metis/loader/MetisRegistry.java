package io.github.yangxlei.metis.loader;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by yanglei on 2017/6/29.
 */

public final class MetisRegistry {

    private static final Map<Class<?>, Set<Class<?>>> sServices = new HashMap<>();

    public static Set<Class<?>> get(Class key) {
        Set<Class<?>> result = sServices.get(key);
        return result == null ? Collections.<Class<?>>emptySet() : Collections.unmodifiableSet(result);
    }

    private static void register(Class key, Class<?> value) {
        Set<Class<?>> result = sServices.get(key);
        if (result == null) {
            result = new HashSet<>();
            sServices.put(key, result);
        }
        result.add(value);
    }
}
