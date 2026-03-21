package io.springlens.server.tool;

import io.springlens.model.AppRegistration;
import io.springlens.server.registry.ApplicationRegistryService;
import io.springlens.server.runtime.RuntimeObservationClient;
import io.springlens.spi.DiagnosticTool;
import io.springlens.spi.ToolDescriptor;
import io.springlens.spi.ToolMetadata;
import io.springlens.spi.ToolRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GetSlowSqlTool implements DiagnosticTool {

    private final ApplicationRegistryService registryService;
    private final RuntimeObservationClient runtimeObservationClient;

    public GetSlowSqlTool(ApplicationRegistryService registryService, RuntimeObservationClient runtimeObservationClient) {
        this.registryService = registryService;
        this.runtimeObservationClient = runtimeObservationClient;
    }

    @Override
    public ToolDescriptor descriptor() {
        return new ToolDescriptor("get_slow_sql", "Query recent slow SQL observations from a Spring Lens runtime.");
    }

    @Override
    public ToolMetadata metadata() {
        ToolDescriptor descriptor = descriptor();
        return ToolMetadata.publiclyExposed(descriptor.name(), descriptor.description());
    }

    @Override
    public Map<String, Object> inputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("applicationId", ToolJsonSchemas.stringProperty("Application id registered in Spring Lens."));
        properties.put("instanceId", ToolJsonSchemas.stringProperty("Optional instance id."));
        properties.put("limit", ToolJsonSchemas.integerProperty("Maximum number of records to return."));
        properties.put("minDurationMs", ToolJsonSchemas.integerProperty("Minimum SQL duration in milliseconds."));
        return ToolJsonSchemas.objectSchema(properties, List.of("applicationId"));
    }

    @Override
    public Object execute(ToolRequest request) {
        AppRegistration registration = registryService.resolve(request.applicationId(), request.instanceId());
        int limit = numberArgument(request, "limit", 10);
        long minDurationMs = longArgument(request, "minDurationMs", 200L);
        return runtimeObservationClient.getSlowSql(registration, limit, minDurationMs);
    }

    static int numberArgument(ToolRequest request, String name, int fallback) {
        Object value = request.arguments().get(name);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    static long longArgument(ToolRequest request, String name, long fallback) {
        Object value = request.arguments().get(name);
        return value instanceof Number number ? number.longValue() : fallback;
    }
}
