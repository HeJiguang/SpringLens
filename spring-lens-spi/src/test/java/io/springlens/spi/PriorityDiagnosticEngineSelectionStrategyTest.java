package io.springlens.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.springlens.model.core.ExecutionContext;
import io.springlens.model.core.ExecutionGraph;
import io.springlens.model.diagnostic.DiagnosticResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PriorityDiagnosticEngineSelectionStrategyTest {

    @Test
    void selectsHighestPrioritySupportedEngine() {
        DiagnosticSelectionContext context = new DiagnosticSelectionContext(graph("graph-1"));
        PriorityDiagnosticEngineSelectionStrategy strategy = new PriorityDiagnosticEngineSelectionStrategy();

        SelectableDiagnosticEngine selected = strategy.select(
                context,
                List.of(
                        new TestSelectableDiagnosticEngine("rule", 10, true),
                        new TestSelectableDiagnosticEngine("ai", 100, true)
                )
        );

        assertEquals("ai", selected.engineId());
    }

    @Test
    void rejectsSelectionWhenNoCandidateSupportsGraph() {
        DiagnosticSelectionContext context = new DiagnosticSelectionContext(graph("graph-2"));
        PriorityDiagnosticEngineSelectionStrategy strategy = new PriorityDiagnosticEngineSelectionStrategy();

        assertThrows(IllegalStateException.class, () -> strategy.select(
                context,
                List.of(
                        new TestSelectableDiagnosticEngine("rule", 10, false),
                        new TestSelectableDiagnosticEngine("ai", 100, false)
                )
        ));
    }

    private ExecutionGraph graph(String executionId) {
        return new ExecutionGraph(
                new ExecutionContext("orders-app", "orders-1", executionId, Instant.parse("2026-03-21T02:00:00Z"), Map.of()),
                List.of(),
                List.of()
        );
    }

    private static final class TestSelectableDiagnosticEngine implements SelectableDiagnosticEngine {

        private final String engineId;
        private final int priority;
        private final boolean supported;

        private TestSelectableDiagnosticEngine(String engineId, int priority, boolean supported) {
            this.engineId = engineId;
            this.priority = priority;
            this.supported = supported;
        }

        @Override
        public String engineId() {
            return engineId;
        }

        @Override
        public int priority() {
            return priority;
        }

        @Override
        public boolean supports(DiagnosticSelectionContext context) {
            return supported;
        }

        @Override
        public DiagnosticResult analyze(ExecutionGraph graph) {
            return DiagnosticResult.builder()
                    .rootCause(engineId)
                    .summary(engineId)
                    .confidence(0.5)
                    .build();
        }
    }
}
