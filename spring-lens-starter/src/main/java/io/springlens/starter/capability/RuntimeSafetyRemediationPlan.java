package io.springlens.starter.capability;

import java.util.List;
import java.util.Objects;

public record RuntimeSafetyRemediationPlan(
        String capabilityId,
        int findingCount,
        int overlaySuggestionCount,
        int patchSuggestionCount,
        List<RuntimeSafetyFinding> findings,
        List<RuntimeSafetyOverlaySuggestion> overlaySuggestions,
        List<RuntimeSafetyPatchSuggestion> patchSuggestions
) {

    public RuntimeSafetyRemediationPlan {
        capabilityId = requireText(capabilityId, "capabilityId");
        findings = findings == null ? List.of() : List.copyOf(findings);
        overlaySuggestions = overlaySuggestions == null ? List.of() : List.copyOf(overlaySuggestions);
        patchSuggestions = patchSuggestions == null ? List.of() : List.copyOf(patchSuggestions);
        findingCount = findings.size();
        overlaySuggestionCount = overlaySuggestions.size();
        patchSuggestionCount = patchSuggestions.size();
    }

    public RuntimeSafetyRemediationPlan(
            String capabilityId,
            List<RuntimeSafetyFinding> findings,
            List<RuntimeSafetyOverlaySuggestion> overlaySuggestions,
            List<RuntimeSafetyPatchSuggestion> patchSuggestions
    ) {
        this(
                capabilityId,
                findings == null ? 0 : findings.size(),
                overlaySuggestions == null ? 0 : overlaySuggestions.size(),
                patchSuggestions == null ? 0 : patchSuggestions.size(),
                findings,
                overlaySuggestions,
                patchSuggestions
        );
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
