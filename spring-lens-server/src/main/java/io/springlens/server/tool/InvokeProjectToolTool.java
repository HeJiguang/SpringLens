package io.springlens.server.tool;

import io.springlens.model.AppRegistration;
import io.springlens.server.registry.ApplicationRegistryService;
import io.springlens.server.runtime.RuntimeObservationClient;
import io.springlens.spi.DiagnosticTool;
import io.springlens.spi.ToolDescriptor;
import io.springlens.spi.ToolMetadata;
import io.springlens.spi.ToolRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class InvokeProjectToolTool implements DiagnosticTool {

    private final ApplicationRegistryService registryService;
    private final RuntimeObservationClient runtimeObservationClient;

    public InvokeProjectToolTool(ApplicationRegistryService registryService, RuntimeObservationClient runtimeObservationClient) {
        this.registryService = registryService;
        this.runtimeObservationClient = runtimeObservationClient;
    }

    @Override
    public ToolDescriptor descriptor() {
        return new ToolDescriptor("invoke_project_tool", "Invoke a project-defined runtime tool on a registered application.");
    }

    @Override
    public ToolMetadata metadata() {
        ToolDescriptor descriptor = descriptor();
        return ToolMetadata.publiclyExposed(descriptor.name(), descriptor.description());
    }

    @Override
    public Map<String, Object> inputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("applicationId", ToolJsonSchemas.stringProperty("Application id registered in Spring Lens."));
        properties.put("instanceId", ToolJsonSchemas.stringProperty("Optional instance id."));
        properties.put("toolName", ToolJsonSchemas.stringProperty("Project tool name."));
        properties.put("arguments", ToolJsonSchemas.objectProperty("Tool arguments as a JSON object."));
        return ToolJsonSchemas.objectSchema(properties, List.of("applicationId", "toolName"));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object execute(ToolRequest request) {
        Object toolName = request.arguments().get("toolName");
        if (toolName == null) {
            throw new IllegalArgumentException("toolName is required");
        }
        AppRegistration registration = registryService.resolve(request.applicationId(), request.instanceId());
        Object arguments = request.arguments().get("arguments");
        Map<String, Object> argumentMap = arguments instanceof Map<?, ?> values
                ? (Map<String, Object>) values
                : Map.of();
        return runtimeObservationClient.invokeProjectTool(registration, String.valueOf(toolName), argumentMap);
    }
}
