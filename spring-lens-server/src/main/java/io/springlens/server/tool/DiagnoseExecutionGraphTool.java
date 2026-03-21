package io.springlens.server.tool;

import io.springlens.model.AppRegistration;
import io.springlens.model.core.ExecutionGraph;
import io.springlens.server.registry.ApplicationRegistryService;
import io.springlens.server.runtime.RuntimeObservationClient;
import io.springlens.spi.DiagnosticEngine;
import io.springlens.spi.DiagnosticTool;
import io.springlens.spi.ToolDescriptor;
import io.springlens.spi.ToolMetadata;
import io.springlens.spi.ToolRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DiagnoseExecutionGraphTool implements DiagnosticTool {

    private final ApplicationRegistryService registryService;
    private final RuntimeObservationClient runtimeObservationClient;
    private final DiagnosticEngine diagnosticEngine;

    public DiagnoseExecutionGraphTool(
            ApplicationRegistryService registryService,
            RuntimeObservationClient runtimeObservationClient,
            DiagnosticEngine diagnosticEngine
    ) {
        this.registryService = registryService;
        this.runtimeObservationClient = runtimeObservationClient;
        this.diagnosticEngine = diagnosticEngine;
    }

    @Override
    public ToolDescriptor descriptor() {
        return new ToolDescriptor(
                "diagnose_execution_graph",
                "Analyze an execution graph and return a structured diagnosis."
        );
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
        properties.put("executionId", ToolJsonSchemas.stringProperty("Execution id to analyze."));
        return ToolJsonSchemas.objectSchema(properties, List.of("applicationId", "executionId"));
    }

    @Override
    public Object execute(ToolRequest request) {
        Object executionId = request.arguments().get("executionId");
        if (executionId == null) {
            throw new IllegalArgumentException("executionId is required");
        }
        AppRegistration registration = registryService.resolve(request.applicationId(), request.instanceId());
        ExecutionGraph graph = runtimeObservationClient.getExecutionGraph(registration, String.valueOf(executionId));
        return diagnosticEngine.analyze(graph);
    }
}
