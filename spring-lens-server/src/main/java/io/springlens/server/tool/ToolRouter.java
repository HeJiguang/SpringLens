package io.springlens.server.tool;

import io.springlens.spi.ToolRequest;
import org.springframework.stereotype.Component;

@Component
public class ToolRouter {

    private final ToolRegistry registry;

    public ToolRouter(ToolRegistry registry) {
        this.registry = registry;
    }

    public Object invoke(String toolName, ToolRequest request) {
        return registry.get(toolName).execute(request);
    }
}
