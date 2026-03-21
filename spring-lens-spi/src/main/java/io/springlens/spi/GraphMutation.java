package io.springlens.spi;

import io.springlens.model.core.ExecutionContext;
import io.springlens.model.core.ExecutionGraph;
import io.springlens.model.core.NodeStatus;
import io.springlens.model.core.NodeType;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * 执行图修改器 (Graph Mutation)。
 * 它提供了一组安全的 API（SPI），允许外部插件或 Collector 在运行时修改和组装当前上下文中的执行图，而不用暴露底层存储。
 */
public interface GraphMutation {

    /**
     * @return 当前执行图的全局上下文信息。
     */
    ExecutionContext context();

    /**
     * 往图里追加一个自定义上下文标签。
     * @param key 键
     * @param value 值
     */
    void putContextTag(String key, String value);

    /**
     * 确保图中存在该节点，如果不存在则创建并开启它（RUNNING 状态），如果已存在则直接返回 ID。
     * @return 节点的唯一 ID。
     */
    String ensureNode(NodeType type, String name, Instant startedAt, Map<String, Object> attributes);

    /**
     * 强制向图中添加一个全新节点（无论是否已有同类节点）。
     * @return 添加后的节点 ID。
     */
    String addNode(
            NodeType type,
            String name,
            NodeStatus status,
            Instant startedAt,
            Instant endedAt,
            Map<String, Object> attributes
    );

    /**
     * 更新一个已经存在的节点状态和结束时间等信息（比如 HTTP 请求结束时或 SQL 执行完毕时调用此方法）。
     */
    void updateNode(String nodeId, NodeStatus status, Instant endedAt, Map<String, Object> attributes);

    /**
     * 查询图中是否已经存在这种类型的节点，返回匹配到的第一个节点的 ID。
     */
    Optional<String> firstNodeId(NodeType type);

    /**
     * 在两个节点间连一条边，建立调用关系。
     */
    void addEdge(String sourceNodeId, String targetNodeId, String relation);

    /**
     * @return 捕获当前内存图的快照并返回数据模型 DTO。
     */
    ExecutionGraph snapshot();
}
