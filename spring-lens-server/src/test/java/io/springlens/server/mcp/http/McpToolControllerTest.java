package io.springlens.server.mcp.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.springlens.server.tool.ToolRegistry;
import io.springlens.spi.DiagnosticTool;
import io.springlens.spi.ToolDescriptor;
import io.springlens.spi.ToolExposureLevel;
import io.springlens.spi.ToolMetadata;
import io.springlens.spi.ToolRequest;
import io.springlens.spi.ToolSchema;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

class McpToolControllerTest {

    private final PublicEchoTool publicTool = new PublicEchoTool();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ToolRegistry registry = new ToolRegistry(List.of(new InternalTool(), publicTool));
        mockMvc = MockMvcBuilders.standaloneSetup(new McpToolController(registry, new ObjectMapper())).build();
    }

    @Test
    void listsOnlyPublicTools() throws Exception {
        mockMvc.perform(get("/mcp/tools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tools.length()").value(1))
                .andExpect(jsonPath("$.tools[0].name").value("public_echo"))
                .andExpect(jsonPath("$.tools[0].description").value("Echo a value from schema"))
                .andExpect(jsonPath("$.tools[0].inputSchema.properties.value.type").value("string"))
                .andExpect(jsonPath("$.tools[0].outputSchema.properties.echo.type").value("string"));
    }

    @Test
    void invokesPublicToolWithTopLevelJsonArguments() throws Exception {
        mockMvc.perform(post("/mcp/tools/public_echo/invoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "applicationId": "orders-app",
                                  "value": "ok"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isError").value(false))
                .andExpect(jsonPath("$.structuredContent.echo").value("ok"))
                .andExpect(jsonPath("$.content[0].type").value("text"));

        assertThat(publicTool.lastRequest.applicationId()).isEqualTo("orders-app");
        assertThat(publicTool.lastRequest.instanceId()).isNull();
        assertThat(publicTool.lastRequest.arguments()).containsEntry("value", "ok");
        assertThat(publicTool.lastRequest.arguments()).doesNotContainKey("applicationId");
    }

    @Test
    void rejectsInternalToolInvocation() throws Exception {
        mockMvc.perform(post("/mcp/tools/internal_only/invoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    private static final class InternalTool implements DiagnosticTool {

        @Override
        public ToolDescriptor descriptor() {
            return new ToolDescriptor("internal_only", "Internal only");
        }

        @Override
        public Object execute(ToolRequest request) {
            return Map.of();
        }
    }

    private static final class PublicEchoTool implements DiagnosticTool {

        private ToolRequest lastRequest;

        @Override
        public ToolDescriptor descriptor() {
            return new ToolDescriptor("public_echo", "Echo a value");
        }

        @Override
        public ToolMetadata metadata() {
            return new ToolMetadata("public_echo", "Echo a value", ToolExposureLevel.PUBLIC);
        }

        @Override
        public ToolSchema schema() {
            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("applicationId", Map.of("type", "string"));
            properties.put("value", Map.of("type", "string"));
            Map<String, Object> inputSchema = Map.of(
                    "$schema", "https://json-schema.org/draft/2020-12/schema",
                    "type", "object",
                    "properties", properties,
                    "required", List.of("applicationId", "value"),
                    "additionalProperties", false
            );
            Map<String, Object> outputSchema = Map.of(
                    "$schema", "https://json-schema.org/draft/2020-12/schema",
                    "type", "object",
                    "properties", Map.of("echo", Map.of("type", "string")),
                    "additionalProperties", false
            );
            return new ToolSchema("public_echo", "Echo a value from schema", inputSchema, outputSchema);
        }

        @Override
        public Object execute(ToolRequest request) {
            lastRequest = request;
            return Map.of("echo", request.arguments().get("value"));
        }
    }
}
