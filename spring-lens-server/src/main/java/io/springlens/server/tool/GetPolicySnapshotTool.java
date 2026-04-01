package io.springlens.server.tool;

import io.springlens.server.controlplane.policy.AgentPolicyService;
import io.springlens.spi.DiagnosticTool;
import io.springlens.spi.ToolDescriptor;
import io.springlens.spi.ToolMetadata;
import io.springlens.spi.ToolRequest;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GetPolicySnapshotTool implements DiagnosticTool {

    private final AgentPolicyService agentPolicyService;

    public GetPolicySnapshotTool(AgentPolicyService agentPolicyService) {
        this.agentPolicyService = agentPolicyService;
    }

    @Override
    public ToolDescriptor descriptor() {
        return new ToolDescriptor("get_policy_snapshot", "Get the current agent instrumentation policy snapshot.");
    }

    @Override
    public ToolMetadata metadata() {
        ToolDescriptor descriptor = descriptor();
        return ToolMetadata.publiclyExposed(descriptor.name(), descriptor.description());
    }

    @Override
    public Map<String, Object> inputSchema() {
        return ToolJsonSchemas.objectSchema(Map.of(), List.of());
    }

    @Override
    public Object execute(ToolRequest request) {
        return agentPolicyService.snapshot();
    }
}
