package io.springlens.model.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

public record ExecutionTransportKind(String value) {

    public static final ExecutionTransportKind HTTP = of("HTTP");
    public static final ExecutionTransportKind JDBC = of("JDBC");
    public static final ExecutionTransportKind MESSAGING = of("MESSAGING");
    public static final ExecutionTransportKind IN_PROCESS = of("IN_PROCESS");
    public static final ExecutionTransportKind UNKNOWN = of("UNKNOWN");

    public ExecutionTransportKind {
        value = normalize(value);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ExecutionTransportKind of(String value) {
        return new ExecutionTransportKind(value);
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
