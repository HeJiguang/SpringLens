package io.springlens.spi;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class PriorityDiagnosticEngineSelectionStrategy implements DiagnosticEngineSelectionStrategy {

    private static final Comparator<SelectableDiagnosticEngine> PRIORITY_ORDER =
            Comparator.comparingInt(SelectableDiagnosticEngine::priority)
                    .thenComparing(SelectableDiagnosticEngine::engineId);

    @Override
    public SelectableDiagnosticEngine select(
            DiagnosticSelectionContext context,
            List<SelectableDiagnosticEngine> engines
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(engines, "engines");

        return engines.stream()
                .filter(engine -> engine.supports(context))
                .max(PRIORITY_ORDER)
                .orElseThrow(() -> new IllegalStateException(
                        "No diagnostic engine supports execution graph " + context.graph().context().executionId()
                ));
    }
}
