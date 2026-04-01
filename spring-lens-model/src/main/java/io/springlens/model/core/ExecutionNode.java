package io.springlens.model.core;

import java.time.Instant;
import java.util.Map;

public record ExecutionNode(
        String nodeId,
        NodeType type,
        String name,
        ExecutionOriginKind originKind,
        String sourceRef,
        NodeStatus status,
        Instant startedAt,
        Instant endedAt,
        Map<String, Object> attributes
) {

    public ExecutionNode {
        originKind = originKind == null ? ExecutionOriginKind.BASE : originKind;
        sourceRef = normalizeNullable(sourceRef);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public ExecutionNode(
            String nodeId,
            NodeType type,
            String name,
            NodeStatus status,
            Instant startedAt,
            Instant endedAt,
            Map<String, Object> attributes
    ) {
        this(nodeId, type, name, ExecutionOriginKind.BASE, null, status, startedAt, endedAt, attributes);
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
