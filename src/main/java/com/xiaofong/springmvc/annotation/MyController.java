package com.xiaofong.springmvc.annotation;

import java.lang.annotation.*;

/**
 * 自定义注解标识一个类为控制层
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyController {

    String value() default "";

}
