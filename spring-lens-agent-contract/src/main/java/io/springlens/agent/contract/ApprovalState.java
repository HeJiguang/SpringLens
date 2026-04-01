package io.springlens.agent.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

public record ApprovalState(String value) {

    public static final ApprovalState PENDING = of("PENDING");
    public static final ApprovalState APPROVED = of("APPROVED");
    public static final ApprovalState REJECTED = of("REJECTED");
    public static final ApprovalState DISABLED = of("DISABLED");

    public ApprovalState {
        value = normalize(value);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ApprovalState of(String value) {
        return new ApprovalState(value);
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
