package io.springlens.server.diagnostic;

import static org.assertj.core.api.Assertions.assertThat;

import io.springlens.model.core.ExecutionContext;
import io.springlens.model.core.ExecutionGraph;
import io.springlens.model.core.ExecutionNode;
import io.springlens.model.core.NodeStatus;
import io.springlens.model.core.NodeType;
import io.springlens.spi.DefaultDiagnosticEngine;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DefaultDiagnosticEngineTest {

    private final DefaultDiagnosticEngine engine = new DefaultDiagnosticEngine();

    @Test
    void prefersExceptionMessageWhenExceptionNodeExists() {
        ExecutionGraph graph = new ExecutionGraph(
                new ExecutionContext("orders-app", "orders-1", "graph-1", Instant.parse("2026-03-20T10:00:00Z"), Map.of()),
                List.of(
                        new ExecutionNode(
                                "sql-1",
                                NodeType.JDBC_SQL,
                                "slow-sql",
                                NodeStatus.SUCCESS,
                                Instant.parse("2026-03-20T10:00:00Z"),
                                Instant.parse("2026-03-20T10:00:01Z"),
                                Map.of("durationMs", 1200L)
                        ),
                        new ExecutionNode(
                                "ex-1",
                                NodeType.EXCEPTION,
                                "java.lang.IllegalStateException",
                                NodeStatus.FAILURE,
                                Instant.parse("2026-03-20T10:00:00Z"),
                                Instant.parse("2026-03-20T10:00:00Z"),
                                Map.of("message", "Inventory state is inconsistent")
                        )
                ),
                List.of()
        );

        assertThat(engine.analyze(graph).rootCause()).isEqualTo("Inventory state is inconsistent");
        assertThat(engine.analyze(graph).confidence()).isEqualTo(0.9);
    }

    @Test
    void returnsSlowDatabaseQueryWhenSlowSqlExistsWithoutException() {
        ExecutionGraph graph = new ExecutionGraph(
                new ExecutionContext("orders-app", "orders-1", "graph-2", Instant.parse("2026-03-20T10:05:00Z"), Map.of()),
                List.of(
                        new ExecutionNode(
                                "sql-1",
                                NodeType.JDBC_SQL,
                                "slow-sql",
                                NodeStatus.SUCCESS,
                                Instant.parse("2026-03-20T10:05:00Z"),
                                Instant.parse("2026-03-20T10:05:01Z"),
                                Map.of("durationMs", 1400L)
                        )
                ),
                List.of()
        );

        assertThat(engine.analyze(graph).rootCause()).isEqualTo("Slow database query");
        assertThat(engine.analyze(graph).confidence()).isEqualTo(0.7);
    }

    @Test
    void returnsUnknownIssueWhenNoMatchingSignalExists() {
        ExecutionGraph graph = new ExecutionGraph(
                new ExecutionContext("orders-app", "orders-1", "graph-3", Instant.parse("2026-03-20T10:10:00Z"), Map.of()),
                List.of(
                        new ExecutionNode(
                                "http-1",
                                NodeType.HTTP_REQUEST,
                                "/orders/1",
                                NodeStatus.SUCCESS,
                                Instant.parse("2026-03-20T10:10:00Z"),
                                Instant.parse("2026-03-20T10:10:00Z"),
                                Map.of("status", 200)
                        )
                ),
                List.of()
        );

        assertThat(engine.analyze(graph).rootCause()).isEqualTo("Unknown issue");
        assertThat(engine.analyze(graph).confidence()).isEqualTo(0.0);
    }
}
