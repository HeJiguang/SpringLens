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
 * HTTP 请求收集器 (HTTP Request Collector)。
 * 它是几乎所有业务执行图的“起点”和“终点”。
 * 当一个 HTTP 请求到来（STARTED）时，它在图中画出一个 HTTP 节点并将图的状态设为 RUNNING。
 * 当请求结束（COMPLETED）时，它在这个节点上记录下耗时、HTTP 状态码。
 */
public final class HttpRequestCollector implements RuntimeCollector {

    @Override
    public String id() {
        return "http-request";
    }

    @Override
    public Set<RuntimeSignalType> supportedTypes() {
        return Set.of(RuntimeSignalType.HTTP_REQUEST_STARTED, RuntimeSignalType.HTTP_REQUEST_COMPLETED);
    }

    @Override
    public void collect(RuntimeSignal signal, GraphMutation graph) {
        if (RuntimeSignalType.HTTP_REQUEST_STARTED.equals(signal.type())) {
            String path = String.valueOf(signal.attributes().getOrDefault("path", "unknown"));
            
            // 将 Path 和 Method 作为全局上下文 Tag 放入图中，以后过滤很容易
            graph.putContextTag("path", path);
            graph.putContextTag("method", String.valueOf(signal.attributes().getOrDefault("method", "GET")));
            
            // 确保图中创建了一个代表 HTTP 入口的根节点
            Map<String, Object> attributes = new LinkedHashMap<>(signal.attributes());
            attributes.putIfAbsent("_originKind", ExecutionOriginKind.COMPAT_FILTER.value());
            attributes.putIfAbsent("_sourceRef", "io.springlens.starter.LensRequestFilter");
            graph.ensureNode(NodeType.HTTP_REQUEST, path, signal.occurredAt(), attributes);
            return;
        }

        // 如果是 HTTP_REQUEST_COMPLETED 信号，也就是请求处理完了，需要去更新那个 HTTP 节点
        graph.firstNodeId(NodeType.HTTP_REQUEST).ifPresent(nodeId -> {
            int statusCode = asInt(signal.attributes().get("status"), 200);
            NodeStatus status = statusCode >= 500 ? NodeStatus.FAILURE : NodeStatus.SUCCESS;
            graph.updateNode(nodeId, status, signal.occurredAt(), Map.of(
                    "status", statusCode,
                    "durationMs", asLong(signal.attributes().get("durationMs"), 0L)
            ));
        });
    }

    private static int asInt(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static long asLong(Object value, long fallback) {
        return value instanceof Number number ? number.longValue() : fallback;
    }
}
