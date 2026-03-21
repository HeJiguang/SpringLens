package io.springlens.runtime;

import io.springlens.model.diagnostic.ExceptionContextRecord;
import io.springlens.model.core.ExecutionContext;
import io.springlens.model.core.ExecutionGraph;
import io.springlens.model.core.ExecutionNode;
import io.springlens.model.core.NodeType;
import io.springlens.model.diagnostic.ProbeCapturePhase;
import io.springlens.model.diagnostic.ProbeValueRecord;
import io.springlens.model.diagnostic.SlowSqlRecord;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存执行图存储库 (In-Memory Execution Graph Store)。
 * 这是微服务内嵌的核心数据据库（Ring Buffer 环形队列），记录了请求在运行时的状态和历史痕迹。
 * 它同时维护了“正在活跃（执行中）的图”以及“最近刚执行完毕的图的只读快照”。
 * 也是 Server 端进行数据拉取 (Pull) 和聚合查询的地方。
 */
public final class InMemoryExecutionGraphStore {

    /** 正在发生的、还没完结的执行图集合，用 executionId 分组。这里面的图还在被各个 Collector 修改着 */
    private final Map<String, MutableExecutionGraph> activeGraphs;

    /** 已经归档封板完成的只读快照，是一个环形双端队列，存放最近 N 条记录 */
    private final Deque<ExecutionGraph> completedGraphs;
    
    /** 环形队列最大容量 */
    private final int maxCompletedGraphs;

    public InMemoryExecutionGraphStore(int maxCompletedGraphs) {
        this.activeGraphs = new ConcurrentHashMap<>();
        this.completedGraphs = new ArrayDeque<>();
        this.maxCompletedGraphs = maxCompletedGraphs;
    }

    /** 开始记录一条全新的请求链路图 */
    public void start(ExecutionContext context) {
        activeGraphs.put(context.executionId(), new MutableExecutionGraph(context));
    }

    /** 拿着请求 ID 获取还在修改中的那张可变图 */
    public Optional<MutableExecutionGraph> activeGraph(String executionId) {
        return Optional.ofNullable(activeGraphs.get(executionId));
    }

    /**
     * 当一个完整请求处理完毕（例如 HTTP 返回 200 了），封板图数据。
     * 从活跃区挪动到已完成区保存快照，防止内存泄露。
     */
    public void complete(String executionId) {
        MutableExecutionGraph graph = activeGraphs.remove(executionId);
        if (graph == null) {
            return;
        }
        synchronized (completedGraphs) {
            completedGraphs.addLast(graph.snapshot());
            // 环形缓冲：满了就像传送带一样把最旧的设计扔掉
            while (completedGraphs.size() > maxCompletedGraphs) {
                completedGraphs.removeFirst();
            }
        }
    }

    /** 通过执行 ID 从活跃区或已完成区找寻对应图的快照 */
    public Optional<ExecutionGraph> findGraph(String executionId) {
        MutableExecutionGraph active = activeGraphs.get(executionId);
        if (active != null) {
            return Optional.of(active.snapshot());
        }
        synchronized (completedGraphs) {
            return completedGraphs.stream()
                    .filter(graph -> Objects.equals(graph.context().executionId(), executionId))
                    .findFirst();
        }
    }

    /**
     * 【诊断查询】从最近完结的请求中，筛选并聚合出所有的慢 SQL 记录。
     */
    public List<SlowSqlRecord> findSlowSql(String applicationId, int limit, long minDurationMs) {
        return snapshots(applicationId).stream()
                .flatMap(graph -> graph.nodes().stream()
                        .filter(node -> NodeType.JDBC_SQL.equals(node.type()))
                        .map(node -> toSlowSql(graph, node))
                        .filter(Objects::nonNull))
                .filter(record -> record.durationMs() >= minDurationMs)
                .sorted(Comparator.comparing(SlowSqlRecord::occurredAt).reversed())
                .limit(limit)
                .toList();
    }

    /**
     * 【诊断查询】从最近完结的请求中，筛选并聚合出被拦截器记录下的所有异常上下文（含堆栈）。
     */
    public List<ExceptionContextRecord> findExceptionContexts(String applicationId, int limit, String exceptionClass) {
        return snapshots(applicationId).stream()
                .flatMap(graph -> graph.nodes().stream()
                        .filter(node -> NodeType.EXCEPTION.equals(node.type()))
                        .map(node -> toExceptionContext(graph, node))
                        .filter(Objects::nonNull))
                .filter(record -> exceptionClass == null || exceptionClass.isBlank() || exceptionClass.equals(record.exceptionClass()))
                .sorted(Comparator.comparing(ExceptionContextRecord::occurredAt).reversed())
                .limit(limit)
                .toList();
    }

    /**
     * 【诊断查询】从最近完结的请求中，筛选并聚合出所有的自定义探针 (@LensWatch) 记录的变量值。
     */
    public List<ProbeValueRecord> findProbeValues(String applicationId, String probeId, int limit) {
        return snapshots(applicationId).stream()
                .flatMap(graph -> graph.nodes().stream()
                        .filter(node -> NodeType.WATCH_VALUE.equals(node.type()))
                        .map(node -> toProbeValue(graph, node))
                        .filter(Objects::nonNull))
                .filter(record -> probeId == null || probeId.isBlank() || probeId.equals(record.probeId()))
                .sorted(Comparator.comparing(ProbeValueRecord::occurredAt).reversed())
                .limit(limit)
                .toList();
    }

    private List<ExecutionGraph> snapshots(String applicationId) {
        synchronized (completedGraphs) {
            List<ExecutionGraph> graphs = new ArrayList<>(completedGraphs);
            return graphs.stream()
                    .filter(graph -> graph.context().applicationId().equals(applicationId))
                    .toList();
        }
    }

    private SlowSqlRecord toSlowSql(ExecutionGraph graph, ExecutionNode node) {
        Object sql = node.attributes().get("sql");
        Object duration = node.attributes().get("durationMs");
        if (!(sql instanceof String sqlText) || !(duration instanceof Number durationMs)) {
            return null;
        }
        return new SlowSqlRecord(
                graph.context().executionId(),
                node.nodeId(),
                sqlText,
                durationMs.longValue(),
                graph.context().tags().getOrDefault("path", "unknown"),
                node.startedAt() == null ? Instant.EPOCH : node.startedAt()
        );
    }

    @SuppressWarnings("unchecked")
    private ExceptionContextRecord toExceptionContext(ExecutionGraph graph, ExecutionNode node) {
        Object exceptionClass = node.attributes().get("exceptionClass");
        if (!(exceptionClass instanceof String exceptionType)) {
            return null;
        }
        Object stackTrace = node.attributes().getOrDefault("stackTrace", List.of());
        List<String> stackTraceLines = stackTrace instanceof List<?> values
                ? values.stream().map(String::valueOf).toList()
                : List.of(String.valueOf(stackTrace));
        return new ExceptionContextRecord(
                graph.context().executionId(),
                node.nodeId(),
                exceptionType,
                String.valueOf(node.attributes().getOrDefault("message", "")),
                graph.context().tags().getOrDefault("path", "unknown"),
                node.startedAt() == null ? Instant.EPOCH : node.startedAt(),
                stackTraceLines
        );
    }

    private ProbeValueRecord toProbeValue(ExecutionGraph graph, ExecutionNode node) {
        Object probeId = node.attributes().get("probeId");
        if (!(probeId instanceof String probeIdentifier)) {
            return null;
        }
        Object phase = node.attributes().getOrDefault("capturePhase", ProbeCapturePhase.MANUAL.value());
        return new ProbeValueRecord(
                graph.context().executionId(),
                node.nodeId(),
                probeIdentifier,
                String.valueOf(node.attributes().getOrDefault("description", "")),
                node.attributes().get("value"),
                String.valueOf(node.attributes().getOrDefault("valueType", "java.lang.Object")),
                graph.context().tags().getOrDefault("path", "unknown"),
                node.startedAt() == null ? Instant.EPOCH : node.startedAt(),
                String.valueOf(node.attributes().getOrDefault("captureSource", "manual")),
                ProbeCapturePhase.of(String.valueOf(phase))
        );
    }
}
