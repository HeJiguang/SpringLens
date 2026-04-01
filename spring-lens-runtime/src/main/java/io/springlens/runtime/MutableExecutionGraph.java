package io.springlens.runtime;

import io.springlens.model.core.ExecutionContext;
import io.springlens.model.core.ExecutionEdge;
import io.springlens.model.core.ExecutionGraph;
import io.springlens.model.core.ExecutionNode;
import io.springlens.model.core.ExecutionOriginKind;
import io.springlens.model.core.NodeStatus;
import io.springlens.model.core.NodeType;
import io.springlens.spi.GraphMutation;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

final class MutableExecutionGraph implements GraphMutation {

    private final ExecutionContext seedContext;
    private final Map<String, String> contextTags;
    private final Map<String, NodeState> nodes;
    private final Map<NodeType, List<String>> nodeIdsByType;
    private final List<ExecutionEdge> edges;

    MutableExecutionGraph(ExecutionContext context) {
        this.seedContext = context;
        this.contextTags = new LinkedHashMap<>(context.tags());
        this.nodes = new LinkedHashMap<>();
        this.nodeIdsByType = new LinkedHashMap<>();
        this.edges = new ArrayList<>();
    }

    @Override
    public ExecutionContext context() {
        return new ExecutionContext(
                seedContext.applicationId(),
                seedContext.instanceId(),
                seedContext.executionId(),
                seedContext.traceId(),
                seedContext.spanId(),
                seedContext.parentSpanId(),
                seedContext.entrypointKind(),
                seedContext.transportKind(),
                seedContext.serviceName(),
                seedContext.environment(),
                seedContext.startedAt(),
                contextTags,
                seedContext.activeOverlayIds(),
                seedContext.activePatchIds(),
                seedContext.captureMode()
        );
    }

    @Override
    public String ensureNode(NodeType type, String name, Instant startedAt, Map<String, Object> attributes) {
        return firstNodeId(type).orElseGet(() -> addNode(type, name, NodeStatus.RUNNING, startedAt, null, attributes));
    }

    @Override
    public String addNode(
            NodeType type,
            String name,
            NodeStatus status,
            Instant startedAt,
            Instant endedAt,
            Map<String, Object> attributes
    ) {
        String nodeId = seedContext.executionId() + ":" + typeSegment(type) + ":" + nodes.size();
        NodeState state = new NodeState(nodeId, type, name, status, startedAt, endedAt, attributes);
        nodes.put(nodeId, state);
        nodeIdsByType.computeIfAbsent(type, ignored -> new ArrayList<>()).add(nodeId);
        return nodeId;
    }

    @Override
    public void updateNode(String nodeId, NodeStatus status, Instant endedAt, Map<String, Object> attributes) {
        NodeState current = nodes.get(nodeId);
        if (current == null) {
            return;
        }
        current.status = status;
        current.endedAt = endedAt;
        if (attributes != null) {
            current.attributes.putAll(attributes);
        }
    }

    @Override
    public Optional<String> firstNodeId(NodeType type) {
        List<String> nodeIds = nodeIdsByType.get(type);
        return nodeIds == null || nodeIds.isEmpty() ? Optional.empty() : Optional.of(nodeIds.getFirst());
    }

    @Override
    public void addEdge(String sourceNodeId, String targetNodeId, String relation) {
        edges.add(new ExecutionEdge(sourceNodeId, targetNodeId, relation));
    }

    @Override
    public void putContextTag(String key, String value) {
        if (key != null && value != null) {
            contextTags.put(key, value);
        }
    }

    @Override
    public ExecutionGraph snapshot() {
        List<ExecutionNode> snapshotNodes = nodes.values()
                .stream()
                .map(NodeState::toNode)
                .toList();
        return new ExecutionGraph(context(), snapshotNodes, edges);
    }

    private String typeSegment(NodeType type) {
        String raw = type.value().toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder(raw.length());
        boolean previousWasSeparator = false;
        for (int index = 0; index < raw.length(); index++) {
            char current = raw.charAt(index);
            if (Character.isLetterOrDigit(current)) {
                builder.append(current);
                previousWasSeparator = false;
            }
            else if (!previousWasSeparator) {
                builder.append('_');
                previousWasSeparator = true;
            }
        }
        int end = builder.length();
        while (end > 0 && builder.charAt(end - 1) == '_') {
            end--;
        }
        return end == 0 ? "node" : builder.substring(0, end);
    }

    private static final class NodeState {
        private final String nodeId;
        private final NodeType type;
        private final String name;
        private final Instant startedAt;
        private final Map<String, Object> attributes;
        private NodeStatus status;
        private Instant endedAt;

        private NodeState(
                String nodeId,
                NodeType type,
                String name,
                NodeStatus status,
                Instant startedAt,
                Instant endedAt,
                Map<String, Object> attributes
        ) {
            this.nodeId = nodeId;
            this.type = type;
            this.name = name;
            this.status = status;
            this.startedAt = startedAt;
            this.endedAt = endedAt;
            this.attributes = attributes == null ? new LinkedHashMap<>() : new LinkedHashMap<>(attributes);
        }

        private ExecutionNode toNode() {
            return new ExecutionNode(
                    nodeId,
                    type,
                    name,
                    resolveOriginKind(),
                    resolveSourceRef(),
                    status,
                    startedAt,
                    endedAt,
                    visibleAttributes()
            );
        }

        private ExecutionOriginKind resolveOriginKind() {
            Object value = attributes.get("_originKind");
            return value == null ? ExecutionOriginKind.BASE : ExecutionOriginKind.of(String.valueOf(value));
        }

        private String resolveSourceRef() {
            Object value = attributes.get("_sourceRef");
            return value == null ? null : String.valueOf(value);
        }

        private Map<String, Object> visibleAttributes() {
            Map<String, Object> visible = new LinkedHashMap<>(attributes);
            visible.remove("_originKind");
            visible.remove("_sourceRef");
            return visible;
        }
    }
}
