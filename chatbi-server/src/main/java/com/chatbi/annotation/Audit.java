package com.chatbi.annotation;

import com.chatbi.common.constant.SysConstant;

import java.lang.annotation.*;

/**
 * 审计日志注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audit {

    /**
     * 操作类型
     */
    String action() default SysConstant.ACTION_QUERY;

    /**
     * 资源类型
     */
    String resourceType() default "";
}
