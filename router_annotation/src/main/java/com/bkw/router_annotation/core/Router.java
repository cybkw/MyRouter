package com.bkw.router_annotation.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义路由注解
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Router {
    /**
     * 路径名
     */
    String path();

    /**
     * 组名,默认为空
     */
    String group() default "";
}
