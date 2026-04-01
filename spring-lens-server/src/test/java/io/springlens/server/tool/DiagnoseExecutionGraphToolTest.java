package io.springlens.server.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.springlens.model.AppRegistration;
import io.springlens.model.RuntimeToolDescriptor;
import io.springlens.model.RuntimeToolSchemaDescriptor;
import io.springlens.model.core.ExecutionContext;
import io.springlens.model.core.ExecutionGraph;
import io.springlens.model.diagnostic.DiagnosticResult;
import io.springlens.server.registry.ApplicationRegistryService;
import io.springlens.server.runtime.RuntimeObservationClient;
import io.springlens.spi.DiagnosticEngine;
import io.springlens.spi.ToolRequest;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DiagnoseExecutionGraphToolTest {

    private ApplicationRegistryService registryService;
    private RecordingRuntimeObservationClient runtimeObservationClient;

    @BeforeEach
    void setUp() {
        registryService = new ApplicationRegistryService();
        registryService.register(new AppRegistration(
                "orders-app",
                "orders-1",
                URI.create("http://localhost:8081"),
                Instant.parse("2026-03-20T09:15:00Z"),
                Map.of()
        ));
        runtimeObservationClient = new RecordingRuntimeObservationClient();
    }

    @Test
    void diagnosesExecutionGraphReturnedByRuntimeClient() {
        DiagnosticEngine diagnosticEngine = graph -> DiagnosticResult.builder()
                .rootCause("Slow database query")
                .summary("Diagnosed " + graph.context().executionId())
                .confidence(0.88)
                .addEvidence("sql node duration 1800ms")
                .addSuggestion("inspect index coverage")
                .build();
        DiagnoseExecutionGraphTool tool = new DiagnoseExecutionGraphTool(registryService, runtimeObservationClient, diagnosticEngine);

        Object result = tool.execute(new ToolRequest(
                "orders-app",
                null,
                Map.of("executionId", "graph-22")
        ));

        assertThat(runtimeObservationClient.lastExecutionId).isEqualTo("graph-22");
        assertThat(result).isEqualTo(DiagnosticResult.builder()
                .rootCause("Slow database query")
                .summary("Diagnosed graph-22")
                .confidence(0.88)
                .addEvidence("sql node duration 1800ms")
                .addSuggestion("inspect index coverage")
                .build());
    }

    @Test
    void requiresExecutionId() {
        DiagnoseExecutionGraphTool tool = new DiagnoseExecutionGraphTool(
                registryService,
                runtimeObservationClient,
                graph -> DiagnosticResult.builder().rootCause("none").summary("none").confidence(0.1).build()
        );

        assertThatThrownBy(() -> tool.execute(new ToolRequest("orders-app", null, Map.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("executionId");
    }

    private static final class RecordingRuntimeObservationClient implements RuntimeObservationClient {

        private String lastExecutionId;

        @Override
        public List<io.springlens.model.diagnostic.SlowSqlRecord> getSlowSql(AppRegistration registration, int limit, long minDurationMs) {
            return List.of();
        }

        @Override
        public List<io.springlens.model.diagnostic.ExceptionContextRecord> getExceptionContexts(AppRegistration registration, int limit, String exceptionClass) {
            return List.of();
        }

        @Override
        public ExecutionGraph getExecutionGraph(AppRegistration registration, String executionId) {
            lastExecutionId = executionId;
            return new ExecutionGraph(
                    new ExecutionContext(registration.applicationId(), registration.instanceId(), executionId, Instant.parse("2026-03-20T09:16:00Z"), Map.of()),
                    List.of(),
                    List.of()
            );
        }

        @Override
        public List<RuntimeToolDescriptor> listRuntimeTools(AppRegistration registration) {
            return List.of();
        }

        @Override
        public List<RuntimeToolSchemaDescriptor> listRuntimeToolSchemas(AppRegistration registration) {
            return List.of();
        }

        @Override
        public Object invokeRuntimeTool(AppRegistration registration, String toolName, Map<String, Object> arguments) {
            return Map.of();
        }

        @Override
        public List<io.springlens.model.diagnostic.ProbeValueRecord> getProbeValues(AppRegistration registration, String probeId, int limit) {
            return List.of();
        }
    }
}
