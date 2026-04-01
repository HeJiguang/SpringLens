package io.springlens.starter.capability;

import static org.assertj.core.api.Assertions.assertThat;

import io.springlens.spi.ToolSchema;
import io.springlens.starter.probe.LensTool;
import io.springlens.starter.probe.LensToolParam;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RuntimeToolSchemaFactoryTest {

    @Test
    void createsGenericSchemaForHandwrittenLensToolMethod() throws Exception {
        Method method = SampleTools.class.getMethod("countOrdersByStatus", String.class);

        ToolSchema schema = RuntimeToolSchemaFactory.handwrittenToolSchema(
                method,
                "count_orders_by_status",
                "Count orders grouped by status."
        );

        assertThat(schema.name()).isEqualTo("count_orders_by_status");
        assertThat(schema.inputSchema()).containsEntry("type", "object");
        assertThat(((Map<?, ?>) schema.inputSchema().get("properties")).get("status"))
                .isEqualTo(Map.of("type", "string"));
        assertThat(schema.outputSchema()).containsEntry("type", "object");
        assertThat(schema.outputSchema()).containsEntry("additionalProperties", true);
    }

    static final class SampleTools {

        @LensTool(name = "count_orders_by_status", description = "Count orders grouped by status.")
        public Map<String, Object> countOrdersByStatus(@LensToolParam("status") String status) {
            return Map.of("status", status, "count", 1);
        }
    }
}
