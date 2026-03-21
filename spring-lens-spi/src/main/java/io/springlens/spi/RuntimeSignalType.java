package io.springlens.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

/**
 * Type of low-level runtime signal emitted by the instrumentation layer.
 */
public record RuntimeSignalType(String value) {

    public static final RuntimeSignalType HTTP_REQUEST_STARTED = of("HTTP_REQUEST_STARTED");
    public static final RuntimeSignalType HTTP_REQUEST_COMPLETED = of("HTTP_REQUEST_COMPLETED");
    public static final RuntimeSignalType JDBC_EXECUTED = of("JDBC_EXECUTED");
    public static final RuntimeSignalType EXCEPTION_CAPTURED = of("EXCEPTION_CAPTURED");
    public static final RuntimeSignalType PROBE_VALUE_CAPTURED = of("PROBE_VALUE_CAPTURED");

    public RuntimeSignalType {
        value = normalize(value);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static RuntimeSignalType of(String value) {
        return new RuntimeSignalType(value);
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
