package io.springlens.spi;

import java.util.List;

public interface DiagnosticEngineSelectionStrategy {

    SelectableDiagnosticEngine select(
            DiagnosticSelectionContext context,
            List<SelectableDiagnosticEngine> engines
    );
}
