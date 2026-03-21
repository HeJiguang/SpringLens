package io.springlens.starter.capability;

import io.springlens.model.ProbeDescriptor;
import io.springlens.model.RuntimeCapabilityDescriptor;
import io.springlens.model.RuntimeToolDescriptor;
import io.springlens.model.RuntimeToolSchemaDescriptor;
import io.springlens.model.diagnostic.ProbeCapturePhase;
import io.springlens.spi.CapabilityContribution;
import io.springlens.spi.CapabilityDescriptor;
import io.springlens.spi.CapabilityKind;
import io.springlens.spi.CapabilitySource;
import io.springlens.spi.LensCallableTool;
import io.springlens.spi.LensCapability;
import io.springlens.spi.ToolMetadata;
import io.springlens.spi.ToolRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LensCapabilityRegistryTest {

    @Test
    void aggregatesBuiltInAndUserDefinedCapabilities() {
        LensCapabilityRegistry registry = new LensCapabilityRegistry(List.of(
                new StubCapability("spring-lens.diagnosis", CapabilitySource.BUILT_IN, List.of(), List.of(new StubTool("diagnose_request"))),
                new StubCapability(
                        "demo.orders",
                        CapabilitySource.APPLICATION,
                        List.of(new ProbeDescriptor("order.status", "Current order status", "OrderCapability", ProbeCapturePhase.MANUAL)),
                        List.of(new StubTool("count_orders"))
                )
        ));

        assertEquals(List.of("demo.orders", "spring-lens.diagnosis"), registry.capabilities().stream()
                .map(RuntimeCapabilityDescriptor::id)
                .toList());
        assertEquals(List.of("count_orders", "diagnose_request"), registry.tools().stream()
                .map(RuntimeToolDescriptor::name)
                .toList());
        assertEquals(List.of("count_orders", "diagnose_request"), registry.toolSchemas().stream()
                .map(RuntimeToolSchemaDescriptor::name)
                .toList());
        assertEquals(List.of("order.status"), registry.probes().stream()
                .map(ProbeDescriptor::probeId)
                .toList());
        assertEquals(Map.of("toolName", "count_orders"), registry.invoke("count_orders", Map.of()));
    }

    private static final class StubCapability implements LensCapability {

        private final String id;
        private final CapabilitySource source;
        private final List<ProbeDescriptor> probes;
        private final List<LensCallableTool> tools;

        private StubCapability(String id, CapabilitySource source, List<ProbeDescriptor> probes, List<LensCallableTool> tools) {
            this.id = id;
            this.source = source;
            this.probes = probes;
            this.tools = tools;
        }

        @Override
        public CapabilityContribution contribute() {
            return new CapabilityContribution(
                    new CapabilityDescriptor(id, id, "Capability " + id, CapabilityKind.OBSERVABILITY, source),
                    probes,
                    tools
            );
        }
    }

    private static final class StubTool implements LensCallableTool {

        private final String name;

        private StubTool(String name) {
            this.name = name;
        }

        @Override
        public ToolMetadata metadata() {
            return ToolMetadata.publiclyExposed(name, "Tool " + name);
        }

        @Override
        public Object execute(ToolRequest request) {
            return Map.of("toolName", name);
        }
    }
}
