package io.springlens.server.runtime;

import io.springlens.model.AppRegistration;
import io.springlens.model.RuntimeToolDescriptor;
import io.springlens.model.RuntimeToolSchemaDescriptor;
import io.springlens.model.core.ExecutionGraph;
import io.springlens.model.diagnostic.ExceptionContextRecord;
import io.springlens.model.diagnostic.ProbeValueRecord;
import io.springlens.model.diagnostic.SlowSqlRecord;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class HttpRuntimeObservationClient implements RuntimeObservationClient {

    private final RestClient restClient;

    public HttpRuntimeObservationClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public List<SlowSqlRecord> getSlowSql(AppRegistration registration, int limit, long minDurationMs) {
        return restClient.get()
                .uri(UriComponentsBuilder.fromUri(registration.runtimeBaseUrl())
                        .path("/internal/spring-lens/slow-sql")
                        .queryParam("limit", limit)
                        .queryParam("minDurationMs", minDurationMs)
                        .build(true)
                        .toUri())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    @Override
    public List<ExceptionContextRecord> getExceptionContexts(AppRegistration registration, int limit, String exceptionClass) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUri(registration.runtimeBaseUrl())
                .path("/internal/spring-lens/exception-context")
                .queryParam("limit", limit);
        if (exceptionClass != null && !exceptionClass.isBlank()) {
            builder.queryParam("exceptionClass", exceptionClass);
        }
        return restClient.get()
                .uri(builder.build(true).toUri())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    @Override
    public ExecutionGraph getExecutionGraph(AppRegistration registration, String executionId) {
        return restClient.get()
                .uri(UriComponentsBuilder.fromUri(registration.runtimeBaseUrl())
                        .path("/internal/spring-lens/graphs/{executionId}")
                        .build(executionId))
                .retrieve()
                .body(ExecutionGraph.class);
    }

    @Override
    public List<RuntimeToolDescriptor> listRuntimeTools(AppRegistration registration) {
        return restClient.get()
                .uri(UriComponentsBuilder.fromUri(registration.runtimeBaseUrl())
                        .path("/internal/spring-lens/tools")
                        .build(true)
                        .toUri())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    @Override
    public List<RuntimeToolSchemaDescriptor> listRuntimeToolSchemas(AppRegistration registration) {
        return restClient.get()
                .uri(UriComponentsBuilder.fromUri(registration.runtimeBaseUrl())
                        .path("/internal/spring-lens/tools/schema")
                        .build(true)
                        .toUri())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    @Override
    public Object invokeRuntimeTool(AppRegistration registration, String toolName, Map<String, Object> arguments) {
        return restClient.post()
                .uri(UriComponentsBuilder.fromUri(registration.runtimeBaseUrl())
                        .path("/internal/spring-lens/tools/{toolName}:invoke")
                        .build(toolName))
                .body(Map.of("arguments", arguments == null ? Map.of() : arguments))
                .retrieve()
                .body(Object.class);
    }

    @Override
    public List<ProbeValueRecord> getProbeValues(AppRegistration registration, String probeId, int limit) {
        return restClient.get()
                .uri(UriComponentsBuilder.fromUri(registration.runtimeBaseUrl())
                        .path("/internal/spring-lens/probe-values")
                        .queryParam("probeId", probeId)
                        .queryParam("limit", limit)
                        .build(true)
                        .toUri())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }
}
