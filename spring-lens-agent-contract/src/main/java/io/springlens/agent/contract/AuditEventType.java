package io.springlens.agent.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

public record AuditEventType(String value) {

    public static final AuditEventType OVERLAY_APPLIED = of("OVERLAY_APPLIED");
    public static final AuditEventType OVERLAY_APPROVED = of("OVERLAY_APPROVED");
    public static final AuditEventType OVERLAY_DISABLED = of("OVERLAY_DISABLED");
    public static final AuditEventType POLICY_READ = of("POLICY_READ");

    public AuditEventType {
        value = normalize(value);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static AuditEventType of(String value) {
        return new AuditEventType(value);
    }

    @Override
    @JsonValue
    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    private static String normalize(String value) {
        String normalized = Objects.requireNonNull(value, "value").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("value must not be blank");
        }
        return normalized;
    }
}
