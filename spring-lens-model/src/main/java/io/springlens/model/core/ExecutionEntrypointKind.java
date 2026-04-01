package io.springlens.model.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

public record ExecutionEntrypointKind(String value) {

    public static final ExecutionEntrypointKind HTTP_SERVER = of("HTTP_SERVER");
    public static final ExecutionEntrypointKind HTTP_CLIENT = of("HTTP_CLIENT");
    public static final ExecutionEntrypointKind KAFKA_CONSUMER = of("KAFKA_CONSUMER");
    public static final ExecutionEntrypointKind RABBIT_LISTENER = of("RABBIT_LISTENER");
    public static final ExecutionEntrypointKind JMS_LISTENER = of("JMS_LISTENER");
    public static final ExecutionEntrypointKind SCHEDULED = of("SCHEDULED");
    public static final ExecutionEntrypointKind UNKNOWN = of("UNKNOWN");

    public ExecutionEntrypointKind {
        value = normalize(value);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ExecutionEntrypointKind of(String value) {
        return new ExecutionEntrypointKind(value);
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
