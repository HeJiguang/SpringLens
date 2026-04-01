package io.springlens.starter.capability;

import static org.assertj.core.api.Assertions.assertThat;

import io.springlens.model.ProbeDescriptor;
import io.springlens.model.diagnostic.ProbeCapturePhase;
import io.springlens.runtime.InMemoryExecutionGraphStore;
import io.springlens.spi.CapabilityToolGenerationRequest;
import io.springlens.spi.ControllerMappingMetadata;
import io.springlens.spi.LensCallableTool;
import io.springlens.spi.ToolRequest;
import io.springlens.starter.LensRuntimeProperties;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DefaultCapabilityToolGeneratorTest {

    @Test
    void generatesRuntimeToolsFromControllerMappingsAndRelatedProbes() {
        LensRuntimeProperties properties = new LensRuntimeProperties();
        properties.setApplicationId("orders-app");
        DefaultCapabilityToolGenerator generator = new DefaultCapabilityToolGenerator(
                new InMemoryExecutionGraphStore(16),
                properties
        );

        CapabilityToolGenerationRequest request = new CapabilityToolGenerationRequest(
                List.of(
                        new ControllerMappingMetadata(
                                "io.springlens.demo.OrderController",
                                "getOrder",
                                List.of("GET"),
                                List.of("/orders/{id}")
                        ),
                        new ControllerMappingMetadata(
                                "io.springlens.demo.OrderController",
                                "probeOrder",
                                List.of("GET"),
                                List.of("/orders/probe/{id}")
                        )
                ),
                List.of(
                        new ProbeDescriptor("order.lookup.result", "Observe repository lookup result", "annotation", ProbeCapturePhase.AFTER_RETURN),
                        new ProbeDescriptor("order.status", "Current order status", "manual", ProbeCapturePhase.MANUAL),
                        new ProbeDescriptor("payment.status", "Payment trace", "manual", ProbeCapturePhase.MANUAL)
                )
        );

        List<LensCallableTool> generatedTools = generator.generate(request);

        assertThat(generatedTools).extracting(tool -> tool.metadata().name())
                .contains("trace_order_flow", "query_order_status");

        LensCallableTool traceTool = generatedTools.stream()
                .filter(tool -> tool.metadata().name().equals("trace_order_flow"))
                .findFirst()
                .orElseThrow();
        assertThat(traceTool.metadata().description()).contains("OrderController");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) traceTool.execute(new ToolRequest(null, null, Map.of("limit", 3)));

        assertThat(result).containsEntry("toolName", "trace_order_flow");
        assertThat(result).containsEntry("controllerClass", "io.springlens.demo.OrderController");
        assertThat(result).containsKey("routes");
        assertThat(result).containsKey("probeIds");
        assertThat((List<String>) result.get("probeIds"))
                .contains("order.lookup.result", "order.status")
                .doesNotContain("payment.status");

        LensCallableTool queryTool = generatedTools.stream()
                .filter(tool -> tool.metadata().name().equals("query_order_status"))
                .findFirst()
                .orElseThrow();

        @SuppressWarnings("unchecked")
        Map<String, Object> queryResult = (Map<String, Object>) queryTool.execute(new ToolRequest(null, null, Map.of("limit", 3)));

        assertThat(queryResult).containsEntry("toolName", "query_order_status");
        assertThat(queryResult).containsEntry("probeId", "order.status");
        assertThat(queryResult).containsEntry("description", "Current order status");
        assertThat(queryResult).containsKey("recentValues");
    }
}
