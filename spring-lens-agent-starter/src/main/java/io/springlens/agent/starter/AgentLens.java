package io.springlens.agent.starter;

public final class AgentLens {

    private AgentLens() {
    }

    public static AgentScope agent(String overlayId) {
        return new AgentScope(overlayId);
    }

    public record AgentScope(String overlayId) {

        public AgentProbe look(String probeId, Object value, String description) {
            return new AgentProbe(overlayId, probeId, value, description);
        }
    }

    public record AgentProbe(
            String overlayId,
            String probeId,
            Object value,
            String description
    ) {
    }
}
