package io.springlens.model.diagnostic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiagnosticResultTest {

    @Test
    void constructorCopiesCollectionsToKeepModelImmutable() {
        List<String> evidence = new ArrayList<>(List.of("slow sql"));
        List<String> suggestions = new ArrayList<>(List.of("add index"));

        DiagnosticResult result = new DiagnosticResult(
                "Missing database index",
                "The request spent most of its time in a slow SQL node.",
                0.82,
                evidence,
                suggestions
        );

        evidence.add("late mutation");
        suggestions.add("another mutation");

        assertEquals(List.of("slow sql"), result.evidence());
        assertEquals(List.of("add index"), result.suggestions());
        assertThrows(UnsupportedOperationException.class, () -> result.evidence().add("x"));
        assertThrows(UnsupportedOperationException.class, () -> result.suggestions().add("x"));
    }

    @Test
    void builderBuildsImmutableResult() {
        DiagnosticResult result = DiagnosticResult.builder()
                .rootCause("Downstream timeout")
                .summary("A downstream dependency caused the request to fail.")
                .confidence(0.91)
                .addEvidence("http client node exceeded timeout")
                .addEvidence("exception node recorded ConnectException")
                .addSuggestion("inspect downstream service health")
                .addSuggestion("increase timeout only after confirming saturation")
                .build();

        assertEquals("Downstream timeout", result.rootCause());
        assertEquals("A downstream dependency caused the request to fail.", result.summary());
        assertEquals(0.91, result.confidence());
        assertEquals(2, result.evidence().size());
        assertEquals(2, result.suggestions().size());
    }

    @Test
    void confidenceMustBeWithinZeroAndOne() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new DiagnosticResult(
                "Root cause",
                "Summary",
                1.2,
                List.of("evidence"),
                List.of("suggestion")
        ));

        assertTrue(exception.getMessage().contains("confidence"));
    }
}
