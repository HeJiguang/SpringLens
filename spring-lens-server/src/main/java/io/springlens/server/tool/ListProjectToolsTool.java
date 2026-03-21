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
public class ListProjectToolsTool implements DiagnosticTool {

    private final ApplicationRegistryService registryService;
    private final RuntimeObservationClient runtimeObservationClient;

    public ListProjectToolsTool(ApplicationRegistryService registryService, RuntimeObservationClient runtimeObservationClient) {
        this.registryService = registryService;
        this.runtimeObservationClient = runtimeObservationClient;
    }

    @Override
    public ToolDescriptor descriptor() {
        return new ToolDescriptor("list_project_tools", "List project-defined runtime tools from a registered application.");
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
        return ToolJsonSchemas.objectSchema(properties, List.of("applicationId"));
    }

    @Override
    public Object execute(ToolRequest request) {
        AppRegistration registration = registryService.resolve(request.applicationId(), request.instanceId());
        return runtimeObservationClient.listProjectTools(registration);
    }
}
