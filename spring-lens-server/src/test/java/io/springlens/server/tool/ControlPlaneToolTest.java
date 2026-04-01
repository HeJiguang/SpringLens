package io.springlens.server.tool;

import static org.assertj.core.api.Assertions.assertThat;

import io.springlens.agent.contract.AgentActionRiskLevel;
import io.springlens.agent.contract.AgentInstrumentationMode;
import io.springlens.agent.contract.AuditEvent;
import io.springlens.agent.contract.AuditEventType;
import io.springlens.agent.contract.PolicySnapshot;
import io.springlens.agent.contract.ApprovalState;
import io.springlens.agent.contract.RegisteredOverlay;
import io.springlens.server.controlplane.audit.AuditTrailService;
import io.springlens.server.controlplane.overlay.OverlayRegistryService;
import io.springlens.server.controlplane.policy.AgentPolicyService;
import io.springlens.spi.ToolRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ControlPlaneToolTest {

    private ToolRouter toolRouter;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-31T15:00:00Z"), ZoneOffset.UTC);
        AuditTrailService auditTrailService = new AuditTrailService(clock);
        OverlayRegistryService overlayRegistryService = new OverlayRegistryService(clock, auditTrailService);
        AgentPolicyService agentPolicyService = new AgentPolicyService();
        toolRouter = new ToolRouter(new ToolRegistry(List.of(
                new GetPolicySnapshotTool(agentPolicyService),
                new ListActiveOverlaysTool(overlayRegistryService),
                new ApplyOverlayInstrumentationTool(overlayRegistryService),
                new ApproveOverlayInstrumentationTool(overlayRegistryService),
                new DisableOverlayInstrumentationTool(overlayRegistryService),
                new ListAuditEventsTool(auditTrailService)
        )));
    }

    @Test
    void returnsDefaultPolicySnapshot() {
        PolicySnapshot snapshot = (PolicySnapshot) toolRouter.invoke("get_policy_snapshot", new ToolRequest(null, null, Map.of()));

        assertThat(snapshot.mode()).isEqualTo(AgentInstrumentationMode.HYBRID_APPROVAL);
        assertThat(snapshot.metadata()).containsEntry("plane", "agent-control");
    }

    @Test
    void appliesDisablesAndListsControlPlaneState() {
        RegisteredOverlay applied = (RegisteredOverlay) toolRouter.invoke(
                "apply_overlay_instrumentation",
                new ToolRequest(null, null, Map.ofEntries(
                        Map.entry("overlayId", "ovl-order-status"),
                        Map.entry("mode", AgentInstrumentationMode.HYBRID_APPROVAL.value()),
                        Map.entry("riskLevel", AgentActionRiskLevel.MEDIUM.value()),
                        Map.entry("enabled", true),
                        Map.entry("ttl", "PT4H"),
                        Map.entry("selectorType", "spring-bean-method"),
                        Map.entry("targetClassName", "com.example.order.OrderService"),
                        Map.entry("targetMethodName", "submitOrder"),
                        Map.entry("capturePhase", "AFTER_RETURN"),
                        Map.entry("probeId", "order.submit.status"),
                        Map.entry("expression", "result.status"),
                        Map.entry("description", "Capture the order status after submission"),
                        Map.entry("tags", List.of("orders")),
                        Map.entry("metadata", Map.of("applicationId", "orders-app")),
                        Map.entry("actor", "codex")
                ))
        );

        assertThat(applied.overlayId()).isEqualTo("ovl-order-status");
        assertThat(applied.approvalState()).isEqualTo(ApprovalState.PENDING);

        List<RegisteredOverlay> activeOverlays = castRegisteredOverlays(
                toolRouter.invoke("list_active_overlays", new ToolRequest(null, null, Map.of()))
        );
        assertThat(activeOverlays).extracting(RegisteredOverlay::overlayId).containsExactly("ovl-order-status");

        RegisteredOverlay approved = (RegisteredOverlay) toolRouter.invoke(
                "approve_overlay_instrumentation",
                new ToolRequest(null, null, Map.of("overlayId", "ovl-order-status", "actor", "reviewer"))
        );
        assertThat(approved.approvalState()).isEqualTo(ApprovalState.APPROVED);

        RegisteredOverlay disabled = (RegisteredOverlay) toolRouter.invoke(
                "disable_overlay_instrumentation",
                new ToolRequest(null, null, Map.of("overlayId", "ovl-order-status", "actor", "codex"))
        );
        assertThat(disabled.approvalState().value()).isEqualTo("DISABLED");

        List<RegisteredOverlay> noActiveOverlays = castRegisteredOverlays(
                toolRouter.invoke("list_active_overlays", new ToolRequest(null, null, Map.of()))
        );
        assertThat(noActiveOverlays).isEmpty();

        List<AuditEvent> auditEvents = castAuditEvents(
                toolRouter.invoke("list_audit_events", new ToolRequest(null, null, Map.of()))
        );
        assertThat(auditEvents).extracting(AuditEvent::eventType)
                .containsExactly(
                        AuditEventType.OVERLAY_APPLIED,
                        AuditEventType.OVERLAY_APPROVED,
                        AuditEventType.OVERLAY_DISABLED
                );
    }

    @SuppressWarnings("unchecked")
    private static List<RegisteredOverlay> castRegisteredOverlays(Object value) {
        return (List<RegisteredOverlay>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<AuditEvent> castAuditEvents(Object value) {
        return (List<AuditEvent>) value;
    }
}
