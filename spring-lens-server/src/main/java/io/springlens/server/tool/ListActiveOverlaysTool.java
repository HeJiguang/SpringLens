package io.springlens.server.tool;

import io.springlens.server.controlplane.overlay.OverlayRegistryService;
import io.springlens.spi.DiagnosticTool;
import io.springlens.spi.ToolDescriptor;
import io.springlens.spi.ToolMetadata;
import io.springlens.spi.ToolRequest;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ListActiveOverlaysTool implements DiagnosticTool {

    private final OverlayRegistryService overlayRegistryService;

    public ListActiveOverlaysTool(OverlayRegistryService overlayRegistryService) {
        this.overlayRegistryService = overlayRegistryService;
    }

    @Override
    public ToolDescriptor descriptor() {
        return new ToolDescriptor("list_active_overlays", "List active agent overlay registrations.");
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
        return overlayRegistryService.listActive();
    }
}
