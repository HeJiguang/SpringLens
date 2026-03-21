package io.springlens.server.tool;

import io.springlens.model.AppRegistration;
import io.springlens.model.RuntimeToolDescriptor;
import io.springlens.model.diagnostic.ExceptionContextRecord;
import io.springlens.model.core.ExecutionContext;
import io.springlens.model.core.ExecutionGraph;
import io.springlens.model.diagnostic.ProbeCapturePhase;
import io.springlens.model.diagnostic.ProbeValueRecord;
import io.springlens.model.diagnostic.SlowSqlRecord;
import io.springlens.server.registry.ApplicationRegistryService;
import io.springlens.server.runtime.RuntimeObservationClient;
import io.springlens.spi.ToolRequest;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeToolGatewayTest {

    private ApplicationRegistryService registryService;
    private RuntimeObservationClient runtimeObservationClient;

    @BeforeEach
    void setUp() {
        registryService = new ApplicationRegistryService();
        registryService.register(new AppRegistration(
                "orders-app",
                "orders-1",
                URI.create("http://localhost:8081"),
                Instant.parse("2026-03-19T10:15:30Z"),
                Map.of()
        ));
        runtimeObservationClient = new FakeRuntimeObservationClient();
    }

    @Test
    void listsRuntimeToolsThroughRuntimeClient() {
        ListRuntimeToolsTool tool = new ListRuntimeToolsTool(registryService, runtimeObservationClient);

        Object result = tool.execute(new ToolRequest("orders-app", null, Map.of()));

        assertThat(result).asList().singleElement().satisfies(value -> {
            RuntimeToolDescriptor descriptor = (RuntimeToolDescriptor) value;
            assertThat(descriptor.name()).isEqualTo("count_orders_by_status");
            assertThat(descriptor.capabilityId()).isEqualTo("demo.orders");
        });
    }

    @Test
    void invokesRuntimeToolThroughRuntimeClient() {
        InvokeRuntimeToolTool tool = new InvokeRuntimeToolTool(registryService, runtimeObservationClient);

        Object result = tool.execute(new ToolRequest(
                "orders-app",
                null,
                Map.of("toolName", "count_orders_by_status", "arguments", Map.of("status", "PAID"))
        ));

        assertThat(result).isEqualTo(Map.of("status", "PAID", "count", 1, "capabilityId", "demo.orders"));
    }

    private static final class FakeRuntimeObservationClient implements RuntimeObservationClient {

        @Override
        public List<SlowSqlRecord> getSlowSql(AppRegistration registration, int limit, long minDurationMs) {
            return List.of();
        }

        @Override
        public List<ExceptionContextRecord> getExceptionContexts(AppRegistration registration, int limit, String exceptionClass) {
            return List.of();
        }

        @Override
        public ExecutionGraph getExecutionGraph(AppRegistration registration, String executionId) {
            return new ExecutionGraph(new ExecutionContext("orders-app", "orders-1", executionId, Instant.now(), Map.of()), List.of(), List.of());
        }

        @Override
        public List<RuntimeToolDescriptor> listRuntimeTools(AppRegistration registration) {
            return List.of(new RuntimeToolDescriptor("count_orders_by_status", "Count by status", "demo.orders"));
        }

        @Override
        public Object invokeRuntimeTool(AppRegistration registration, String toolName, Map<String, Object> arguments) {
            return Map.of("status", arguments.get("status"), "count", 1, "capabilityId", "demo.orders");
        }

        @Override
        public List<ProbeValueRecord> getProbeValues(AppRegistration registration, String probeId, int limit) {
            return List.of(new ProbeValueRecord(
                    "graph-1",
                    "node-1",
                    probeId,
                    "Local status",
                    "PAID",
                    String.class.getName(),
                    "/orders/probe/1",
                    Instant.parse("2026-03-19T10:15:30Z"),
                    "manual",
                    ProbeCapturePhase.MANUAL
            ));
        }
    }
}
