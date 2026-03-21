package io.springlens.spi;

import io.springlens.model.core.ExecutionGraph;
import io.springlens.model.diagnostic.DiagnosticResult;

public interface SelectableDiagnosticEngine {

    String engineId();

    default int priority() {
        return 0;
    }

    default boolean supports(DiagnosticSelectionContext context) {
        return true;
    }

    DiagnosticResult analyze(ExecutionGraph graph);
}
