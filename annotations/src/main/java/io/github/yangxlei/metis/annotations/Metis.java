package io.github.yangxlei.metis.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by yanglei on 2017/6/29.
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Metis {
    Class<?>[] value();
}
