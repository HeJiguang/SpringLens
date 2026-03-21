package io.springlens.spi;

import io.springlens.model.core.ExecutionGraph;
import io.springlens.model.diagnostic.DiagnosticResult;
import java.util.List;
import java.util.Objects;

public class RoutingDiagnosticEngine implements DiagnosticEngine {

    private final List<SelectableDiagnosticEngine> engines;
    private final DiagnosticEngineSelectionStrategy selectionStrategy;

    public RoutingDiagnosticEngine(
            List<SelectableDiagnosticEngine> engines,
            DiagnosticEngineSelectionStrategy selectionStrategy
    ) {
        this.engines = List.copyOf(Objects.requireNonNull(engines, "engines"));
        this.selectionStrategy = Objects.requireNonNull(selectionStrategy, "selectionStrategy");
        if (this.engines.isEmpty()) {
            throw new IllegalArgumentException("At least one diagnostic engine is required");
        }
    }

    @Override
    public DiagnosticResult analyze(ExecutionGraph graph) {
        DiagnosticSelectionContext context = new DiagnosticSelectionContext(graph);
        SelectableDiagnosticEngine engine = selectionStrategy.select(context, engines);
        return engine.analyze(graph);
    }
}
