package io.springlens.server.controlplane.policy;

import io.springlens.agent.contract.AgentInstrumentationMode;
import io.springlens.agent.contract.PolicySnapshot;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AgentPolicyService {

    public PolicySnapshot snapshot() {
        return new PolicySnapshot(
                AgentInstrumentationMode.HYBRID_APPROVAL,
                true,
                false,
                true,
                Map.of(
                        "plane", "agent-control",
                        "policySource", "in-memory"
                )
        );
    }
}
