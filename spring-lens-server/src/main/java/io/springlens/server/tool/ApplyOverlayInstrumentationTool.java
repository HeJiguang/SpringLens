package io.springlens.server.tool;

import io.springlens.agent.contract.AgentActionRiskLevel;
import io.springlens.agent.contract.AgentInstrumentationMode;
import io.springlens.agent.contract.OverlaySpec;
import io.springlens.server.controlplane.overlay.OverlayRegistryService;
import io.springlens.spi.DiagnosticTool;
import io.springlens.spi.ToolDescriptor;
import io.springlens.spi.ToolMetadata;
import io.springlens.spi.ToolRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ApplyOverlayInstrumentationTool implements DiagnosticTool {

    private final OverlayRegistryService overlayRegistryService;

    public ApplyOverlayInstrumentationTool(OverlayRegistryService overlayRegistryService) {
        this.overlayRegistryService = overlayRegistryService;
    }

    @Override
    public ToolDescriptor descriptor() {
        return new ToolDescriptor("apply_overlay_instrumentation", "Register a new agent overlay specification.");
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
                        "overlayId", ToolJsonSchemas.stringProperty("Stable overlay identifier."),
                        "mode", ToolJsonSchemas.stringProperty("Instrumentation mode, for example HYBRID_APPROVAL."),
                        "riskLevel", ToolJsonSchemas.stringProperty("Risk level, for example LOW, MEDIUM, or HIGH."),
                        "selectorType", ToolJsonSchemas.stringProperty("Selector type, for example spring-bean-method."),
                        "targetClassName", ToolJsonSchemas.stringProperty("Target class name."),
                        "targetMethodName", ToolJsonSchemas.stringProperty("Target method name."),
                        "actor", ToolJsonSchemas.stringProperty("Actor requesting the overlay application."),
                        "metadata", ToolJsonSchemas.objectProperty("Overlay metadata map.")
                ),
                List.of("overlayId", "mode", "riskLevel", "selectorType")
        );
    }

    @Override
    public Object execute(ToolRequest request) {
        Map<String, Object> arguments = request.arguments();
        OverlaySpec spec = new OverlaySpec(
                requireString(arguments, "overlayId"),
                AgentInstrumentationMode.of(requireString(arguments, "mode")),
                AgentActionRiskLevel.of(requireString(arguments, "riskLevel")),
                optionalBoolean(arguments, "enabled", true),
                optionalString(arguments, "ttl", null),
                requireString(arguments, "selectorType"),
                optionalString(arguments, "targetClassName", null),
                optionalString(arguments, "targetMethodName", null),
                optionalString(arguments, "capturePhase", null),
                optionalString(arguments, "probeId", null),
                optionalString(arguments, "expression", null),
                optionalString(arguments, "description", null),
                stringList(arguments.get("tags")),
                stringMap(arguments.get("metadata"))
        );
        String actor = optionalString(arguments, "actor", "system");
        return overlayRegistryService.apply(spec, actor);
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
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        return fallback;
    }

    private static boolean optionalBoolean(Map<String, Object> arguments, String key, boolean fallback) {
        Object value = arguments.get(key);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof String text && !text.isBlank()) {
            return Boolean.parseBoolean(text);
        }
        return fallback;
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof Iterable<?> items)) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (Object item : items) {
            if (item == null) {
                continue;
            }
            String text = item.toString().trim();
            if (!text.isEmpty()) {
                values.add(text);
            }
        }
        return List.copyOf(values);
    }

    private static Map<String, String> stringMap(Object value) {
        if (!(value instanceof Map<?, ?> rawMap)) {
            return Map.of();
        }
        Map<String, String> values = new LinkedHashMap<>();
        rawMap.forEach((key, mapValue) -> {
            if (key == null || mapValue == null) {
                return;
            }
            String normalizedKey = key.toString().trim();
            String normalizedValue = mapValue.toString().trim();
            if (!normalizedKey.isEmpty() && !normalizedValue.isEmpty()) {
                values.put(normalizedKey, normalizedValue);
            }
        });
        return Map.copyOf(values);
    }
}
