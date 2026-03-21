package io.springlens.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

public record CapabilitySource(String value) {

    public static final CapabilitySource BUILT_IN = of("BUILT_IN");
    public static final CapabilitySource APPLICATION = of("APPLICATION");
    public static final CapabilitySource GENERATED = of("GENERATED");

    public CapabilitySource {
        value = normalize(value);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static CapabilitySource of(String value) {
        return new CapabilitySource(value);
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
