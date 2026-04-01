package io.springlens.agent.contract;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record RuntimeSafetyDraftBundle(
        String applicationId,
        String instanceId,
        String capabilityId,
        int overlayDraftCount,
        int patchDraftCount,
        List<OverlaySpec> overlayDrafts,
        List<PatchProposalDraft> patchDrafts,
        Map<String, String> metadata
) {

    public RuntimeSafetyDraftBundle {
        applicationId = requireText(applicationId, "applicationId");
        instanceId = normalizeNullable(instanceId);
        capabilityId = requireText(capabilityId, "capabilityId");
        overlayDrafts = overlayDrafts == null ? List.of() : List.copyOf(overlayDrafts);
        patchDrafts = patchDrafts == null ? List.of() : List.copyOf(patchDrafts);
        overlayDraftCount = overlayDrafts.size();
        patchDraftCount = patchDrafts.size();
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public RuntimeSafetyDraftBundle(
            String applicationId,
            String instanceId,
            String capabilityId,
            List<OverlaySpec> overlayDrafts,
            List<PatchProposalDraft> patchDrafts,
            Map<String, String> metadata
    ) {
        this(
                applicationId,
                instanceId,
                capabilityId,
                overlayDrafts == null ? 0 : overlayDrafts.size(),
                patchDrafts == null ? 0 : patchDrafts.size(),
                overlayDrafts,
                patchDrafts,
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
