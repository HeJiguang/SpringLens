package io.springlens.demo;

import io.springlens.model.ProjectToolDescriptor;
import io.springlens.model.ProjectToolSchemaDescriptor;
import io.springlens.model.ProjectToolSourceType;
import io.springlens.model.ProbeDescriptor;
import io.springlens.model.diagnostic.ExceptionContextRecord;
import io.springlens.model.diagnostic.ProbeValueRecord;
import io.springlens.model.diagnostic.SlowSqlRecord;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = SpringLensDemoApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.lens.registration-enabled=false"
)
class SpringLensProbeIntegrationTests {

    @LocalServerPort
    private int port;

    @Test
    void exposesRegisteredProbesAndCapturedValues() {
        List<ProbeDescriptor> probes = restClient().get()
                .uri("http://localhost:" + port + "/internal/spring-lens/probes")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        assertThat(probes).extracting(ProbeDescriptor::probeId)
                .contains("order.lookup.result");

        Map<String, Object> response = restClient().get()
                .uri("http://localhost:" + port + "/orders/probe/1")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        List<ProbeValueRecord> watchedValues = restClient().get()
                .uri("http://localhost:" + port + "/internal/spring-lens/probe-values?probeId=order.lookup.result&limit=5")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        List<ProbeValueRecord> statusValues = restClient().get()
                .uri("http://localhost:" + port + "/internal/spring-lens/probe-values?probeId=order.status&limit=5")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        List<ProbeValueRecord> customerValues = restClient().get()
                .uri("http://localhost:" + port + "/internal/spring-lens/probe-values?probeId=order.customer_name&limit=5")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        List<ProbeValueRecord> summaryValues = restClient().get()
                .uri("http://localhost:" + port + "/internal/spring-lens/probe-values?probeId=order.summary&limit=5")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        assertThat(response).containsEntry("probeDemo", "order");
        assertThat(response).containsKeys("highlights", "timeline");
        assertThat(watchedValues).isNotEmpty();
        assertThat(watchedValues.getFirst().probeId()).isEqualTo("order.lookup.result");
        assertThat(statusValues).isNotEmpty();
        assertThat(statusValues.getFirst().probeId()).isEqualTo("order.status");
        assertThat(customerValues).isNotEmpty();
        assertThat(customerValues.getFirst().probeId()).isEqualTo("order.customer_name");
        assertThat(summaryValues).isNotEmpty();
        assertThat(summaryValues.getFirst().probeId()).isEqualTo("order.summary");
    }

    @Test
    void exposesAndInvokesProjectTools() {
        List<ProjectToolDescriptor> tools = restClient().get()
                .uri("http://localhost:" + port + "/internal/spring-lens/project-tools")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        assertThat(tools).extracting(ProjectToolDescriptor::name)
                .contains("count_orders_by_status", "diagnose_request", "trace_order_flow");

        Map<String, Object> response = restClient().post()
                .uri("http://localhost:" + port + "/internal/spring-lens/project-tools/count_orders_by_status:invoke")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("arguments", Map.of("status", "PAID")))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        assertThat(response).containsEntry("status", "PAID");
        assertThat(response).containsEntry("count", 1);

        restClient().get()
                .uri("http://localhost:" + port + "/orders/probe/1")
                .retrieve()
                .toBodilessEntity();

        List<ProjectToolDescriptor> toolsAfterProbe = restClient().get()
                .uri("http://localhost:" + port + "/internal/spring-lens/project-tools")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        assertThat(toolsAfterProbe).extracting(ProjectToolDescriptor::name)
                .contains("query_order_status", "query_order_customer_name", "query_order_summary");

        Map<String, Object> generatedToolResponse = restClient().post()
                .uri("http://localhost:" + port + "/internal/spring-lens/project-tools/trace_order_flow:invoke")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("arguments", Map.of("limit", 5)))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        assertThat(generatedToolResponse).containsEntry("toolName", "trace_order_flow");
        assertThat(generatedToolResponse).containsEntry("controllerClass", "io.springlens.demo.OrderController");
        assertThat((List<String>) generatedToolResponse.get("probeIds"))
                .contains("order.lookup.result", "order.status");

        Map<String, Object> queryProbeToolResponse = restClient().post()
                .uri("http://localhost:" + port + "/internal/spring-lens/project-tools/query_order_status:invoke")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("arguments", Map.of("limit", 5)))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        assertThat(queryProbeToolResponse).containsEntry("toolName", "query_order_status");
        assertThat(queryProbeToolResponse).containsEntry("probeId", "order.status");
        assertThat(queryProbeToolResponse).containsEntry("description", "Current order status");
        assertThat(queryProbeToolResponse).containsKey("recentValues");

        Map<String, Object> querySummaryToolResponse = restClient().post()
                .uri("http://localhost:" + port + "/internal/spring-lens/project-tools/query_order_summary:invoke")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("arguments", Map.of("limit", 5)))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        assertThat(querySummaryToolResponse).containsEntry("probeId", "order.summary");
        assertThat(querySummaryToolResponse).containsKey("latestValue");
    }

    @Test
    void exposesProjectToolSchemasWithoutBreakingLegacyList() {
        List<ProjectToolDescriptor> legacyTools = restClient().get()
                .uri("http://localhost:" + port + "/internal/spring-lens/project-tools")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        assertThat(legacyTools).extracting(ProjectToolDescriptor::name)
                .contains("count_orders_by_status", "diagnose_request", "trace_order_flow");

        restClient().get()
                .uri("http://localhost:" + port + "/orders/probe/1")
                .retrieve()
                .toBodilessEntity();

        List<ProjectToolSchemaDescriptor> schemas = restClient().get()
                .uri("http://localhost:" + port + "/internal/spring-lens/project-tools/schema")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        ProjectToolSchemaDescriptor handwritten = schemas.stream()
                .filter(schema -> schema.name().equals("count_orders_by_status"))
                .findFirst()
                .orElseThrow();
        assertThat(handwritten.generated()).isFalse();
        assertThat(handwritten.sourceType()).isEqualTo(ProjectToolSourceType.HANDWRITTEN);
        assertThat(((Map<?, ?>) handwritten.inputSchema().get("properties")).get("status"))
                .isEqualTo(Map.of("type", "string"));

        ProjectToolSchemaDescriptor trace = schemas.stream()
                .filter(schema -> schema.name().equals("trace_order_flow"))
                .findFirst()
                .orElseThrow();
        assertThat(trace.generated()).isTrue();
        assertThat(trace.sourceType()).isEqualTo(ProjectToolSourceType.GENERATED_TRACE);
        assertThat(((Map<?, ?>) trace.inputSchema().get("properties")).get("executionId"))
                .isEqualTo(Map.of("type", "string"));

        ProjectToolSchemaDescriptor probe = schemas.stream()
                .filter(schema -> schema.name().equals("query_order_status"))
                .findFirst()
                .orElseThrow();
        assertThat(probe.generated()).isTrue();
        assertThat(probe.sourceType()).isEqualTo(ProjectToolSourceType.GENERATED_PROBE);
        assertThat(((Map<?, ?>) probe.inputSchema().get("properties")).get("limit"))
                .isEqualTo(Map.of("type", "integer"));

        assertThat(schemas).extracting(ProjectToolSchemaDescriptor::name)
                .contains("query_order_customer_name", "query_order_summary");
    }

    @Test
    void showcasesSlowEndpointAndCapturesSlowSqlEvidence() {
        Map<String, Object> response = restClient().get()
                .uri("http://localhost:" + port + "/orders/slow")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        List<SlowSqlRecord> slowSql = restClient().get()
                .uri("http://localhost:" + port + "/internal/spring-lens/slow-sql?limit=5&minDurationMs=10")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        assertThat(response).containsEntry("scenario", "slow-sql-showcase");
        assertThat(response).containsKeys("highlightedOrder", "statusBreakdown", "sleepMs");
        assertThat(slowSql).isNotEmpty();
        assertThat(slowSql.getFirst().requestPath()).isEqualTo("/orders/slow");
    }

    @Test
    void invokesBuiltInDiagnoseRequestTool() {
        restClient().get()
                .uri("http://localhost:" + port + "/orders/fail")
                .exchange((request, response) -> response.getStatusCode());

        List<ExceptionContextRecord> exceptions = restClient().get()
                .uri("http://localhost:" + port + "/internal/spring-lens/exception-context?limit=1")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        Map<String, Object> response = restClient().post()
                .uri("http://localhost:" + port + "/internal/spring-lens/project-tools/diagnose_request:invoke")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("arguments", Map.of("executionId", exceptions.getFirst().graphId())))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        assertThat(response).containsEntry("rootCause", "Inventory reservation failed for order 5");
        assertThat(((Number) response.get("confidence")).doubleValue()).isEqualTo(0.9);
    }

    @Test
    void exposesFailureEndpointWithRichExceptionContext() {
        var statusCode = restClient().get()
                .uri("http://localhost:" + port + "/orders/fail")
                .exchange((request, response) -> response.getStatusCode());

        List<ExceptionContextRecord> exceptions = restClient().get()
                .uri("http://localhost:" + port + "/internal/spring-lens/exception-context?limit=1")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        assertThat(statusCode.value()).isEqualTo(500);
        assertThat(exceptions).isNotEmpty();
        assertThat(exceptions.getFirst().requestPath()).isEqualTo("/orders/fail");
        assertThat(exceptions.getFirst().message()).contains("Inventory reservation failed for order 5");
    }

    private RestClient restClient() {
        return RestClient.create();
    }
}
