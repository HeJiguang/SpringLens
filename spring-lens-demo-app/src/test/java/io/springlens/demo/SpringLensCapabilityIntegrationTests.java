package io.springlens.demo;

import io.springlens.model.RuntimeCapabilityDescriptor;
import io.springlens.model.RuntimeToolDescriptor;
import io.springlens.model.RuntimeToolSchemaDescriptor;
import io.springlens.model.core.ExecutionGraph;
import io.springlens.model.core.ExecutionOriginKind;
import io.springlens.model.core.ExecutionEntrypointKind;
import io.springlens.model.core.ExecutionTransportKind;
import io.springlens.model.core.NodeType;
import io.springlens.model.diagnostic.ExceptionContextRecord;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        classes = SpringLensDemoApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.lens.registration-enabled=false"
)
class SpringLensCapabilityIntegrationTests {

    @LocalServerPort
    private int port;

    @Test
    void discoversCustomAndFirstPartyCapabilitiesThroughRuntimeCatalogs() {
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

        assertThat(capabilities).extracting(RuntimeCapabilityDescriptor::id)
                .contains(
                        "demo.orders",
                        "spring-lens.annotation-tools",
                        "spring-lens.generated-tools",
                        "spring-lens.diagnosis",
                        "spring-lens.runtime-safety"
                );
        assertThat(tools).extracting(RuntimeToolDescriptor::name)
                .contains(
                        "summarize_order_statuses",
                        "count_orders_by_status",
                        "trace_order_flow",
                        "query_order_status",
                        "diagnose_request",
                        "inspect_runtime_safety",
                        "plan_runtime_safety_remediation"
                );
        assertThat(schemas).filteredOn(schema -> schema.name().equals("count_orders_by_status"))
                .singleElement()
                .extracting(item -> ((RuntimeToolSchemaDescriptor) item).capabilityId())
                .isEqualTo("spring-lens.annotation-tools");
        assertThat(schemas).filteredOn(schema -> schema.name().equals("trace_order_flow"))
                .singleElement()
                .extracting(item -> ((RuntimeToolSchemaDescriptor) item).capabilityId())
                .isEqualTo("spring-lens.generated-tools");
    }

    @Test
    void invokesRuntimeToolsAcrossCustomAnnotationGeneratedAndDiagnosisCapabilities() {
        Map<String, Object> customResult = restClient().post()
                .uri("http://localhost:" + port + "/internal/spring-lens/tools/summarize_order_statuses:invoke")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("arguments", Map.of()))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        Map<String, Object> annotationResult = restClient().post()
                .uri("http://localhost:" + port + "/internal/spring-lens/tools/count_orders_by_status:invoke")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("arguments", Map.of("status", "PAID")))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        restClient().get()
                .uri("http://localhost:" + port + "/orders/probe/1")
                .retrieve()
                .toBodilessEntity();

        Map<String, Object> generatedResult = restClient().post()
                .uri("http://localhost:" + port + "/internal/spring-lens/tools/trace_order_flow:invoke")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("arguments", Map.of("limit", 5)))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        restClient().get()
                .uri("http://localhost:" + port + "/orders/fail")
                .exchange((request, response) -> response.getStatusCode());

        List<ExceptionContextRecord> exceptions = restClient().get()
                .uri("http://localhost:" + port + "/internal/spring-lens/exception-context?limit=1")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        ExecutionGraph executionGraph = restClient().get()
                .uri("http://localhost:" + port + "/internal/spring-lens/graphs/" + exceptions.getFirst().graphId())
                .retrieve()
                .body(ExecutionGraph.class);

        Map<String, Object> diagnosis = restClient().post()
                .uri("http://localhost:" + port + "/internal/spring-lens/tools/diagnose_request:invoke")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("arguments", Map.of("executionId", exceptions.getFirst().graphId())))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        Map<String, Object> runtimeSafety = restClient().post()
                .uri("http://localhost:" + port + "/internal/spring-lens/tools/inspect_runtime_safety:invoke")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("arguments", Map.of("maxFindings", 10)))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        Map<String, Object> runtimeSafetyPlan = restClient().post()
                .uri("http://localhost:" + port + "/internal/spring-lens/tools/plan_runtime_safety_remediation:invoke")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("arguments", Map.of("maxFindings", 10)))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        assertThat(customResult).containsEntry("capabilityId", "demo.orders");
        assertThat(customResult).containsKey("statusBreakdown");
        assertThat(annotationResult).containsEntry("status", "PAID");
        assertThat(annotationResult).containsEntry("count", 1);
        assertThat(generatedResult).containsEntry("toolName", "trace_order_flow");
        assertThat(generatedResult).containsEntry("controllerClass", "io.springlens.demo.OrderController");
        assertThat(executionGraph.context().traceId()).isEqualTo(exceptions.getFirst().graphId());
        assertThat(executionGraph.context().entrypointKind()).isEqualTo(ExecutionEntrypointKind.HTTP_SERVER);
        assertThat(executionGraph.context().transportKind()).isEqualTo(ExecutionTransportKind.HTTP);
        assertThat(executionGraph.context().captureMode()).isEqualTo("compatibility");
        assertThat(executionGraph.nodes()).filteredOn(node -> NodeType.HTTP_REQUEST.equals(node.type()))
                .singleElement()
                .satisfies(node -> assertThat(node.originKind()).isEqualTo(ExecutionOriginKind.COMPAT_FILTER));
        assertThat(diagnosis).containsEntry("rootCause", "Inventory reservation failed for order 5");
        assertThat(runtimeSafety).containsEntry("capabilityId", "spring-lens.runtime-safety");
        assertThat(((Number) runtimeSafety.get("findingCount")).intValue()).isGreaterThanOrEqualTo(7);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> runtimeSafetyFindings = (List<Map<String, Object>>) runtimeSafety.get("findings");
        assertThat(runtimeSafetyFindings)
                .extracting(item -> String.valueOf(item.get("ruleId")))
                .contains(
                        "singleton-thread-local-field",
                        "singleton-manual-executor-field",
                        "singleton-non-thread-safe-collection",
                        "singleton-thread-unsafe-formatter",
                        "singleton-async-threadlocal-context-risk",
                        "singleton-unbounded-queue-field",
                        "singleton-non-atomic-counter-field"
                );
        assertThat(runtimeSafetyPlan).containsEntry("capabilityId", "spring-lens.runtime-safety");
        assertThat(((Number) runtimeSafetyPlan.get("overlaySuggestionCount")).intValue()).isGreaterThanOrEqualTo(2);
        assertThat(((Number) runtimeSafetyPlan.get("patchSuggestionCount")).intValue()).isGreaterThanOrEqualTo(5);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> patchSuggestions = (List<Map<String, Object>>) runtimeSafetyPlan.get("patchSuggestions");
        assertThat(patchSuggestions)
                .extracting(item -> String.valueOf(item.get("templateId")))
                .contains(
                        "replace-threadlocal-state",
                        "shutdown-manual-executor",
                        "replace-with-concurrent-collection",
                        "replace-simpledateformat-with-datetimeformatter",
                        "replace-counter-with-atomic"
                );
    }

    @Test
    void rejectsLegacyProjectToolEndpoints() {
        assertThatThrownBy(() -> restClient().get()
                .uri("http://localhost:" + port + "/internal/spring-lens/project-tools")
                .retrieve()
                .toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.NotFound.class);

        assertThatThrownBy(() -> restClient().post()
                .uri("http://localhost:" + port + "/internal/spring-lens/project-tools/count_orders_by_status:invoke")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("arguments", Map.of("status", "PAID")))
                .retrieve()
                .toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.NotFound.class);
    }

    private RestClient restClient() {
        return RestClient.create();
    }
}
