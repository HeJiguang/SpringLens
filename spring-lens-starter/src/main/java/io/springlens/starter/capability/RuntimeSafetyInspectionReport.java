package io.springlens.starter.capability;

import java.util.List;
import java.util.Objects;

public record RuntimeSafetyInspectionReport(
        String capabilityId,
        int inspectedBeanCount,
        int findingCount,
        List<RuntimeSafetyFinding> findings
) {

    public RuntimeSafetyInspectionReport {
        capabilityId = requireText(capabilityId, "capabilityId");
        inspectedBeanCount = Math.max(0, inspectedBeanCount);
        findings = findings == null ? List.of() : List.copyOf(findings);
        findingCount = findings.size();
    }

    public RuntimeSafetyInspectionReport(String capabilityId, int inspectedBeanCount, List<RuntimeSafetyFinding> findings) {
        this(capabilityId, inspectedBeanCount, findings == null ? 0 : findings.size(), findings);
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
