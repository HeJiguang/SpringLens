package io.springlens.agent.contract;

import java.util.List;
import java.util.Objects;

public record OverlayDeliverySnapshot(
        String applicationId,
        String instanceId,
        PolicySnapshot policy,
        List<RegisteredOverlay> activeOverlays,
        String deliveredAt
) {

    public OverlayDeliverySnapshot {
        applicationId = requireText(applicationId, "applicationId");
        instanceId = normalizeNullable(instanceId);
        policy = Objects.requireNonNull(policy, "policy");
        activeOverlays = activeOverlays == null ? List.of() : List.copyOf(activeOverlays);
        deliveredAt = requireText(deliveredAt, "deliveredAt");
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
