package io.springlens.spi;

import io.springlens.model.core.ExecutionGraph;
import io.springlens.model.core.ExecutionNode;
import io.springlens.model.core.NodeType;
import io.springlens.model.diagnostic.DiagnosticResult;

public class DefaultDiagnosticEngine implements SelectableDiagnosticEngine {

    @Override
    public String engineId() {
        return "default-rule-based";
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public DiagnosticResult analyze(ExecutionGraph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("graph is required");
        }

        for (ExecutionNode node : graph.nodes()) {
            if (NodeType.EXCEPTION.equals(node.type())) {
                String message = String.valueOf(node.attributes().getOrDefault("message", node.name()));
                return DiagnosticResult.builder()
                        .rootCause(message)
                        .summary("Detected exception node in execution graph")
                        .confidence(0.9)
                        .build();
            }
        }

        for (ExecutionNode node : graph.nodes()) {
            if (NodeType.JDBC_SQL.equals(node.type())) {
                return DiagnosticResult.builder()
                        .rootCause("Slow database query")
                        .summary("Detected JDBC SQL node in execution graph")
                        .confidence(0.7)
                        .build();
            }
        }

        return DiagnosticResult.builder()
                .rootCause("Unknown issue")
                .summary("No exception or slow SQL signal found in execution graph")
                .confidence(0.0)
                .build();
    }
}
