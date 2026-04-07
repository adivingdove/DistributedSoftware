package com.distributed.inventory.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(-1)
public class DataSourceAspect {

    private static final Logger log = LoggerFactory.getLogger(DataSourceAspect.class);

    @Around("@annotation(readOnly)")
    public Object around(ProceedingJoinPoint point, ReadOnly readOnly) throws Throwable {
        DynamicDataSourceContextHolder.setDataSourceType(DynamicDataSourceContextHolder.SLAVE);
        log.info("[读写分离] 切换到从库 -> {}", point.getSignature().toShortString());
        try {
            return point.proceed();
        } finally {
            DynamicDataSourceContextHolder.clear();
            log.info("[读写分离] 恢复到主库 -> {}", point.getSignature().toShortString());
        }
    }
}
