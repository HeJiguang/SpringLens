package io.springlens.agent.contract;

import java.util.Map;
import java.util.Objects;

public record RegisteredOverlay(
        String overlayId,
        OverlaySpec spec,
        ApprovalState approvalState,
        String createdAt,
        String disabledAt,
        Map<String, String> metadata
) {

    public RegisteredOverlay {
        overlayId = requireText(overlayId, "overlayId");
        spec = Objects.requireNonNull(spec, "spec");
        approvalState = Objects.requireNonNull(approvalState, "approvalState");
        createdAt = requireText(createdAt, "createdAt");
        disabledAt = normalizeNullable(disabledAt);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    private static String requireText(String value, String fieldName) {
        String normalized = normalizeNullable(value);
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
