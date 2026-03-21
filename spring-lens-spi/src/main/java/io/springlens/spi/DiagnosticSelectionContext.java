package io.springlens.spi;

import io.springlens.model.core.ExecutionGraph;
import java.util.Objects;

public record DiagnosticSelectionContext(ExecutionGraph graph) {

    public DiagnosticSelectionContext {
        Objects.requireNonNull(graph, "graph");
    }
}
