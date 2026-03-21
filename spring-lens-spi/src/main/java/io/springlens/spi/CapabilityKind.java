package io.springlens.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

public record CapabilityKind(String value) {

    public static final CapabilityKind OBSERVABILITY = of("OBSERVABILITY");
    public static final CapabilityKind DIAGNOSIS = of("DIAGNOSIS");
    public static final CapabilityKind AUTOMATION = of("AUTOMATION");

    public CapabilityKind {
        value = normalize(value);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static CapabilityKind of(String value) {
        return new CapabilityKind(value);
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
