package io.springlens.starter;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import io.springlens.model.ProbeDescriptor;
import io.springlens.model.RuntimeCapabilityDescriptor;
import io.springlens.model.RuntimeToolDescriptor;
import io.springlens.model.RuntimeToolSchemaDescriptor;
import io.springlens.model.core.ExecutionGraph;
import io.springlens.model.diagnostic.ExceptionContextRecord;
import io.springlens.model.diagnostic.ProbeValueRecord;
import io.springlens.model.diagnostic.SlowSqlRecord;
import io.springlens.runtime.InMemoryExecutionGraphStore;
import io.springlens.starter.capability.LensCapabilityRegistry;
import io.springlens.starter.probe.LensProbeRegistry;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/spring-lens")
public class RuntimeQueryController {

    private final InMemoryExecutionGraphStore graphStore;
    private final LensRuntimeProperties properties;
    private final LensProbeRegistry probeRegistry;
    private final LensCapabilityRegistry capabilityRegistry;

    public RuntimeQueryController(
            InMemoryExecutionGraphStore graphStore,
            LensRuntimeProperties properties,
            LensProbeRegistry probeRegistry,
            LensCapabilityRegistry capabilityRegistry
    ) {
        this.graphStore = graphStore;
        this.properties = properties;
        this.probeRegistry = probeRegistry;
        this.capabilityRegistry = capabilityRegistry;
    }

    @GetMapping("/slow-sql")
    public List<SlowSqlRecord> getSlowSql(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "200") long minDurationMs
    ) {
        return graphStore.findSlowSql(applicationId(), limit, minDurationMs);
    }

    @GetMapping("/exception-context")
    public List<ExceptionContextRecord> getExceptionContext(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String exceptionClass
    ) {
        return graphStore.findExceptionContexts(applicationId(), limit, exceptionClass);
    }

    @GetMapping("/graphs/{executionId}")
    public ExecutionGraph getExecutionGraph(@PathVariable String executionId) {
        return graphStore.findGraph(executionId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Execution graph not found: " + executionId));
    }

    @GetMapping("/probes")
    public List<ProbeDescriptor> getProbes() {
        return probeRegistry.list();
    }

    @GetMapping("/capabilities")
    public List<RuntimeCapabilityDescriptor> getCapabilities() {
        return capabilityRegistry.capabilities();
    }

    @GetMapping("/tools")
    public List<RuntimeToolDescriptor> getTools() {
        return capabilityRegistry.tools();
    }

    @GetMapping("/tools/schema")
    public List<RuntimeToolSchemaDescriptor> getToolSchemas() {
        return capabilityRegistry.toolSchemas();
    }

    @PostMapping("/tools/{toolName}:invoke")
    public Object invokeTool(
            @PathVariable String toolName,
            @RequestBody(required = false) ToolInvocationRequest request
    ) {
        return capabilityRegistry.invoke(toolName, request == null ? Map.of() : request.arguments());
    }

    @GetMapping("/probe-values")
    public List<ProbeValueRecord> getProbeValues(
            @RequestParam(required = false) String probeId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return graphStore.findProbeValues(applicationId(), probeId, limit);
    }

    private String applicationId() {
        return properties.getApplicationId();
    }
}
