package io.springlens.spi;

import java.util.Map;

public interface DiagnosticTool {

    ToolDescriptor descriptor();

    default ToolMetadata metadata() {
        ToolDescriptor descriptor = descriptor();
        return ToolMetadata.internal(descriptor.name(), descriptor.description());
    }

    default ToolSchema schema() {
        ToolMetadata metadata = metadata();
        return new ToolSchema(
                metadata.name(),
                metadata.description(),
                inputSchema(),
                outputSchema()
        );
    }

    default Map<String, Object> inputSchema() {
        return Map.of(
                "$schema", "https://json-schema.org/draft/2020-12/schema",
                "type", "object",
                "additionalProperties", false
        );
    }

    default Map<String, Object> outputSchema() {
        return Map.of(
                "$schema", "https://json-schema.org/draft/2020-12/schema",
                "type", "object",
                "additionalProperties", true
        );
    }

    default Map<String, Object> annotations() {
        return Map.of();
    }

    Object execute(ToolRequest request);
}
