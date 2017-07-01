package io.github.yangxlei.metis.loader;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by yanglei on 2017/6/29.
 */

public final class MetisLoader<S> implements Iterable<Class<S>> {

    public static final <S> MetisLoader<S> load(Class<S> loadClass) {
        return new MetisLoader<>(loadClass);
    }

    private Set<Class<S>> mProviders;

    private MetisLoader(Class<S> klass) {
        mProviders = new LinkedHashSet<>();
        for (Class<?> clazz : MetisRegistry.get(klass)) {
            mProviders.add((Class<S>) clazz);
        }
    }

    @Override
    public Iterator<Class<S>> iterator() {
        return mProviders.iterator();
    }
}
