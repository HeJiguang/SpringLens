package io.springlens.starter.probe;

import io.springlens.model.core.ExecutionGraph;
import io.springlens.model.diagnostic.DiagnosticResult;
import io.springlens.runtime.InMemoryExecutionGraphStore;
import io.springlens.spi.DiagnosticEngine;

public class LensDiagnosticTool {

    private final InMemoryExecutionGraphStore graphStore;
    private final DiagnosticEngine diagnosticEngine;

    public LensDiagnosticTool(InMemoryExecutionGraphStore graphStore, DiagnosticEngine diagnosticEngine) {
        this.graphStore = graphStore;
        this.diagnosticEngine = diagnosticEngine;
    }

    @LensTool(name = "diagnose_request", description = "Analyze a captured request by execution id and return a structured diagnosis.")
    public DiagnosticResult diagnoseRequest(@LensToolParam("executionId") String executionId) {
        if (executionId == null || executionId.isBlank()) {
            throw new IllegalArgumentException("executionId is required");
        }
        ExecutionGraph graph = graphStore.findGraph(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution graph not found: " + executionId));
        return diagnosticEngine.analyze(graph);
    }
}
