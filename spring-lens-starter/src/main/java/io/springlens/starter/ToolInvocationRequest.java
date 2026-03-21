package io.springlens.starter;

import java.util.Map;

public record ToolInvocationRequest(Map<String, Object> arguments) {

    public ToolInvocationRequest {
        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
    }
}
