package io.springlens.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

public record ProjectToolSourceType(String value) {

    public static final ProjectToolSourceType HANDWRITTEN = of("HANDWRITTEN");
    public static final ProjectToolSourceType GENERATED_TRACE = of("GENERATED_TRACE");
    public static final ProjectToolSourceType GENERATED_PROBE = of("GENERATED_PROBE");

    public ProjectToolSourceType {
        value = normalize(value);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ProjectToolSourceType of(String value) {
        return new ProjectToolSourceType(value);
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
