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
public class ApproveOverlayInstrumentationTool implements DiagnosticTool {

    private final OverlayRegistryService overlayRegistryService;

    public ApproveOverlayInstrumentationTool(OverlayRegistryService overlayRegistryService) {
        this.overlayRegistryService = overlayRegistryService;
    }

    @Override
    public ToolDescriptor descriptor() {
        return new ToolDescriptor("approve_overlay_instrumentation", "Approve a pending overlay registration by overlay id.");
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
                        "overlayId", ToolJsonSchemas.stringProperty("Overlay id to approve."),
                        "actor", ToolJsonSchemas.stringProperty("Actor approving the overlay.")
                ),
                List.of("overlayId")
        );
    }

    @Override
    public Object execute(ToolRequest request) {
        String overlayId = requireString(request.arguments(), "overlayId");
        String actor = optionalString(request.arguments(), "actor", "system");
        return overlayRegistryService.approve(overlayId, actor);
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
