package io.springlens.server.mcp.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import org.springframework.lang.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpToolDefinitionDto(
        String name,
        String description,
        Map<String, Object> inputSchema,
        Map<String, Object> outputSchema,
        @Nullable Map<String, Object> annotations
) {
}
