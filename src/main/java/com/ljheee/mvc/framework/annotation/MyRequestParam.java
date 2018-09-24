package com.ljheee.mvc.framework.annotation;

import java.lang.annotation.*;

/**
 * Created by lijianhua04 on 2018/9/24.
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyRequestParam {
    String value() default "";
}
