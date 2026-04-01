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
        Map<String, Object> attributes = new LinkedHashMap<>(signal.attributes());
        attributes.put("_originKind", resolveOriginKind(signal).value());
        attributes.put("_sourceRef", resolveSourceRef(signal));

        String probeNodeId = graph.addNode(
                NodeType.WATCH_VALUE,
                String.valueOf(signal.attributes().getOrDefault("probeId", "probe")),
                NodeStatus.SUCCESS,
                signal.occurredAt(),
                signal.occurredAt(),
                attributes
        );

        graph.firstNodeId(NodeType.HTTP_REQUEST)
                .ifPresent(requestNodeId -> graph.addEdge(requestNodeId, probeNodeId, "child_of"));
    }

    private ExecutionOriginKind resolveOriginKind(RuntimeSignal signal) {
        Object captureSource = signal.attributes().get("captureSource");
        if ("annotation".equals(captureSource)) {
            return ExecutionOriginKind.LENS_WATCH;
        }
        if ("manual".equals(captureSource)) {
            return ExecutionOriginKind.LENS_LOOK;
        }
        if ("agent-overlay".equals(captureSource)) {
            return ExecutionOriginKind.AGENT_OVERLAY;
        }
        return ExecutionOriginKind.BASE;
    }

    private String resolveSourceRef(RuntimeSignal signal) {
        Object overlayId = signal.attributes().get("overlayId");
        if (overlayId != null) {
            return "overlay:" + overlayId;
        }
        return "io.springlens.starter.probe.LensProbeCaptureService";
    }
}
