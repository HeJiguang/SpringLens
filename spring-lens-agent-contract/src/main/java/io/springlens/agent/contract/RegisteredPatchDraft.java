package io.springlens.agent.contract;

import java.util.Map;
import java.util.Objects;

public record RegisteredPatchDraft(
        String draftId,
        PatchProposalDraft draft,
        PatchDraftStatus status,
        String submittedAt,
        String resolvedAt,
        Map<String, String> metadata
) {

    public RegisteredPatchDraft {
        draftId = requireText(draftId, "draftId");
        draft = Objects.requireNonNull(draft, "draft");
        status = Objects.requireNonNull(status, "status");
        submittedAt = requireText(submittedAt, "submittedAt");
        resolvedAt = normalizeNullable(resolvedAt);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
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
