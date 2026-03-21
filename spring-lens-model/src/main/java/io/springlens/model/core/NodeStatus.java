package io.springlens.model.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

/**
 * Execution status of a node.
 */
public record NodeStatus(String value) {

    public static final NodeStatus RUNNING = of("RUNNING");
    public static final NodeStatus SUCCESS = of("SUCCESS");
    public static final NodeStatus FAILURE = of("FAILURE");

    public NodeStatus {
        value = normalize(value);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static NodeStatus of(String value) {
        return new NodeStatus(value);
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
