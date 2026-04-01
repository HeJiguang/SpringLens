package io.springlens.agent.starter;

import static org.assertj.core.api.Assertions.assertThat;

import io.springlens.agent.contract.AgentActionRiskLevel;
import io.springlens.agent.contract.AgentInstrumentationMode;
import io.springlens.agent.contract.ApprovalState;
import io.springlens.agent.contract.OverlayDeliverySnapshot;
import io.springlens.agent.contract.OverlaySpec;
import io.springlens.agent.contract.PolicySnapshot;
import io.springlens.agent.contract.RegisteredOverlay;
import io.springlens.starter.LensRuntimeProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AgentOverlayEngineTest {

    @Test
    void storesOnlyApprovedAndUnexpiredOverlaysFromSnapshot() {
        AgentOverlayEngine engine = new AgentOverlayEngine(
                agentProperties(true),
                runtimeProperties("orders-app", "orders-1"),
                Clock.fixed(Instant.parse("2026-03-31T16:30:00Z"), ZoneOffset.UTC)
        );

        engine.applySnapshot(new OverlayDeliverySnapshot(
                "orders-app",
                "orders-1",
                policySnapshot(),
                List.of(
                        registeredOverlay("ovl-orders-approved", "orders-app", null, true, ApprovalState.APPROVED, "PT4H", "2026-03-31T16:25:00Z"),
                        registeredOverlay("ovl-orders-pending", "orders-app", null, true, ApprovalState.PENDING, "PT4H", "2026-03-31T16:25:00Z"),
                        registeredOverlay("ovl-orders-expired", "orders-app", null, true, ApprovalState.APPROVED, "PT1M", "2026-03-31T16:20:00Z"),
                        registeredOverlay("ovl-orders-other-instance", "orders-app", "orders-2", true, ApprovalState.APPROVED, "PT4H", "2026-03-31T16:25:00Z"),
                        registeredOverlay("ovl-billing-default", "billing-app", null, true, ApprovalState.APPROVED, "PT4H", "2026-03-31T16:25:00Z"),
                        registeredOverlay("ovl-orders-disabled", "orders-app", null, false, ApprovalState.APPROVED, "PT4H", "2026-03-31T16:25:00Z")
                ),
                "2026-03-31T16:30:00Z"
        ));

        assertThat(engine.activeOverlays()).extracting(OverlaySpec::overlayId)
                .containsExactly("ovl-orders-approved");
        assertThat(engine.lastDeliveredAt()).isEqualTo("2026-03-31T16:30:00Z");
        assertThat(engine.lastPolicy()).isNotNull();
    }

    @Test
    void rejectsAllOverlaysWhenInstrumentationIsDisabled() {
        AgentOverlayEngine engine = new AgentOverlayEngine(
                agentProperties(false),
                runtimeProperties("orders-app", "orders-1"),
                Clock.fixed(Instant.parse("2026-03-31T16:31:00Z"), ZoneOffset.UTC)
        );

        engine.applySnapshot(new OverlayDeliverySnapshot(
                "orders-app",
                "orders-1",
                policySnapshot(),
                List.of(registeredOverlay(
                        "ovl-orders-default",
                        "orders-app",
                        null,
                        true,
                        ApprovalState.APPROVED,
                        "PT4H",
                        "2026-03-31T16:25:00Z"
                )),
                "2026-03-31T16:31:00Z"
        ));

        assertThat(engine.activeOverlays()).isEmpty();
    }

    private AgentInstrumentationProperties agentProperties(boolean enabled) {
        AgentInstrumentationProperties properties = new AgentInstrumentationProperties();
        properties.setEnabled(enabled);
        properties.setMode(AgentInstrumentationMode.HYBRID_APPROVAL);
        return properties;
    }

    private LensRuntimeProperties runtimeProperties(String applicationId, String instanceId) {
        LensRuntimeProperties properties = new LensRuntimeProperties();
        properties.setApplicationId(applicationId);
        properties.setInstanceId(instanceId);
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

    private RegisteredOverlay registeredOverlay(
            String overlayId,
            String applicationId,
            String instanceId,
            boolean enabled,
            ApprovalState approvalState,
            String ttl,
            String createdAt
    ) {
        Map<String, String> metadata = instanceId == null
                ? Map.of("applicationId", applicationId)
                : Map.of("applicationId", applicationId, "instanceId", instanceId);
        return new RegisteredOverlay(
                overlayId,
                new OverlaySpec(
                        overlayId,
                        AgentInstrumentationMode.HYBRID_APPROVAL,
                        AgentActionRiskLevel.MEDIUM,
                        enabled,
                        ttl,
                        "spring-bean-method",
                        "com.example.order.OrderService",
                        "submitOrder",
                        "AFTER_RETURN",
                        "order.submit.status",
                        "result.status",
                        "Capture status",
                        List.of("orders"),
                        metadata
                ),
                approvalState,
                createdAt,
                null,
                Map.of()
        );
    }
}
