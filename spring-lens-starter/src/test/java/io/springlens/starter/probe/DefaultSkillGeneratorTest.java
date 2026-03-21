package io.springlens.starter.probe;

import static org.assertj.core.api.Assertions.assertThat;

import io.springlens.model.ProbeDescriptor;
import io.springlens.model.diagnostic.ProbeCapturePhase;
import io.springlens.runtime.InMemoryExecutionGraphStore;
import io.springlens.spi.ControllerMappingMetadata;
import io.springlens.spi.GeneratedSkillTool;
import io.springlens.spi.SkillGenerationRequest;
import io.springlens.starter.LensRuntimeProperties;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DefaultSkillGeneratorTest {

    @Test
    void generatesTraceToolFromControllerMappingsAndRelatedProbes() {
        LensRuntimeProperties properties = new LensRuntimeProperties();
        properties.setApplicationId("orders-app");
        DefaultSkillGenerator generator = new DefaultSkillGenerator(new InMemoryExecutionGraphStore(16), properties);

        SkillGenerationRequest request = new SkillGenerationRequest(
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

        List<GeneratedSkillTool> generatedTools = generator.generate(request);

        assertThat(generatedTools).extracting(GeneratedSkillTool::name)
                .contains("trace_order_flow", "query_order_status");

        GeneratedSkillTool traceTool = generatedTools.stream()
                .filter(tool -> tool.name().equals("trace_order_flow"))
                .findFirst()
                .orElseThrow();
        assertThat(traceTool.description()).contains("OrderController");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) traceTool.invoke(Map.of("limit", 3));

        assertThat(result).containsEntry("toolName", "trace_order_flow");
        assertThat(result).containsEntry("controllerClass", "io.springlens.demo.OrderController");
        assertThat(result).containsKey("routes");
        assertThat(result).containsKey("probeIds");
        assertThat((List<String>) result.get("probeIds"))
                .contains("order.lookup.result", "order.status")
                .doesNotContain("payment.status");

        GeneratedSkillTool queryTool = generatedTools.stream()
                .filter(tool -> tool.name().equals("query_order_status"))
                .findFirst()
                .orElseThrow();

        @SuppressWarnings("unchecked")
        Map<String, Object> queryResult = (Map<String, Object>) queryTool.invoke(Map.of("limit", 3));

        assertThat(queryResult).containsEntry("toolName", "query_order_status");
        assertThat(queryResult).containsEntry("probeId", "order.status");
        assertThat(queryResult).containsEntry("description", "Current order status");
        assertThat(queryResult).containsKey("recentValues");
    }
}
