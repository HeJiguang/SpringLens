package io.springlens.model.core;

import java.time.Instant;
import java.util.Map;

/**
 * 执行节点 (Execution Node)。
 * 执行图中的单个节点，代表一个被 Spring Lens 观察到的动作或事件。
 *
 * @param nodeId     节点的唯一标识符。
 * @param type       节点的动作类型（如 HTTP请求、SQL执行等）。
 * @param name       节点的名称描述（如具体的 URI 或 方法名）。
 * @param status     节点最终的执行结果状态。
 * @param startedAt  该节点动作开始的时间戳。
 * @param endedAt    该节点动作结束的时间戳。
 * @param attributes 灵活的属性字典，用于存放该节点的特定诊断详情（如完整的 SQL 语句内容、HTTP 详情等）。
 */
public record ExecutionNode(
        String nodeId,
        NodeType type,
        String name,
        NodeStatus status,
        Instant startedAt,
        Instant endedAt,
        Map<String, Object> attributes
) {

    public ExecutionNode {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
