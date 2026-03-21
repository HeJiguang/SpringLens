package io.springlens.server.diagnostic;

import static org.assertj.core.api.Assertions.assertThat;

import io.springlens.model.core.ExecutionContext;
import io.springlens.model.core.ExecutionEdge;
import io.springlens.model.core.ExecutionGraph;
import io.springlens.model.core.ExecutionNode;
import io.springlens.model.core.NodeStatus;
import io.springlens.model.core.NodeType;
import io.springlens.model.diagnostic.DiagnosticResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HeuristicDiagnosticEngineTest {

    private final HeuristicDiagnosticEngine engine = new HeuristicDiagnosticEngine();

    @Test
    void prioritizesExceptionNodesWhenRequestFails() {
        ExecutionGraph graph = new ExecutionGraph(
                new ExecutionContext(
                        "orders-app",
                        "orders-1",
                        "graph-1",
                        Instant.parse("2026-03-20T09:00:00Z"),
                        Map.of("path", "/orders/fail")
                ),
                List.of(
                        new ExecutionNode(
                                "http-1",
                                NodeType.HTTP_REQUEST,
                                "/orders/fail",
                                NodeStatus.FAILURE,
                                Instant.parse("2026-03-20T09:00:00Z"),
                                Instant.parse("2026-03-20T09:00:01Z"),
                                Map.of("status", 500, "durationMs", 1000L)
                        ),
                        new ExecutionNode(
                                "ex-1",
                                NodeType.EXCEPTION,
                                "java.sql.SQLTimeoutException",
                                NodeStatus.FAILURE,
                                Instant.parse("2026-03-20T09:00:00Z"),
                                Instant.parse("2026-03-20T09:00:00Z"),
                                Map.of(
                                        "exceptionClass", "java.sql.SQLTimeoutException",
                                        "message", "Query timed out after 800ms"
                                )
                        )
                ),
                List.of(new ExecutionEdge("http-1", "ex-1", "throws"))
        );

        DiagnosticResult result = engine.analyze(graph);

        assertThat(result.rootCause()).isEqualTo("java.sql.SQLTimeoutException");
        assertThat(result.summary()).contains("/orders/fail");
        assertThat(result.confidence()).isGreaterThanOrEqualTo(0.9);
        assertThat(result.evidence()).anyMatch(item -> item.contains("Query timed out after 800ms"));
        assertThat(result.suggestions()).anyMatch(item -> item.contains("stack trace"));
    }

    @Test
    void flagsSlowSqlWhenNoExceptionIsPresent() {
        ExecutionGraph graph = new ExecutionGraph(
                new ExecutionContext(
                        "orders-app",
                        "orders-1",
                        "graph-2",
                        Instant.parse("2026-03-20T09:05:00Z"),
                        Map.of("path", "/orders/slow")
                ),
                List.of(
                        new ExecutionNode(
                                "http-1",
                                NodeType.HTTP_REQUEST,
                                "/orders/slow",
                                NodeStatus.SUCCESS,
                                Instant.parse("2026-03-20T09:05:00Z"),
                                Instant.parse("2026-03-20T09:05:02Z"),
                                Map.of("status", 200, "durationMs", 2100L)
                        ),
                        new ExecutionNode(
                                "sql-1",
                                NodeType.JDBC_SQL,
                                "slow-sql",
                                NodeStatus.SUCCESS,
                                Instant.parse("2026-03-20T09:05:01Z"),
                                Instant.parse("2026-03-20T09:05:01Z"),
                                Map.of(
                                        "sql", "select * from orders where status = ?",
                                        "durationMs", 1800L
                                )
                        )
                ),
                List.of(new ExecutionEdge("http-1", "sql-1", "executes"))
        );

        DiagnosticResult result = engine.analyze(graph);

        assertThat(result.rootCause()).isEqualTo("Slow database query");
        assertThat(result.summary()).contains("/orders/slow");
        assertThat(result.confidence()).isGreaterThanOrEqualTo(0.8);
        assertThat(result.evidence()).anyMatch(item -> item.contains("1800ms"));
        assertThat(result.suggestions()).anyMatch(item -> item.contains("query plan"));
    }

    @Test
    void returnsLowConfidenceResultWhenNoFailureSignalExists() {
        ExecutionGraph graph = new ExecutionGraph(
                new ExecutionContext(
                        "orders-app",
                        "orders-1",
                        "graph-3",
                        Instant.parse("2026-03-20T09:10:00Z"),
                        Map.of("path", "/orders/1")
                ),
                List.of(
                        new ExecutionNode(
                                "http-1",
                                NodeType.HTTP_REQUEST,
                                "/orders/1",
                                NodeStatus.SUCCESS,
                                Instant.parse("2026-03-20T09:10:00Z"),
                                Instant.parse("2026-03-20T09:10:00Z"),
                                Map.of("status", 200, "durationMs", 42L)
                        )
                ),
                List.of()
        );

        DiagnosticResult result = engine.analyze(graph);

        assertThat(result.rootCause()).isEqualTo("No obvious root cause detected");
        assertThat(result.confidence()).isLessThan(0.5);
        assertThat(result.suggestions()).anyMatch(item -> item.contains("probe"));
    }
}
