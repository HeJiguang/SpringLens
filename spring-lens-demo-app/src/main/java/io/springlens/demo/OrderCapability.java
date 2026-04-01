package io.springlens.demo;

import io.springlens.model.ProbeDescriptor;
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
import org.springframework.stereotype.Component;

@Component
public class OrderCapability implements LensCapability {

    static final String CAPABILITY_ID = "demo.orders";

    private final OrderRepository orderRepository;

    public OrderCapability(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public CapabilityContribution contribute() {
        return new CapabilityContribution(
                new CapabilityDescriptor(
                        CAPABILITY_ID,
                        "Order Capability",
                        "Expose order runtime probes and callable runtime tools.",
                        CapabilityKind.OBSERVABILITY,
                        CapabilitySource.APPLICATION
                ),
                List.of(
                        new ProbeDescriptor("order.status", "Current order status", "OrderProbeService", ProbeCapturePhase.MANUAL),
                        new ProbeDescriptor("order.customer_name", "Customer name used by this order", "OrderProbeService", ProbeCapturePhase.MANUAL),
                        new ProbeDescriptor("order.summary", "Human-readable order summary", "OrderProbeService", ProbeCapturePhase.MANUAL)
                ),
                List.of(new SummarizeOrderStatusesTool(orderRepository))
        );
    }

    private static final class SummarizeOrderStatusesTool implements LensCallableTool {

        private final OrderRepository orderRepository;

        private SummarizeOrderStatusesTool(OrderRepository orderRepository) {
            this.orderRepository = orderRepository;
        }

        @Override
        public ToolMetadata metadata() {
            return ToolMetadata.publiclyExposed(
                    "summarize_order_statuses",
                    "Summarize current order counts grouped by status."
            );
        }

        @Override
        public Object execute(ToolRequest request) {
            return Map.of(
                    "capabilityId", CAPABILITY_ID,
                    "statusBreakdown", orderRepository.statusBreakdown()
            );
        }
    }
}
