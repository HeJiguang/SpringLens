package io.springlens.agent.starter;

import static org.assertj.core.api.Assertions.assertThat;

import io.springlens.agent.contract.AgentActionRiskLevel;
import io.springlens.agent.contract.AgentInstrumentationMode;
import io.springlens.agent.contract.ApprovalState;
import io.springlens.agent.contract.OverlayDeliverySnapshot;
import io.springlens.agent.contract.OverlaySpec;
import io.springlens.agent.contract.PolicySnapshot;
import io.springlens.agent.contract.RegisteredOverlay;
import io.springlens.model.core.ExecutionContext;
import io.springlens.model.core.ExecutionOriginKind;
import io.springlens.model.core.ExecutionGraph;
import io.springlens.model.core.NodeType;
import io.springlens.runtime.HttpRequestCollector;
import io.springlens.runtime.InMemoryExecutionGraphStore;
import io.springlens.runtime.ProbeValueCollector;
import io.springlens.runtime.RuntimeSignalProcessor;
import io.springlens.spi.RuntimeSignal;
import io.springlens.spi.RuntimeSignalType;
import io.springlens.starter.LensRuntimeProperties;
import io.springlens.starter.RuntimeExecutionContextHolder;
import io.springlens.starter.probe.LensProbeCaptureService;
import io.springlens.starter.probe.LensProbeRegistry;
import io.springlens.starter.probe.LensValueSanitizer;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

class AgentOverlayHttpInterceptorTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-03-31T17:10:00Z"), java.time.ZoneOffset.UTC);

    @Test
    void capturesAgentOverlayProbeForMatchingHttpRoute() throws Exception {
        InMemoryExecutionGraphStore store = new InMemoryExecutionGraphStore(32);
        RuntimeSignalProcessor processor = new RuntimeSignalProcessor(store, List.of(new HttpRequestCollector(), new ProbeValueCollector()));
        RuntimeExecutionContextHolder contextHolder = new RuntimeExecutionContextHolder();
        LensProbeCaptureService captureService = new LensProbeCaptureService(
                processor,
                contextHolder,
                new LensProbeRegistry(new StaticApplicationContext()),
                new LensValueSanitizer(new ObjectMapper())
        );
        AgentOverlayEngine engine = new AgentOverlayEngine(agentProperties(), runtimeProperties(), FIXED_CLOCK);
        engine.applySnapshot(new OverlayDeliverySnapshot(
                "orders-app",
                "orders-1",
                policySnapshot(),
                List.of(registeredOverlay("/orders/**", "GET")),
                "2026-03-31T17:10:00Z"
        ));

        AgentOverlayHttpInterceptor interceptor = new AgentOverlayHttpInterceptor(engine, captureService, new AgentOverlayValueResolver());

        ExecutionContext context = new ExecutionContext(
                "orders-app",
                "orders-1",
                "graph-2",
                Instant.parse("2026-03-31T17:10:00Z"),
                Map.of("path", "/orders/42")
        );
        processor.start(context);
        contextHolder.set("graph-2");
        processor.process(new RuntimeSignal(
                "graph-2",
                RuntimeSignalType.HTTP_REQUEST_STARTED,
                Instant.parse("2026-03-31T17:10:00Z"),
                Map.of("method", "GET", "path", "/orders/42")
        ));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/orders/42");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        interceptor.afterCompletion(request, response, new Object(), null);

        processor.process(new RuntimeSignal(
                "graph-2",
                RuntimeSignalType.HTTP_REQUEST_COMPLETED,
                Instant.parse("2026-03-31T17:10:01Z"),
                Map.of("status", 200, "durationMs", 1000L)
        ));
        contextHolder.clear();

        assertThat(store.findProbeValues("orders-app", "http.route.path", 10))
                .singleElement()
                .satisfies(record -> {
                    assertThat(record.value()).isEqualTo("/orders/42");
                    assertThat(record.captureSource()).isEqualTo("agent-overlay");
                });

        ExecutionGraph graph = store.findGraph("graph-2").orElseThrow();
        assertThat(graph.nodes()).filteredOn(node -> NodeType.WATCH_VALUE.equals(node.type()))
                .singleElement()
                .satisfies(node -> {
                    assertThat(node.originKind()).isEqualTo(ExecutionOriginKind.AGENT_OVERLAY);
                    assertThat(node.sourceRef()).isEqualTo("overlay:ovl-http-route");
                });
    }

    private AgentInstrumentationProperties agentProperties() {
        AgentInstrumentationProperties properties = new AgentInstrumentationProperties();
        properties.setEnabled(true);
        properties.setMode(AgentInstrumentationMode.HYBRID_APPROVAL);
        return properties;
    }

    private LensRuntimeProperties runtimeProperties() {
        LensRuntimeProperties properties = new LensRuntimeProperties();
        properties.setApplicationId("orders-app");
        properties.setInstanceId("orders-1");
        return properties;
    }

    private PolicySnapshot policySnapshot() {
        return new PolicySnapshot(
                AgentInstrumentationMode.HYBRID_APPROVAL,
                true,
                false,
                true,
                Map.of("plane", "agent-control")
        );
    }

    private RegisteredOverlay registeredOverlay(String pathPattern, String httpMethod) {
        return new RegisteredOverlay(
                "ovl-http-route",
                new OverlaySpec(
                        "ovl-http-route",
                        AgentInstrumentationMode.HYBRID_APPROVAL,
                        AgentActionRiskLevel.MEDIUM,
                        true,
                        "PT4H",
                        "http-route",
                        pathPattern,
                        httpMethod,
                        "AFTER_RETURN",
                        "http.route.path",
                        "request.path",
                        "Capture request path for the route overlay",
                        List.of("http"),
                        Map.of("applicationId", "orders-app")
                ),
                ApprovalState.APPROVED,
                "2026-03-31T17:09:00Z",
                null,
                Map.of()
        );
    }
}
