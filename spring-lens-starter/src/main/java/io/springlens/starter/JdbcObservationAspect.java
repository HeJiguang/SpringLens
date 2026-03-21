package io.springlens.starter;

import io.springlens.runtime.RuntimeSignalProcessor;
import io.springlens.spi.RuntimeSignal;
import io.springlens.spi.RuntimeSignalType;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * 数据库操作侦测切面 (AOP Aspect)。
 * 基于 Spring AOP 技术，专门环绕拦截 (Around) 所有对 `org.springframework.jdbc.core.JdbcOperations` 接口的方法调用。
 * 拦截时计算耗时，生成一个带有 `JDBC_EXECUTED` 类型的原初信号事件抛出去。
 */
@Aspect
public class JdbcObservationAspect {

    private final RuntimeSignalProcessor signalProcessor;
    private final RuntimeExecutionContextHolder contextHolder;
    private final LensRuntimeProperties properties;

    public JdbcObservationAspect(
            RuntimeSignalProcessor signalProcessor,
            RuntimeExecutionContextHolder contextHolder,
            LensRuntimeProperties properties
    ) {
        this.signalProcessor = signalProcessor;
        this.contextHolder = contextHolder;
        this.properties = properties;
    }

    @Around("execution(* org.springframework.jdbc.core.JdbcOperations.*(..))")
    public Object observeJdbc(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] arguments = joinPoint.getArgs();
        // 取出要执行的 SQL 语句文本，如果不是 String 返回了，说明调用的可能是其他重载，就不抓取了直接放行
        if (arguments.length == 0 || !(arguments[0] instanceof String sql)) {
            return joinPoint.proceed();
        }

        Instant startedAt = Instant.now();
        try {
            // 继续向下层 JDBC 发起实际调用，跑业务代码
            return joinPoint.proceed();
        }
        finally {
            // 无论执行成功还是失败，只要有耗时结果就算。
            long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
            
            // 是否打上 slow 的标签供后续 Collector 去裁决
            boolean slow = durationMs >= properties.getSlowSqlThresholdMs();
            
            contextHolder.currentExecutionId().ifPresent(executionId -> {
                Map<String, Object> attributes = new LinkedHashMap<>();
                attributes.put("sql", sql);
                attributes.put("durationMs", durationMs);
                attributes.put("slow", slow);
                signalProcessor.process(new RuntimeSignal(
                        executionId,
                        RuntimeSignalType.JDBC_EXECUTED,
                        Instant.now(),
                        attributes
                ));
            });
        }
    }
}
