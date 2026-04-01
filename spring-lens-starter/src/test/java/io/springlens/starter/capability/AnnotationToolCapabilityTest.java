package io.springlens.starter.capability;

import static org.assertj.core.api.Assertions.assertThat;

import io.springlens.spi.LensCallableTool;
import io.springlens.spi.ToolRequest;
import io.springlens.starter.probe.LensTool;
import io.springlens.starter.probe.LensToolParam;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import tools.jackson.databind.ObjectMapper;

class AnnotationToolCapabilityTest {

    @Test
    void discoversLensToolMethodsFromApplicationBeans() {
        GenericApplicationContext applicationContext = new GenericApplicationContext();
        applicationContext.registerBean(SampleTools.class);
        applicationContext.refresh();

        try {
            AnnotationToolCapability capability = new AnnotationToolCapability(applicationContext, new ObjectMapper());

            assertThat(capability.contribute().descriptor().id()).isEqualTo(AnnotationToolCapability.CAPABILITY_ID);
            LensCallableTool tool = capability.contribute().tools().getFirst();
            assertThat(tool.metadata().name()).isEqualTo("count_orders_by_status");

            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) tool.execute(new ToolRequest(null, null, Map.of("status", "PAID")));

            assertThat(result).containsEntry("status", "PAID").containsEntry("count", 1);
        }
        finally {
            applicationContext.close();
        }
    }

    static final class SampleTools {

        @LensTool(name = "count_orders_by_status", description = "Count orders grouped by status.")
        public Map<String, Object> countOrdersByStatus(@LensToolParam("status") String status) {
            return Map.of("status", status, "count", 1);
        }
    }
}
