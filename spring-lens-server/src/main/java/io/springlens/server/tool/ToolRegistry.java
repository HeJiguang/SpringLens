package io.springlens.server.tool;

import io.springlens.spi.DiagnosticTool;
import io.springlens.spi.ToolDescriptor;
import io.springlens.spi.ToolExposureLevel;
import io.springlens.spi.ToolSchema;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ToolRegistry {

    private final Map<String, DiagnosticTool> tools = new ConcurrentHashMap<>();

    public ToolRegistry(List<DiagnosticTool> tools) {
        tools.forEach(tool -> this.tools.put(tool.metadata().name(), tool));
    }

    public DiagnosticTool get(String name) {
        DiagnosticTool tool = tools.get(name);
        if (tool == null) {
            throw new IllegalArgumentException("No tool registered for " + name);
        }
        return tool;
    }

    public DiagnosticTool getPublic(String name) {
        DiagnosticTool tool = get(name);
        if (!ToolExposureLevel.PUBLIC.equals(tool.metadata().exposureLevel())) {
            throw new IllegalArgumentException("No public tool registered for " + name);
        }
        return tool;
    }

    public List<DiagnosticTool> publicTools() {
        return tools.values().stream()
                .filter(tool -> ToolExposureLevel.PUBLIC.equals(tool.metadata().exposureLevel()))
                .sorted((left, right) -> left.metadata().name().compareTo(right.metadata().name()))
                .toList();
    }

    public List<ToolDescriptor> descriptors() {
        return publicTools().stream()
                .map(DiagnosticTool::schema)
                .map(this::toDescriptor)
                .sorted((left, right) -> left.name().compareTo(right.name()))
                .toList();
    }

    private ToolDescriptor toDescriptor(ToolSchema schema) {
        return new ToolDescriptor(schema.name(), schema.description());
    }
}
