package io.springlens.starter;

import io.springlens.runtime.RuntimeSignalProcessor;
import io.springlens.spi.RuntimeSignal;
import io.springlens.spi.RuntimeSignalType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 拦截未处理异常的探测器 (Spring Web MVC Interceptor)。
 * 它在 Controller 方法跑完或者抛出未捕获的错误时触发。
 * 如果它能摸到抛出来的 Exception，就把它包裹成 RuntimeSignal 发出去。
 */
public class LensExceptionInterceptor implements HandlerInterceptor {

    private final RuntimeSignalProcessor signalProcessor;
    private final RuntimeExecutionContextHolder contextHolder;

    public LensExceptionInterceptor(RuntimeSignalProcessor signalProcessor, RuntimeExecutionContextHolder contextHolder) {
        this.signalProcessor = signalProcessor;
        this.contextHolder = contextHolder;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (ex == null) {
            return;
        }
        // 如果是从 Filter 传下来的某个有效业务请求
        contextHolder.currentExecutionId().ifPresent(executionId -> signalProcessor.process(new RuntimeSignal(
                executionId,
                RuntimeSignalType.EXCEPTION_CAPTURED,
                Instant.now(),
                Map.of(
                        "exceptionClass", ex.getClass().getName(),
                        "message", ex.getMessage() == null ? "" : ex.getMessage(),
                        "stackTrace", Arrays.stream(ex.getStackTrace()).limit(8).map(StackTraceElement::toString).toList()
                )
        )));
    }
}
