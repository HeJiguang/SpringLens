package io.springlens.starter;

import io.springlens.model.core.ExecutionContext;
import io.springlens.runtime.RuntimeSignalProcessor;
import io.springlens.spi.RuntimeSignal;
import io.springlens.spi.RuntimeSignalType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.env.Environment;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 全局 HTTP 请求过滤器 (Filter)。
 * 它是抓取流量的“第一道门神”，每次有用户的 HTTP 请求进来（除了内部拉取 API 之外），
 * 它就会在这里悄悄记录，派发 HTTP_REQUEST_STARTED/COMPLETED 伪造系统事件发给 Processor 处理。
 */
public class LensRequestFilter extends OncePerRequestFilter {

    private final RuntimeSignalProcessor signalProcessor;
    private final RuntimeExecutionContextHolder contextHolder;
    private final LensRuntimeProperties properties;
    private final Environment environment;

    public LensRequestFilter(
            RuntimeSignalProcessor signalProcessor,
            RuntimeExecutionContextHolder contextHolder,
            LensRuntimeProperties properties,
            Environment environment
    ) {
        this.signalProcessor = signalProcessor;
        this.contextHolder = contextHolder;
        this.properties = properties;
        this.environment = environment;
    }

    /**
     * 放过 Spring Lens 自身的拉取查询端点不代理，避免“看自己的运行日志而无限套娃”。
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/internal/spring-lens");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Instant startedAt = Instant.now();
        // 1. 每进来一次强行拉起一个全新的 executionId 
        String executionId = UUID.randomUUID().toString();
        ExecutionContext context = new ExecutionContext(
                applicationId(),
                properties.getInstanceId(),
                executionId,
                startedAt,
                Map.of("path", request.getRequestURI())
        );

        // 2. 告诉大堂经理：有一桌客人的全新执行即将开始
        signalProcessor.start(context);
        
        // 3. 把 executionId 放到执行线程上下文 (ThreadLocal) 里，方便后续其他地方任意获取
        contextHolder.set(executionId);
        
        // 4. 发出真实的“请求进入”信号事件
        signalProcessor.process(new RuntimeSignal(
                executionId,
                RuntimeSignalType.HTTP_REQUEST_STARTED,
                startedAt,
                Map.of("method", request.getMethod(), "path", request.getRequestURI())
        ));

        try {
            // 给 Spring 去跑正经的 Controller 业务...
            filterChain.doFilter(request, response);
        }
        finally {
            // 5. 不论有无异常或正常结束，最后都清理上下文，并且发送“HTTP请求执行结束”信号。
            signalProcessor.process(new RuntimeSignal(
                    executionId,
                    RuntimeSignalType.HTTP_REQUEST_COMPLETED,
                    Instant.now(),
                    Map.of(
                            "status", response.getStatus(),
                            "durationMs", java.time.Duration.between(startedAt, Instant.now()).toMillis()
                    )
            ));
            contextHolder.clear();
        }
    }

    private String applicationId() {
        if (properties.getApplicationId() != null && !properties.getApplicationId().isBlank()) {
            return properties.getApplicationId();
        }
        return environment.getProperty("spring.application.name", "spring-lens-app");
    }
}
