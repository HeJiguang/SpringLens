package io.springlens.agent.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

public record PatchDraftStatus(String value) {

    public static final PatchDraftStatus PENDING = of("PENDING");
    public static final PatchDraftStatus ACCEPTED = of("ACCEPTED");
    public static final PatchDraftStatus DISMISSED = of("DISMISSED");

    public PatchDraftStatus {
        value = normalize(value);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PatchDraftStatus of(String value) {
        return new PatchDraftStatus(value);
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
