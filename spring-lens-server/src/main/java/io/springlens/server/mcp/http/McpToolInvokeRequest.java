package io.springlens.server.mcp.http;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.LinkedHashMap;
import java.util.Map;

public class McpToolInvokeRequest {

    private final Map<String, Object> arguments = new LinkedHashMap<>();

    @JsonAnySetter
    public void put(String key, Object value) {
        arguments.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> arguments() {
        return arguments;
    }
}
