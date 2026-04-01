package io.springlens.runtime;

import io.springlens.model.core.ExecutionOriginKind;
import io.springlens.model.core.NodeStatus;
import io.springlens.model.core.NodeType;
import io.springlens.spi.GraphMutation;
import io.springlens.spi.RuntimeCollector;
import io.springlens.spi.RuntimeSignal;
import io.springlens.spi.RuntimeSignalType;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 异常收集器 (Exception Collector)。
 * 专门监听业务代码逻辑抛出的被抛出并未捕获（或被拦截器截获）的 Exception。
 * 它会在执行图上挂载一个表示异常的子节点。
 */
public final class ExceptionCollector implements RuntimeCollector {

    @Override
    public String id() {
        return "exception";
    }

    @Override
    public Set<RuntimeSignalType> supportedTypes() {
        return Set.of(RuntimeSignalType.EXCEPTION_CAPTURED);
    }

    @Override
    public void collect(RuntimeSignal signal, GraphMutation graph) {
        Map<String, Object> attributes = new LinkedHashMap<>(signal.attributes());
        attributes.put("_originKind", ExecutionOriginKind.COMPAT_INTERCEPTOR.value());
        attributes.put("_sourceRef", "io.springlens.starter.LensExceptionInterceptor");

        String exceptionNodeId = graph.addNode(
                NodeType.EXCEPTION,
                String.valueOf(signal.attributes().getOrDefault("exceptionClass", "unknown")),
                NodeStatus.FAILURE,
                signal.occurredAt(),
                signal.occurredAt(),
                attributes
        );
        
        // 尝试找到 HTTP 入口节点，然后把这个异常挂在 HTTP 父节点下面（这说明是在这次请求中抛出的）
        graph.firstNodeId(NodeType.HTTP_REQUEST)
                .ifPresent(requestNodeId -> graph.addEdge(requestNodeId, exceptionNodeId, "child_of"));
    }
}
