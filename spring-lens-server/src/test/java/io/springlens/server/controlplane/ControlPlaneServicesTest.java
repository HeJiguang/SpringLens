package io.springlens.server.controlplane;

import static org.assertj.core.api.Assertions.assertThat;

import io.springlens.agent.contract.AgentActionRiskLevel;
import io.springlens.agent.contract.AgentInstrumentationMode;
import io.springlens.agent.contract.ApprovalState;
import io.springlens.agent.contract.AuditEventType;
import io.springlens.agent.contract.OverlaySpec;
import io.springlens.agent.contract.PatchDraftStatus;
import io.springlens.agent.contract.PatchProposalDraft;
import io.springlens.server.controlplane.patch.PatchDraftRegistryService;
import io.springlens.server.controlplane.audit.AuditTrailService;
import io.springlens.server.controlplane.overlay.OverlayRegistryService;
import io.springlens.server.controlplane.policy.AgentPolicyService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ControlPlaneServicesTest {

    private Clock clock;
    private AuditTrailService auditTrailService;
    private OverlayRegistryService overlayRegistryService;
    private PatchDraftRegistryService patchDraftRegistryService;
    private AgentPolicyService agentPolicyService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-03-31T14:00:00Z"), ZoneOffset.UTC);
        auditTrailService = new AuditTrailService(clock);
        overlayRegistryService = new OverlayRegistryService(clock, auditTrailService);
        patchDraftRegistryService = new PatchDraftRegistryService(clock, auditTrailService);
        agentPolicyService = new AgentPolicyService();
    }

    @Test
    void returnsDefaultPolicySnapshot() {
        var snapshot = agentPolicyService.snapshot();

        assertThat(snapshot.mode()).isEqualTo(AgentInstrumentationMode.HYBRID_APPROVAL);
        assertThat(snapshot.sourceEditEnabled()).isTrue();
        assertThat(snapshot.sourceEditAutoApply()).isFalse();
        assertThat(snapshot.approvalRequired()).isTrue();
        assertThat(snapshot.metadata()).containsEntry("plane", "agent-control");
    }

    @Test
    void appliesOverlayAndEmitsAuditEvent() {
        var overlay = overlayRegistryService.apply(overlaySpec(), "codex");

        assertThat(overlay.overlayId()).isEqualTo("ovl-order-status");
        assertThat(overlay.approvalState()).isEqualTo(ApprovalState.PENDING);
        assertThat(overlay.createdAt()).isEqualTo("2026-03-31T14:00:00Z");
        assertThat(overlay.disabledAt()).isNull();
        assertThat(overlayRegistryService.listActive()).extracting(item -> item.overlayId())
                .containsExactly("ovl-order-status");
        assertThat(overlayRegistryService.listDeliverable("orders-app", null)).isEmpty();
        assertThat(auditTrailService.list()).singleElement().satisfies(event -> {
            assertThat(event.eventType()).isEqualTo(AuditEventType.OVERLAY_APPLIED);
            assertThat(event.actor()).isEqualTo("codex");
            assertThat(event.targetId()).isEqualTo("ovl-order-status");
        });
    }

    @Test
    void approvesOverlayAndRecordsAuditTrail() {
        overlayRegistryService.apply(overlaySpec(), "codex");

        var overlay = overlayRegistryService.approve("ovl-order-status", "reviewer");

        assertThat(overlay.approvalState()).isEqualTo(ApprovalState.APPROVED);
        assertThat(overlayRegistryService.listDeliverable("orders-app", null))
                .extracting(item -> item.overlayId())
                .containsExactly("ovl-order-status");
        assertThat(auditTrailService.list()).hasSize(2);
        assertThat(auditTrailService.list()).extracting(item -> item.eventType())
                .containsExactly(AuditEventType.OVERLAY_APPLIED, AuditEventType.OVERLAY_APPROVED);
    }

    @Test
    void disablesOverlayAndRecordsAuditTrail() {
        overlayRegistryService.apply(overlaySpec(), "codex");
        overlayRegistryService.approve("ovl-order-status", "reviewer");

        var overlay = overlayRegistryService.disable("ovl-order-status", "codex");

        assertThat(overlay.approvalState()).isEqualTo(ApprovalState.DISABLED);
        assertThat(overlay.disabledAt()).isEqualTo("2026-03-31T14:00:00Z");
        assertThat(overlayRegistryService.listActive()).isEmpty();
        assertThat(overlayRegistryService.listDeliverable("orders-app", null)).isEmpty();
        assertThat(auditTrailService.list()).hasSize(3);
        assertThat(auditTrailService.list()).extracting(item -> item.eventType())
                .containsExactly(
                        AuditEventType.OVERLAY_APPLIED,
                        AuditEventType.OVERLAY_APPROVED,
                        AuditEventType.OVERLAY_DISABLED
                );
    }

    @Test
    void submitsPatchDraftAndEmitsAuditEvent() {
        var patchDraft = patchDraftRegistryService.submit(patchProposalDraft(), "codex");

        assertThat(patchDraft.draftId()).isEqualTo("runtime-safety-patch-1");
        assertThat(patchDraft.status()).isEqualTo(PatchDraftStatus.PENDING);
        assertThat(patchDraft.submittedAt()).isEqualTo("2026-03-31T14:00:00Z");
        assertThat(patchDraft.resolvedAt()).isNull();
        assertThat(patchDraftRegistryService.list()).extracting(item -> item.draftId())
                .containsExactly("runtime-safety-patch-1");
        assertThat(auditTrailService.list()).singleElement().satisfies(event -> {
            assertThat(event.eventType()).isEqualTo(AuditEventType.of("PATCH_DRAFT_SUBMITTED"));
            assertThat(event.actor()).isEqualTo("codex");
            assertThat(event.targetId()).isEqualTo("runtime-safety-patch-1");
        });
    }

    private OverlaySpec overlaySpec() {
        return new OverlaySpec(
                "ovl-order-status",
                AgentInstrumentationMode.HYBRID_APPROVAL,
                AgentActionRiskLevel.MEDIUM,
                true,
                "PT4H",
                "spring-bean-method",
                "com.example.order.OrderService",
                "submitOrder",
                "AFTER_RETURN",
                "order.submit.status",
                "result.status",
                "Capture the order status after submission",
                List.of("orders"),
                Map.of("applicationId", "orders-app")
        );
    }

    private PatchProposalDraft patchProposalDraft() {
        return new PatchProposalDraft(
                "runtime-safety-patch-1",
                AgentActionRiskLevel.HIGH,
                "replace-counter-with-atomic",
                "Replace the shared counter with AtomicInteger",
                "The shared counter is mutated across threads without atomic protection.",
                List.of("src/main/java/com/example/demo/DemoSafetyRiskService.java"),
                true,
                "runtime-safety-remediation",
                Map.of("applicationId", "orders-app")
        );
    }
}
