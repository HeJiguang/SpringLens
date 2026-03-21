package io.springlens.server.tool;

import io.springlens.server.registry.ApplicationRegistryService;
import io.springlens.spi.DiagnosticTool;
import io.springlens.spi.ToolDescriptor;
import io.springlens.spi.ToolMetadata;
import io.springlens.spi.ToolRequest;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ListRegisteredAppsTool implements DiagnosticTool {

    private final ApplicationRegistryService registryService;

    public ListRegisteredAppsTool(ApplicationRegistryService registryService) {
        this.registryService = registryService;
    }

    @Override
    public ToolDescriptor descriptor() {
        return new ToolDescriptor("list_registered_apps", "List registered Spring Lens runtime applications.");
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
        return registryService.list();
    }
}
