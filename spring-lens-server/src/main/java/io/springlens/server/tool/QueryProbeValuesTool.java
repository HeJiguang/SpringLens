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
public class QueryProbeValuesTool implements DiagnosticTool {

    private final ApplicationRegistryService registryService;
    private final RuntimeObservationClient runtimeObservationClient;

    public QueryProbeValuesTool(ApplicationRegistryService registryService, RuntimeObservationClient runtimeObservationClient) {
        this.registryService = registryService;
        this.runtimeObservationClient = runtimeObservationClient;
    }

    @Override
    public ToolDescriptor descriptor() {
        return new ToolDescriptor("query_probe_values", "Query captured probe values from a registered application.");
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
        properties.put("probeId", ToolJsonSchemas.stringProperty("Probe id to query."));
        properties.put("limit", ToolJsonSchemas.integerProperty("Maximum number of records to return."));
        return ToolJsonSchemas.objectSchema(properties, List.of("applicationId", "probeId"));
    }

    @Override
    public Object execute(ToolRequest request) {
        Object probeId = request.arguments().get("probeId");
        if (probeId == null) {
            throw new IllegalArgumentException("probeId is required");
        }
        AppRegistration registration = registryService.resolve(request.applicationId(), request.instanceId());
        int limit = GetSlowSqlTool.numberArgument(request, "limit", 10);
        return runtimeObservationClient.getProbeValues(registration, String.valueOf(probeId), limit);
    }
}
