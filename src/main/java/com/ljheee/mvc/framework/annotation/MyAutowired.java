package com.ljheee.mvc.framework.annotation;

import java.lang.annotation.*;

/**
 * Created by lijianhua04 on 2018/9/24.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MyAutowired {
    String value() default "";
}
