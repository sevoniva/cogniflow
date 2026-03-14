package com.chatbi.aspect;

import com.chatbi.annotation.DataSourceSwitch;
import com.chatbi.datasource.DynamicDataSourceContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 数据源切换切面
 */
@Slf4j
@Aspect
@Component
@Order(1) // 确保在事务切面之前执行
public class DataSourceSwitchAspect {

    @Pointcut("@annotation(com.chatbi.annotation.DataSourceSwitch)")
    public void dataSourcePointcut() {
    }

    @Around("dataSourcePointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        // 获取注解
        DataSourceSwitch annotation = method.getAnnotation(DataSourceSwitch.class);
        if (annotation == null) {
            // 尝试从类上获取
            annotation = point.getTarget().getClass().getAnnotation(DataSourceSwitch.class);
        }

        if (annotation != null) {
            String dataSourceKey = annotation.value();
            log.debug("切换数据源到：{}", dataSourceKey);
            DynamicDataSourceContextHolder.setDataSourceKey(dataSourceKey);
        }

        try {
            return point.proceed();
        } finally {
            // 清空上下文
            DynamicDataSourceContextHolder.clear();
            log.debug("清空数据源上下文");
        }
    }
}
