package io.springlens.spi;

import io.springlens.model.ProbeDescriptor;
import io.springlens.model.diagnostic.ProbeCapturePhase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LensCapabilityContractTest {

    @Test
    void exposesDescriptorToolsAndProbesThroughOneContract() {
        LensCallableTool tool = new SampleTool();
        LensCapability capability = new SampleCapability(tool);

        CapabilityContribution contribution = capability.contribute();

        assertEquals("demo.orders", contribution.descriptor().id());
        assertEquals(CapabilityKind.OBSERVABILITY, contribution.descriptor().kind());
        assertEquals(CapabilitySource.APPLICATION, contribution.descriptor().source());
        assertEquals(List.of("count_orders"), contribution.tools().stream()
                .map(toolItem -> toolItem.metadata().name())
                .toList());
        assertEquals(List.of("order.status"), contribution.probes().stream()
                .map(ProbeDescriptor::probeId)
                .toList());
    }

    private static final class SampleCapability implements LensCapability {

        private final LensCallableTool tool;

        private SampleCapability(LensCallableTool tool) {
            this.tool = tool;
        }

        @Override
        public CapabilityContribution contribute() {
            return new CapabilityContribution(
                    new CapabilityDescriptor(
                            "demo.orders",
                            "Order Capability",
                            "Expose order runtime insights.",
                            CapabilityKind.OBSERVABILITY,
                            CapabilitySource.APPLICATION
                    ),
                    List.of(new ProbeDescriptor("order.status", "Current order status", "OrderCapability", ProbeCapturePhase.MANUAL)),
                    List.of(tool)
            );
        }
    }

    private static final class SampleTool implements LensCallableTool {

        @Override
        public ToolMetadata metadata() {
            return ToolMetadata.publiclyExposed("count_orders", "Count orders grouped by status.");
        }

        @Override
        public Object execute(ToolRequest request) {
            return Map.of("count", 1);
        }
    }
}
