package com.ljheee.mvc.framework.annotation;

import java.lang.annotation.*;

/**
 * Created by lijianhua04 on 2018/9/24.
 */
@Documented
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MyRequestMapping {
    String value() default "";
}
