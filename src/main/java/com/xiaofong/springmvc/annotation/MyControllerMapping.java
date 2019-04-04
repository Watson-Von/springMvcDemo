package com.xiaofong.springmvc.annotation;

import java.lang.annotation.*;

/**
 * 自定义一个注解标识一个url
 */
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyControllerMapping {

    String value() default "";

}
