package io.springlens.agent.contract;

import java.util.Map;
import java.util.Objects;

public record AuditEvent(
        String auditEventId,
        AuditEventType eventType,
        String occurredAt,
        String actor,
        String targetId,
        Map<String, String> metadata
) {

    public AuditEvent {
        auditEventId = requireText(auditEventId, "auditEventId");
        eventType = Objects.requireNonNull(eventType, "eventType");
        occurredAt = requireText(occurredAt, "occurredAt");
        actor = requireText(actor, "actor");
        targetId = requireText(targetId, "targetId");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    private static String requireText(String value, String fieldName) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
