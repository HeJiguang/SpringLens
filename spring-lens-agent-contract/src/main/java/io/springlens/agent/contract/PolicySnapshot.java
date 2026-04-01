package io.springlens.agent.contract;

import java.util.Map;
import java.util.Objects;

public record PolicySnapshot(
        AgentInstrumentationMode mode,
        boolean sourceEditEnabled,
        boolean sourceEditAutoApply,
        boolean approvalRequired,
        Map<String, String> metadata
) {

    public PolicySnapshot {
        mode = Objects.requireNonNull(mode, "mode");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
