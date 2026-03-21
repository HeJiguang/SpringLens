package io.springlens.starter.probe;

import static org.assertj.core.api.Assertions.assertThat;

import io.springlens.model.ProjectToolSchemaDescriptor;
import io.springlens.model.ProjectToolSourceType;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProjectToolSchemaFactoryTest {

    @Test
    void createsGenericSchemaForHandwrittenLensToolMethod() throws Exception {
        Method method = SampleTools.class.getMethod("countOrdersByStatus", String.class);

        ProjectToolSchemaDescriptor descriptor = ProjectToolSchemaFactory.handwrittenToolSchema(
                method,
                "count_orders_by_status",
                "Count orders grouped by status."
        );

        assertThat(descriptor.name()).isEqualTo("count_orders_by_status");
        assertThat(descriptor.generated()).isFalse();
        assertThat(descriptor.sourceType()).isEqualTo(ProjectToolSourceType.HANDWRITTEN);
        assertThat(descriptor.inputSchema()).containsEntry("type", "object");
        assertThat(((Map<?, ?>) descriptor.inputSchema().get("properties")).get("status"))
                .isEqualTo(Map.of("type", "string"));
        assertThat(descriptor.outputSchema()).containsEntry("type", "object");
        assertThat(descriptor.outputSchema()).containsEntry("additionalProperties", true);
    }

    static final class SampleTools {

        @LensTool(name = "count_orders_by_status", description = "Count orders grouped by status.")
        public Map<String, Object> countOrdersByStatus(@LensToolParam("status") String status) {
            return Map.of("status", status, "count", 1);
        }
    }
}
