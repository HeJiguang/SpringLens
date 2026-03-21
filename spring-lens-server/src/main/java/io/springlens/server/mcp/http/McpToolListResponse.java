package io.springlens.server.mcp.http;

import java.util.List;

public record McpToolListResponse(
        List<McpToolDefinitionDto> tools
) {
}
