package io.springlens.server.tool;

import io.springlens.spi.DiagnosticTool;
import io.springlens.spi.ToolDescriptor;
import io.springlens.spi.ToolMetadata;
import io.springlens.spi.ToolRequest;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DraftRuntimeSafetyRemediationTool implements DiagnosticTool {

    private final RuntimeSafetyDraftPlanner runtimeSafetyDraftPlanner;

    public DraftRuntimeSafetyRemediationTool(RuntimeSafetyDraftPlanner runtimeSafetyDraftPlanner) {
        this.runtimeSafetyDraftPlanner = runtimeSafetyDraftPlanner;
    }

    @Override
    public ToolDescriptor descriptor() {
        return new ToolDescriptor(
                "draft_runtime_safety_remediation",
                "Generate control-plane-ready overlay and patch drafts from runtime safety remediation findings."
        );
    }

    @Override
    public ToolMetadata metadata() {
        ToolDescriptor descriptor = descriptor();
        return ToolMetadata.publiclyExposed(descriptor.name(), descriptor.description());
    }

    @Override
    public Map<String, Object> inputSchema() {
        return ToolJsonSchemas.objectSchema(
                Map.of(
                        "applicationId", ToolJsonSchemas.stringProperty("Application id registered in Spring Lens."),
                        "instanceId", ToolJsonSchemas.stringProperty("Optional instance id."),
                        "arguments", ToolJsonSchemas.objectProperty("Optional runtime safety remediation arguments.")
                ),
                List.of("applicationId")
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object execute(ToolRequest request) {
        Object arguments = request.arguments().get("arguments");
        Map<String, Object> runtimeArguments = arguments instanceof Map<?, ?> values
                ? (Map<String, Object>) values
                : Map.of();
        return runtimeSafetyDraftPlanner.plan(request.applicationId(), request.instanceId(), runtimeArguments);
    }
}
