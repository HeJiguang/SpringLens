package io.springlens.model.core;

public record ExecutionEdge(
        String sourceNodeId,
        String targetNodeId,
        String relation,
        ExecutionOriginKind originKind
) {

    public ExecutionEdge {
        sourceNodeId = requireText(sourceNodeId, "sourceNodeId");
        targetNodeId = requireText(targetNodeId, "targetNodeId");
        relation = requireText(relation, "relation");
        originKind = originKind == null ? ExecutionOriginKind.BASE : originKind;
    }

    public ExecutionEdge(String sourceNodeId, String targetNodeId, String relation) {
        this(sourceNodeId, targetNodeId, relation, ExecutionOriginKind.BASE);
    }

    private static String requireText(String value, String fieldName) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
