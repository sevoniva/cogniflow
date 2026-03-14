package com.chatbi.annotation;

import java.lang.annotation.*;

/**
 * 动态数据源切换注解
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataSourceSwitch {

    /**
     * 数据源 ID
     */
    String value() default "master";
}
