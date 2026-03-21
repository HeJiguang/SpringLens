package io.springlens.server.mcp.http;

import java.util.List;
import java.util.Map;

public record McpToolInvokeResponse(
        List<McpToolTextContentDto> content,
        Map<String, Object> structuredContent,
        boolean isError
) {

    public static McpToolInvokeResponse success(Map<String, Object> structuredContent, String text) {
        return new McpToolInvokeResponse(
                List.of(new McpToolTextContentDto("text", text)),
                structuredContent,
                false
        );
    }

    public static McpToolInvokeResponse error(String message) {
        return new McpToolInvokeResponse(
                List.of(new McpToolTextContentDto("text", message == null ? "Tool execution failed" : message)),
                Map.of(),
                true
        );
    }
}
