package io.springlens.runtime;

import io.springlens.model.core.NodeStatus;
import io.springlens.model.core.NodeType;
import io.springlens.spi.GraphMutation;
import io.springlens.spi.RuntimeCollector;
import io.springlens.spi.RuntimeSignal;
import io.springlens.spi.RuntimeSignalType;
import java.util.Set;

/**
 * 探针数据收集器 (Probe Value Collector)。
 * 在业务代码或注解 `@LensWatch` 拦截到的变量具体数值，将被转换成一个节点。
 * 从而实现了 AI Agent 可以像看 Debug Watch 一样，直接从执行图里拉取到参数或局部变量。
 */
public final class ProbeValueCollector implements RuntimeCollector {

    @Override
    public String id() {
        return "probe-value";
    }

    @Override
    public Set<RuntimeSignalType> supportedTypes() {
        return Set.of(RuntimeSignalType.PROBE_VALUE_CAPTURED);
    }

    @Override
    public void collect(RuntimeSignal signal, GraphMutation graph) {
        String probeNodeId = graph.addNode(
                NodeType.WATCH_VALUE,
                String.valueOf(signal.attributes().getOrDefault("probeId", "probe")),
                NodeStatus.SUCCESS,
                signal.occurredAt(),
                signal.occurredAt(),
                signal.attributes()
        );
        
        // 与异常和慢查询不同的是，“观察到了这个变量”，这也属于该请求链路上发生的一环事件。
        graph.firstNodeId(NodeType.HTTP_REQUEST)
                .ifPresent(requestNodeId -> graph.addEdge(requestNodeId, probeNodeId, "observes"));
    }
}
