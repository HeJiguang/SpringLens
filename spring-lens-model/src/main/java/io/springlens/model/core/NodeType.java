package io.springlens.model.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

/**
 * Semantic type of an execution node.
 */
public record NodeType(String value) {

    public static final NodeType HTTP_REQUEST = of("HTTP_REQUEST");
    public static final NodeType JDBC_SQL = of("JDBC_SQL");
    public static final NodeType EXCEPTION = of("EXCEPTION");
    public static final NodeType WATCH_VALUE = of("WATCH_VALUE");
    public static final NodeType CHECKPOINT = of("CHECKPOINT");

    public NodeType {
        value = normalize(value);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static NodeType of(String value) {
        return new NodeType(value);
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
