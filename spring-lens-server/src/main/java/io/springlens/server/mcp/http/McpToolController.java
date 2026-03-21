package io.springlens.server.mcp.http;

import io.springlens.server.tool.ToolRegistry;
import io.springlens.spi.DiagnosticTool;
import io.springlens.spi.ToolRequest;
import io.springlens.spi.ToolSchema;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/mcp/tools")
public class McpToolController {

    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    public McpToolController(ToolRegistry toolRegistry, ObjectMapper objectMapper) {
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public McpToolListResponse listTools() {
        List<McpToolDefinitionDto> tools = toolRegistry.publicTools().stream()
                .map(this::toDefinition)
                .toList();
        return new McpToolListResponse(tools);
    }

    @PostMapping("/{toolName}/invoke")
    public McpToolInvokeResponse invokeTool(
            @PathVariable String toolName,
            @RequestBody(required = false) McpToolInvokeRequest request
    ) {
        DiagnosticTool tool = resolvePublicTool(toolName);
        try {
            Object result = tool.execute(toToolRequest(request));
            Map<String, Object> structuredContent = toStructuredContent(result);
            return McpToolInvokeResponse.success(structuredContent, writeJson(structuredContent));
        }
        catch (RuntimeException ex) {
            return McpToolInvokeResponse.error(ex.getMessage());
        }
    }

    private DiagnosticTool resolvePublicTool(String toolName) {
        try {
            return toolRegistry.getPublic(toolName);
        }
        catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(NOT_FOUND, ex.getMessage());
        }
    }

    private McpToolDefinitionDto toDefinition(DiagnosticTool tool) {
        ToolSchema schema = tool.schema();
        return new McpToolDefinitionDto(
                schema.name(),
                schema.description(),
                schema.inputSchema(),
                schema.outputSchema(),
                tool.annotations().isEmpty() ? null : tool.annotations()
        );
    }

    private ToolRequest toToolRequest(McpToolInvokeRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>(request == null ? Map.of() : request.arguments());
        String applicationId = stringValue(payload.remove("applicationId"));
        String instanceId = stringValue(payload.remove("instanceId"));
        return new ToolRequest(applicationId, instanceId, payload);
    }

    private Map<String, Object> toStructuredContent(Object result) {
        Object converted = objectMapper.convertValue(result, Object.class);
        if (converted instanceof Map<?, ?> map) {
            Map<String, Object> structured = new LinkedHashMap<>();
            map.forEach((key, value) -> structured.put(String.valueOf(key), value));
            return structured;
        }
        return Map.of("result", converted);
    }

    private String writeJson(Map<String, Object> structuredContent) {
        try {
            return objectMapper.writeValueAsString(structuredContent);
        }
        catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize tool result", ex);
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
