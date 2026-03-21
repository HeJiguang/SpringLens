package io.springlens.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RuntimeToolSchemaDescriptorTest {

    @Test
    void copiesNestedSchemaMapsImmutably() {
        Map<String, Object> nestedInput = Map.of(
                "properties", Map.of(
                        "status", Map.of("type", "string")
                ),
                "required", List.of("status")
        );
        Map<String, Object> nestedOutput = Map.of(
                "properties", Map.of(
                        "count", Map.of("type", "integer")
                )
        );

        RuntimeToolSchemaDescriptor descriptor = new RuntimeToolSchemaDescriptor(
                "count_orders_by_status",
                "Count orders by status",
                nestedInput,
                nestedOutput,
                "demo.orders"
        );

        assertEquals("count_orders_by_status", descriptor.name());
        assertEquals("demo.orders", descriptor.capabilityId());
        assertThrows(UnsupportedOperationException.class, () -> descriptor.inputSchema().put("x", "y"));
        @SuppressWarnings("unchecked")
        Map<String, Object> inputProperties = (Map<String, Object>) descriptor.inputSchema().get("properties");
        assertThrows(UnsupportedOperationException.class, () -> inputProperties.put("x", Map.of()));
        assertThrows(UnsupportedOperationException.class, () -> descriptor.outputSchema().put("x", "y"));
    }
}
