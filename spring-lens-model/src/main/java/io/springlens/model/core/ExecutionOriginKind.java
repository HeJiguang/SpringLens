package io.springlens.model.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

public record ExecutionOriginKind(String value) {

    public static final ExecutionOriginKind BASE = of("BASE");
    public static final ExecutionOriginKind OBSERVATION = of("OBSERVATION");
    public static final ExecutionOriginKind COMPAT_FILTER = of("COMPAT_FILTER");
    public static final ExecutionOriginKind COMPAT_INTERCEPTOR = of("COMPAT_INTERCEPTOR");
    public static final ExecutionOriginKind COMPAT_ASPECT = of("COMPAT_ASPECT");
    public static final ExecutionOriginKind LENS_WATCH = of("LENS_WATCH");
    public static final ExecutionOriginKind LENS_LOOK = of("LENS_LOOK");
    public static final ExecutionOriginKind AGENT_OVERLAY = of("AGENT_OVERLAY");
    public static final ExecutionOriginKind AGENT_PATCH = of("AGENT_PATCH");
    public static final ExecutionOriginKind RUNTIME_TOOL = of("RUNTIME_TOOL");

    public ExecutionOriginKind {
        value = normalize(value);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ExecutionOriginKind of(String value) {
        return new ExecutionOriginKind(value);
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
