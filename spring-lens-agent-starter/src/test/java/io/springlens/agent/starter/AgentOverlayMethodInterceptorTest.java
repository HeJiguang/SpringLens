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
import io.springlens.model.diagnostic.ProbeValueRecord;
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
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.support.StaticApplicationContext;
import tools.jackson.databind.ObjectMapper;

class AgentOverlayMethodInterceptorTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-03-31T17:00:00Z"), java.time.ZoneOffset.UTC);

    @Test
    void capturesAgentOverlayProbeForMatchingBeanMethod() {
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
                List.of(registeredOverlay(TestOrderService.class.getName(), "submitOrder")),
                "2026-03-31T17:00:00Z"
        ));

        ProxyFactory proxyFactory = new ProxyFactory(new TestOrderService());
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice(new AgentOverlayMethodInterceptor(engine, captureService, new AgentOverlayValueResolver()));
        TestOrderService service = (TestOrderService) proxyFactory.getProxy();

        ExecutionContext context = new ExecutionContext(
                "orders-app",
                "orders-1",
                "graph-1",
                Instant.parse("2026-03-31T17:00:00Z"),
                Map.of("path", "/orders/submit")
        );
        processor.start(context);
        contextHolder.set("graph-1");
        processor.process(new RuntimeSignal(
                "graph-1",
                RuntimeSignalType.HTTP_REQUEST_STARTED,
                Instant.parse("2026-03-31T17:00:00Z"),
                Map.of("method", "POST", "path", "/orders/submit")
        ));

        String status = service.submitOrder();

        processor.process(new RuntimeSignal(
                "graph-1",
                RuntimeSignalType.HTTP_REQUEST_COMPLETED,
                Instant.parse("2026-03-31T17:00:01Z"),
                Map.of("status", 200, "durationMs", 1000L)
        ));
        contextHolder.clear();

        assertThat(status).isEqualTo("PAID");
        assertThat(store.findProbeValues("orders-app", "order.submit.status", 10))
                .singleElement()
                .satisfies(record -> {
                    assertThat(record.value()).isEqualTo("PAID");
                    assertThat(record.captureSource()).isEqualTo("agent-overlay");
                });

        ExecutionGraph graph = store.findGraph("graph-1").orElseThrow();
        assertThat(graph.nodes()).filteredOn(node -> NodeType.WATCH_VALUE.equals(node.type()))
                .singleElement()
                .satisfies(node -> {
                    assertThat(node.originKind()).isEqualTo(ExecutionOriginKind.AGENT_OVERLAY);
                    assertThat(node.sourceRef()).isEqualTo("overlay:ovl-order-status");
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

    private RegisteredOverlay registeredOverlay(String targetClassName, String targetMethodName) {
        return new RegisteredOverlay(
                "ovl-order-status",
                new OverlaySpec(
                        "ovl-order-status",
                        AgentInstrumentationMode.HYBRID_APPROVAL,
                        AgentActionRiskLevel.MEDIUM,
                        true,
                        "PT4H",
                        "spring-bean-method",
                        targetClassName,
                        targetMethodName,
                        "AFTER_RETURN",
                        "order.submit.status",
                        "result",
                        "Capture status after submitOrder",
                        List.of("orders"),
                        Map.of("applicationId", "orders-app")
                ),
                ApprovalState.APPROVED,
                "2026-03-31T16:59:00Z",
                null,
                Map.of()
        );
    }

    static class TestOrderService {

        String submitOrder() {
            return "PAID";
        }
    }
}
