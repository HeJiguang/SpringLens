package io.springlens.agent.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

public record AgentInstrumentationMode(String value) {

    public static final AgentInstrumentationMode OFF = of("OFF");
    public static final AgentInstrumentationMode OVERLAY_ONLY = of("OVERLAY_ONLY");
    public static final AgentInstrumentationMode HYBRID_APPROVAL = of("HYBRID_APPROVAL");
    public static final AgentInstrumentationMode FULL_TRUST = of("FULL_TRUST");

    public AgentInstrumentationMode {
        value = normalize(value);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static AgentInstrumentationMode of(String value) {
        return new AgentInstrumentationMode(value);
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
