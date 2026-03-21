package io.springlens.model.diagnostic;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured output from an execution-graph diagnosis.
 *
 * @param rootCause the most likely root cause inferred from the graph
 * @param summary a short diagnosis summary for humans or AI agents
 * @param confidence confidence score between 0.0 and 1.0
 * @param evidence supporting evidence extracted from the analysis
 * @param suggestions follow-up actions suggested by the diagnosis
 */
public record DiagnosticResult(
        String rootCause,
        String summary,
        double confidence,
        List<String> evidence,
        List<String> suggestions
) {

    public DiagnosticResult {
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be between 0.0 and 1.0");
        }
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String rootCause;
        private String summary;
        private double confidence;
        private final List<String> evidence = new ArrayList<>();
        private final List<String> suggestions = new ArrayList<>();

        private Builder() {
        }

        public Builder rootCause(String rootCause) {
            this.rootCause = rootCause;
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }

        public Builder evidence(List<String> evidence) {
            this.evidence.clear();
            if (evidence != null) {
                this.evidence.addAll(evidence);
            }
            return this;
        }

        public Builder addEvidence(String evidenceItem) {
            if (evidenceItem != null) {
                this.evidence.add(evidenceItem);
            }
            return this;
        }

        public Builder suggestions(List<String> suggestions) {
            this.suggestions.clear();
            if (suggestions != null) {
                this.suggestions.addAll(suggestions);
            }
            return this;
        }

        public Builder addSuggestion(String suggestion) {
            if (suggestion != null) {
                this.suggestions.add(suggestion);
            }
            return this;
        }

        public DiagnosticResult build() {
            return new DiagnosticResult(rootCause, summary, confidence, evidence, suggestions);
        }
    }
}
