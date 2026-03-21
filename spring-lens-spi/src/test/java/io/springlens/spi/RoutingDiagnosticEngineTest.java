package io.springlens.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.springlens.model.core.ExecutionContext;
import io.springlens.model.core.ExecutionGraph;
import io.springlens.model.diagnostic.DiagnosticResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RoutingDiagnosticEngineTest {

    @Test
    void delegatesAnalysisToEngineChosenByStrategy() {
        ExecutionGraph graph = new ExecutionGraph(
                new ExecutionContext("orders-app", "orders-1", "graph-3", Instant.parse("2026-03-21T02:05:00Z"), Map.of()),
                List.of(),
                List.of()
        );
        RecordingSelectableDiagnosticEngine defaultEngine = new RecordingSelectableDiagnosticEngine(
                "default",
                10,
                DiagnosticResult.builder().rootCause("default").summary("default").confidence(0.4).build()
        );
        RecordingSelectableDiagnosticEngine heuristicEngine = new RecordingSelectableDiagnosticEngine(
                "heuristic",
                100,
                DiagnosticResult.builder().rootCause("heuristic").summary("heuristic").confidence(0.8).build()
        );
        RoutingDiagnosticEngine routingEngine = new RoutingDiagnosticEngine(
                List.of(defaultEngine, heuristicEngine),
                (context, candidates) -> candidates.stream()
                        .filter(candidate -> candidate.engineId().equals("heuristic"))
                        .findFirst()
                        .orElseThrow()
        );

        DiagnosticResult result = routingEngine.analyze(graph);

        assertEquals("heuristic", result.rootCause());
        assertEquals(0, defaultEngine.invocations);
        assertEquals(1, heuristicEngine.invocations);
    }

    private static final class RecordingSelectableDiagnosticEngine implements SelectableDiagnosticEngine {

        private final String engineId;
        private final int priority;
        private final DiagnosticResult result;
        private int invocations;

        private RecordingSelectableDiagnosticEngine(String engineId, int priority, DiagnosticResult result) {
            this.engineId = engineId;
            this.priority = priority;
            this.result = result;
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
            return true;
        }

        @Override
        public DiagnosticResult analyze(ExecutionGraph graph) {
            invocations++;
            return result;
        }
    }
}
