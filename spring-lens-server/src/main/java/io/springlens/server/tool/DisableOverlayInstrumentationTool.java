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
public class DisableOverlayInstrumentationTool implements DiagnosticTool {

    private final OverlayRegistryService overlayRegistryService;

    public DisableOverlayInstrumentationTool(OverlayRegistryService overlayRegistryService) {
        this.overlayRegistryService = overlayRegistryService;
    }

    @Override
    public ToolDescriptor descriptor() {
        return new ToolDescriptor("disable_overlay_instrumentation", "Disable an agent overlay registration by overlay id.");
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
                        "overlayId", ToolJsonSchemas.stringProperty("Overlay id to disable."),
                        "actor", ToolJsonSchemas.stringProperty("Actor requesting the disable action.")
                ),
                List.of("overlayId")
        );
    }

    @Override
    public Object execute(ToolRequest request) {
        String overlayId = requireString(request.arguments(), "overlayId");
        String actor = optionalString(request.arguments(), "actor", "system");
        return overlayRegistryService.disable(overlayId, actor);
    }

    private static String requireString(Map<String, Object> arguments, String key) {
        Object value = arguments.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(key + " must not be blank");
        }
        return text;
    }

    private static String optionalString(Map<String, Object> arguments, String key, String fallback) {
        Object value = arguments.get(key);
        return value instanceof String text && !text.isBlank() ? text : fallback;
    }
}
