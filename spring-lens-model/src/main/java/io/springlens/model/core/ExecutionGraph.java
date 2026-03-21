package io.springlens.model.core;

import java.util.List;

/**
 * 执行图 (Execution Graph)。
 * 它是 Spring Lens 运行时的顶层数据容器，用于表示一次完整业务执行（如单个 HTTP 请求处理过程）的结构化生命周期。
 *
 * @param context 执行图的全局上下文信息（应用、实例、全局追踪ID等）。
 * @param nodes   执行图中包含的所有操作节点（如 Controller处理、SQL执行、探针触发等）。
 * @param edges   图中各节点之间的调用/触发关系连线。
 */
public record ExecutionGraph(
        ExecutionContext context,
        List<ExecutionNode> nodes,
        List<ExecutionEdge> edges
) {

    public ExecutionGraph {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        edges = edges == null ? List.of() : List.copyOf(edges);
    }
}
