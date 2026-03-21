package io.springlens.demo;

import io.springlens.model.RuntimeCapabilityDescriptor;
import io.springlens.model.RuntimeToolDescriptor;
import io.springlens.model.RuntimeToolSchemaDescriptor;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = SpringLensDemoApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.lens.registration-enabled=false"
)
class SpringLensCapabilityIntegrationTests {

    @LocalServerPort
    private int port;

    @Test
    void discoversUserDefinedCapabilityAndInvokesItsTool() {
        List<RuntimeCapabilityDescriptor> capabilities = restClient().get()
                .uri("http://localhost:" + port + "/internal/spring-lens/capabilities")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        List<RuntimeToolDescriptor> tools = restClient().get()
                .uri("http://localhost:" + port + "/internal/spring-lens/tools")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        List<RuntimeToolSchemaDescriptor> schemas = restClient().get()
                .uri("http://localhost:" + port + "/internal/spring-lens/tools/schema")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        Map<String, Object> result = restClient().post()
                .uri("http://localhost:" + port + "/internal/spring-lens/tools/count_orders_by_status:invoke")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("arguments", Map.of("status", "PAID")))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        assertEquals(List.of("demo.orders"), capabilities.stream()
                .map(RuntimeCapabilityDescriptor::id)
                .toList());
        assertEquals(List.of("count_orders_by_status"), tools.stream()
                .map(RuntimeToolDescriptor::name)
                .toList());
        assertEquals(List.of("count_orders_by_status"), schemas.stream()
                .map(RuntimeToolSchemaDescriptor::name)
                .toList());
        assertEquals("demo.orders", schemas.getFirst().capabilityId());
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schemas.getFirst().inputSchema().get("properties");
        assertEquals(Map.of("type", "string"), properties.get("status"));
        assertEquals("PAID", result.get("status"));
        assertEquals(1, result.get("count"));
        assertTrue(result.containsKey("capabilityId"));
    }

    private RestClient restClient() {
        return RestClient.create();
    }
}
