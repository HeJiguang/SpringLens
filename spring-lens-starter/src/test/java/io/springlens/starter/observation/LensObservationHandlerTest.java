package io.springlens.starter.observation;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.observation.Observation;
import io.springlens.model.core.ExecutionEntrypointKind;
import io.springlens.model.core.ExecutionOriginKind;
import io.springlens.model.core.ExecutionTransportKind;
import io.springlens.model.core.NodeType;
import io.springlens.runtime.HttpRequestCollector;
import io.springlens.runtime.InMemoryExecutionGraphStore;
import io.springlens.runtime.RuntimeSignalProcessor;
import io.springlens.starter.LensRuntimeProperties;
import io.springlens.starter.RuntimeExecutionContextHolder;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LensObservationHandlerTest {

    @Test
    void mapsServerObservationContextIntoCompletedExecutionGraph() {
        InMemoryExecutionGraphStore store = new InMemoryExecutionGraphStore(32);
        RuntimeSignalProcessor processor = new RuntimeSignalProcessor(store, List.of(new HttpRequestCollector()));
        LensRuntimeProperties properties = new LensRuntimeProperties();
        properties.setApplicationId("orders-app");
        properties.setInstanceId("orders-app-1");
        properties.setObservationNativeEnabled(true);
        properties.setCompatibilityInstrumentationEnabled(false);

        LensObservationContextAccessor accessor = new LensObservationContextAccessor();
        LensObservationExecutionBridge bridge = new LensObservationExecutionBridge(
                processor,
                new RuntimeExecutionContextHolder(),
                properties,
                new StandardEnvironment(),
                accessor
        );
        LensObservationHandler handler = new LensObservationHandler(bridge, accessor);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/orders/42");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(204);
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, response);
        context.setPathPattern("/orders/{id}");
        context.put(LensObservationContextAccessor.TRACE_ID_KEY, "trace-123");
        context.put(LensObservationContextAccessor.SPAN_ID_KEY, "span-456");
        context.put(LensObservationContextAccessor.PARENT_SPAN_ID_KEY, "span-root");

        handler.onStart(context);
        handler.onStop(context);

        String executionId = accessor.executionId(context);
        var graph = store.findGraph(executionId).orElseThrow();

        assertThat(graph.context().traceId()).isEqualTo("trace-123");
        assertThat(graph.context().spanId()).isEqualTo("span-456");
        assertThat(graph.context().parentSpanId()).isEqualTo("span-root");
        assertThat(graph.context().entrypointKind()).isEqualTo(ExecutionEntrypointKind.HTTP_SERVER);
        assertThat(graph.context().transportKind()).isEqualTo(ExecutionTransportKind.HTTP);
        assertThat(graph.context().captureMode()).isEqualTo("observation-native");
        assertThat(graph.context().tags()).containsEntry("path", "/orders/{id}");
        assertThat(graph.nodes()).filteredOn(node -> NodeType.HTTP_REQUEST.equals(node.type()))
                .singleElement()
                .satisfies(node -> {
                    assertThat(node.originKind()).isEqualTo(ExecutionOriginKind.OBSERVATION);
                    assertThat(node.attributes()).containsEntry("status", 204);
                });
    }

    @Test
    void enablesObservationNativeByDefaultAndAllowsCompatToggle() {
        LensRuntimeProperties properties = new LensRuntimeProperties();

        assertThat(properties.isObservationNativeEnabled()).isTrue();
        assertThat(properties.isCompatibilityInstrumentationEnabled()).isTrue();

        properties.setCompatibilityInstrumentationEnabled(false);

        assertThat(properties.isCompatibilityInstrumentationEnabled()).isFalse();
    }

    @Test
    void ignoresNonServerObservationContexts() {
        LensObservationHandler handler = new LensObservationHandler(
                new LensObservationExecutionBridge(
                        new RuntimeSignalProcessor(new InMemoryExecutionGraphStore(4), List.of(new HttpRequestCollector())),
                        new RuntimeExecutionContextHolder(),
                        new LensRuntimeProperties(),
                        new StandardEnvironment(),
                        new LensObservationContextAccessor()
                ),
                new LensObservationContextAccessor()
        );

        assertThat(handler.supportsContext(new Observation.Context())).isFalse();
    }
}
