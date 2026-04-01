package io.springlens.agent.contract;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record RuntimeSafetyPromotionResult(
        String applicationId,
        String instanceId,
        String capabilityId,
        int overlayRegistrationCount,
        int patchDraftRegistrationCount,
        List<RegisteredOverlay> overlayRegistrations,
        List<RegisteredPatchDraft> patchDraftRegistrations,
        Map<String, String> metadata
) {

    public RuntimeSafetyPromotionResult {
        applicationId = requireText(applicationId, "applicationId");
        instanceId = normalizeNullable(instanceId);
        capabilityId = requireText(capabilityId, "capabilityId");
        overlayRegistrations = overlayRegistrations == null ? List.of() : List.copyOf(overlayRegistrations);
        patchDraftRegistrations = patchDraftRegistrations == null ? List.of() : List.copyOf(patchDraftRegistrations);
        overlayRegistrationCount = overlayRegistrations.size();
        patchDraftRegistrationCount = patchDraftRegistrations.size();
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public RuntimeSafetyPromotionResult(
            String applicationId,
            String instanceId,
            String capabilityId,
            List<RegisteredOverlay> overlayRegistrations,
            List<RegisteredPatchDraft> patchDraftRegistrations,
            Map<String, String> metadata
    ) {
        this(
                applicationId,
                instanceId,
                capabilityId,
                overlayRegistrations == null ? 0 : overlayRegistrations.size(),
                patchDraftRegistrations == null ? 0 : patchDraftRegistrations.size(),
                overlayRegistrations,
                patchDraftRegistrations,
                metadata
        );
    }

    private static String requireText(String value, String fieldName) {
        String normalized = normalizeNullable(Objects.requireNonNull(value, fieldName));
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
