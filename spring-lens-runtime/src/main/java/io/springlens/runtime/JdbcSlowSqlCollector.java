package io.springlens.runtime;

import io.springlens.model.core.NodeStatus;
import io.springlens.model.core.NodeType;
import io.springlens.spi.GraphMutation;
import io.springlens.spi.RuntimeCollector;
import io.springlens.spi.RuntimeSignal;
import io.springlens.spi.RuntimeSignalType;
import java.util.Set;

/**
 * JDBC 慢 SQL 收集器 (JDBC Slow SQL Collector)。
 * 专门监听数据库访问层的信号。
 * 为避免内存被正常的极速简单 SQL 撑爆，它只提取标记为 "slow=true" 的语句。
 * （普通的高频 SQL 不会在图中展现，通常只会输出 Metrics，只有慢查询才会变成节点附加在请求图中以便后续排错）
 */
public final class JdbcSlowSqlCollector implements RuntimeCollector {

    @Override
    public String id() {
        return "jdbc-slow-sql";
    }

    @Override
    public Set<RuntimeSignalType> supportedTypes() {
        return Set.of(RuntimeSignalType.JDBC_EXECUTED);
    }

    @Override
    public void collect(RuntimeSignal signal, GraphMutation graph) {
        // 如果信号里没有把这句标为 slow，直接丢弃，不在请求追踪图里展现。
        if (!Boolean.TRUE.equals(signal.attributes().get("slow"))) {
            return;
        }
        
        String sqlNodeId = graph.addNode(
                NodeType.JDBC_SQL,
                "slow-sql",
                NodeStatus.SUCCESS,
                signal.occurredAt(),
                signal.occurredAt(),
                signal.attributes()
        );
        
        // 挂在请求入口父节点上
        graph.firstNodeId(NodeType.HTTP_REQUEST)
                .ifPresent(requestNodeId -> graph.addEdge(requestNodeId, sqlNodeId, "executes"));
    }
}
