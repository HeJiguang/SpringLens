package io.springlens.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

public record ToolExposureLevel(String value) {

    public static final ToolExposureLevel INTERNAL = of("INTERNAL");
    public static final ToolExposureLevel PUBLIC = of("PUBLIC");

    public ToolExposureLevel {
        value = normalize(value);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ToolExposureLevel of(String value) {
        return new ToolExposureLevel(value);
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
