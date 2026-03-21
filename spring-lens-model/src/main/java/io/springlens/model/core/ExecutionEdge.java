package io.springlens.model.core;

/**
 * 执行边 (Execution Edge)。
 * 用来表示执行图中两节点之间的直接调用或包含关系。
 *
 * @param sourceNodeId 发起调用的父节点 ID。
 * @param targetNodeId 被调用的子节点 ID。
 * @param relation     节点间的关联语义（如 "calls"）。
 */
public record ExecutionEdge(
        String sourceNodeId,
        String targetNodeId,
        String relation
) {
}
