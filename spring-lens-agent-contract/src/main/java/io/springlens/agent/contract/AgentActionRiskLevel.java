package io.springlens.agent.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

public record AgentActionRiskLevel(String value) {

    public static final AgentActionRiskLevel LOW = of("LOW");
    public static final AgentActionRiskLevel MEDIUM = of("MEDIUM");
    public static final AgentActionRiskLevel HIGH = of("HIGH");

    public AgentActionRiskLevel {
        value = normalize(value);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static AgentActionRiskLevel of(String value) {
        return new AgentActionRiskLevel(value);
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
