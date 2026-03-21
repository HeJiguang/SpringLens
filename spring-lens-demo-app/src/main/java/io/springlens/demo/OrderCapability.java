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
import io.springlens.spi.ToolSchema;
import java.util.LinkedHashMap;
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
                        new ProbeDescriptor("order.status", "Current order status", "OrderProbeService", ProbeCapturePhase.MANUAL)
                ),
                List.of(new CountOrdersByStatusTool(orderRepository))
        );
    }

    private static final class CountOrdersByStatusTool implements LensCallableTool {

        private final OrderRepository orderRepository;

        private CountOrdersByStatusTool(OrderRepository orderRepository) {
            this.orderRepository = orderRepository;
        }

        @Override
        public ToolMetadata metadata() {
            return ToolMetadata.publiclyExposed("count_orders_by_status", "Count orders grouped by a single status.");
        }

        @Override
        public ToolSchema schema() {
            Map<String, Object> inputProperties = new LinkedHashMap<>();
            inputProperties.put("status", Map.of("type", "string"));
            Map<String, Object> outputProperties = new LinkedHashMap<>();
            outputProperties.put("capabilityId", Map.of("type", "string"));
            outputProperties.put("status", Map.of("type", "string"));
            outputProperties.put("count", Map.of("type", "integer"));
            return new ToolSchema(
                    metadata().name(),
                    metadata().description(),
                    Map.of(
                            "$schema", "https://json-schema.org/draft/2020-12/schema",
                            "type", "object",
                            "properties", inputProperties,
                            "required", List.of("status"),
                            "additionalProperties", false
                    ),
                    Map.of(
                            "$schema", "https://json-schema.org/draft/2020-12/schema",
                            "type", "object",
                            "properties", outputProperties,
                            "required", List.of("capabilityId", "status", "count"),
                            "additionalProperties", false
                    )
            );
        }

        @Override
        public Object execute(ToolRequest request) {
            String status = String.valueOf(request.arguments().get("status"));
            return Map.of(
                    "capabilityId", CAPABILITY_ID,
                    "status", status,
                    "count", orderRepository.countByStatus(status)
            );
        }
    }
}
