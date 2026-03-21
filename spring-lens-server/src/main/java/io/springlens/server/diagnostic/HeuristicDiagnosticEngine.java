package io.springlens.server.diagnostic;

import io.springlens.model.core.ExecutionGraph;
import io.springlens.model.core.ExecutionNode;
import io.springlens.model.core.NodeStatus;
import io.springlens.model.core.NodeType;
import io.springlens.model.diagnostic.DiagnosticResult;
import io.springlens.spi.DiagnosticSelectionContext;
import io.springlens.spi.SelectableDiagnosticEngine;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class HeuristicDiagnosticEngine implements SelectableDiagnosticEngine {

    @Override
    public String engineId() {
        return "heuristic-rule-based";
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public boolean supports(DiagnosticSelectionContext context) {
        return !context.graph().nodes().isEmpty();
    }

    @Override
    public DiagnosticResult analyze(ExecutionGraph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("graph is required");
        }

        Optional<ExecutionNode> httpNode = graph.nodes().stream()
                .filter(node -> NodeType.HTTP_REQUEST.equals(node.type()))
                .min(Comparator.comparing(ExecutionNode::startedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        Optional<ExecutionNode> exceptionNode = graph.nodes().stream()
                .filter(node -> NodeType.EXCEPTION.equals(node.type()))
                .max(Comparator.comparing(this::nodeInstant, Comparator.nullsLast(Comparator.naturalOrder())));
        Optional<ExecutionNode> slowSqlNode = graph.nodes().stream()
                .filter(node -> NodeType.JDBC_SQL.equals(node.type()))
                .max(Comparator.comparingLong(this::durationMs));

        String requestPath = requestPath(graph, httpNode);

        if (exceptionNode.isPresent()) {
            return diagnoseException(requestPath, httpNode, exceptionNode.get(), slowSqlNode.orElse(null));
        }
        if (slowSqlNode.isPresent()) {
            return diagnoseSlowSql(requestPath, httpNode, slowSqlNode.get());
        }
        if (httpNode.isPresent() && NodeStatus.FAILURE.equals(httpNode.get().status())) {
            return diagnoseHttpFailure(requestPath, httpNode.get());
        }
        return DiagnosticResult.builder()
                .rootCause("No obvious root cause detected")
                .summary("Execution graph" + locationSuffix(requestPath) + " completed without an exception or slow SQL signal.")
                .confidence(0.25)
                .addEvidence("No EXCEPTION or JDBC_SQL nodes stood out in graph " + graph.context().executionId() + ".")
                .addSuggestion("Inspect probe values and neighboring execution graphs for more context.")
                .addSuggestion("Add domain-specific probes around suspected branches before drawing conclusions.")
                .build();
    }

    private DiagnosticResult diagnoseException(
            String requestPath,
            Optional<ExecutionNode> httpNode,
            ExecutionNode exceptionNode,
            ExecutionNode slowSqlNode
    ) {
        String exceptionClass = stringAttribute(exceptionNode, "exceptionClass")
                .orElseGet(() -> exceptionNode.name() == null || exceptionNode.name().isBlank() ? "Unknown exception" : exceptionNode.name());
        DiagnosticResult.Builder builder = DiagnosticResult.builder()
                .rootCause(exceptionClass)
                .summary("Execution graph" + locationSuffix(requestPath) + " failed with " + exceptionClass + ".")
                .confidence(slowSqlNode == null ? 0.92 : 0.95)
                .addEvidence("Exception node " + exceptionNode.nodeId() + " captured " + exceptionClass + ".");

        stringAttribute(exceptionNode, "message")
                .ifPresent(message -> builder.addEvidence("Exception message: " + message));
        httpNode.flatMap(node -> stringAttribute(node, "status"))
                .ifPresent(status -> builder.addEvidence("HTTP status at failure time was " + status + "."));

        if (slowSqlNode != null) {
            builder.addEvidence("A JDBC node also took " + durationMs(slowSqlNode) + "ms before the failure.");
            builder.addSuggestion("Inspect the stack trace together with the slow SQL path before only increasing timeouts.");
            builder.addSuggestion("Review the query plan and index coverage for the database call on the failing path.");
        }
        else {
            builder.addSuggestion("Inspect the stack trace and the failing component around " + exceptionClass + ".");
            builder.addSuggestion("Correlate this exception with probe values captured just before the failure.");
        }
        return builder.build();
    }

    private DiagnosticResult diagnoseSlowSql(String requestPath, Optional<ExecutionNode> httpNode, ExecutionNode slowSqlNode) {
        long durationMs = durationMs(slowSqlNode);
        DiagnosticResult.Builder builder = DiagnosticResult.builder()
                .rootCause("Slow database query")
                .summary("Execution graph" + locationSuffix(requestPath) + " is dominated by a slow database query.")
                .confidence(durationMs >= 1000 ? 0.86 : 0.8)
                .addEvidence("JDBC node " + slowSqlNode.nodeId() + " took " + durationMs + "ms.")
                .addSuggestion("Inspect the query plan and index coverage for the slow SQL.")
                .addSuggestion("Compare this graph with recent probe values to see whether the query fan-out is expected.");

        stringAttribute(slowSqlNode, "sql")
                .ifPresent(sql -> builder.addEvidence("SQL snippet: " + abbreviate(sql, 160)));
        httpNode.flatMap(node -> stringAttribute(node, "durationMs"))
                .ifPresent(duration -> builder.addEvidence("End-to-end HTTP duration was " + duration + "ms."));
        return builder.build();
    }

    private DiagnosticResult diagnoseHttpFailure(String requestPath, ExecutionNode httpNode) {
        String status = stringAttribute(httpNode, "status").orElse("unknown");
        return DiagnosticResult.builder()
                .rootCause("HTTP request failure")
                .summary("Execution graph" + locationSuffix(requestPath) + " ended with HTTP status " + status + ".")
                .confidence(0.6)
                .addEvidence("HTTP node " + httpNode.nodeId() + " finished with status " + status + ".")
                .addSuggestion("Inspect application-level error handling around the failing request path.")
                .addSuggestion("Add probes on the failing branch to capture the last successful state transition.")
                .build();
    }

    private String requestPath(ExecutionGraph graph, Optional<ExecutionNode> httpNode) {
        if (httpNode.isPresent() && httpNode.get().name() != null && !httpNode.get().name().isBlank()) {
            return httpNode.get().name();
        }
        String tagPath = graph.context().tags().get("path");
        return tagPath == null || tagPath.isBlank() ? null : tagPath;
    }

    private long durationMs(ExecutionNode node) {
        Object rawValue = node.attributes().get("durationMs");
        if (rawValue instanceof Number number) {
            return number.longValue();
        }
        if (node.startedAt() != null && node.endedAt() != null) {
            return Math.max(0L, node.endedAt().toEpochMilli() - node.startedAt().toEpochMilli());
        }
        return 0L;
    }

    private Instant nodeInstant(ExecutionNode node) {
        return node.endedAt() != null ? node.endedAt() : node.startedAt();
    }

    private Optional<String> stringAttribute(ExecutionNode node, String key) {
        Object value = node.attributes().get(key);
        if (value == null) {
            return Optional.empty();
        }
        String stringValue = String.valueOf(value);
        return stringValue.isBlank() ? Optional.empty() : Optional.of(stringValue);
    }

    private String locationSuffix(String requestPath) {
        return requestPath == null ? "" : " for " + requestPath;
    }

    private String abbreviate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }
}
