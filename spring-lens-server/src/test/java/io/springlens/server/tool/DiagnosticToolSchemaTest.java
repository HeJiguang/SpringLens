package io.springlens.server.tool;

import static org.assertj.core.api.Assertions.assertThat;

import io.springlens.spi.DiagnosticTool;
import io.springlens.spi.ToolDescriptor;
import io.springlens.spi.ToolRequest;
import io.springlens.spi.ToolSchema;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DiagnosticToolSchemaTest {

    @Test
    void buildsDefaultSchemaFromDescriptorAndDefaultJsonSchemas() {
        DiagnosticTool tool = new DiagnosticTool() {
            @Override
            public ToolDescriptor descriptor() {
                return new ToolDescriptor("sample_tool", "Sample tool");
            }

            @Override
            public Object execute(ToolRequest request) {
                return Map.of();
            }
        };

        ToolSchema schema = tool.schema();

        assertThat(schema.name()).isEqualTo("sample_tool");
        assertThat(schema.description()).isEqualTo("Sample tool");
        assertThat(schema.inputSchema())
                .containsEntry("$schema", "https://json-schema.org/draft/2020-12/schema")
                .containsEntry("type", "object")
                .containsEntry("additionalProperties", false);
        assertThat(schema.outputSchema())
                .containsEntry("$schema", "https://json-schema.org/draft/2020-12/schema")
                .containsEntry("type", "object")
                .containsEntry("additionalProperties", true);
    }
}
