package io.springlens.server.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import io.springlens.agent.contract.AuditEvent;
import io.springlens.agent.contract.AuditEventType;
import io.springlens.agent.contract.PatchProposalDraft;
import io.springlens.agent.contract.PatchDraftStatus;
import io.springlens.agent.contract.PolicySnapshot;
import io.springlens.agent.contract.RegisteredOverlay;
import io.springlens.agent.contract.RegisteredPatchDraft;
import io.springlens.agent.contract.AgentInstrumentationMode;
import io.springlens.agent.contract.ApprovalState;
import io.springlens.agent.contract.OverlaySpec;
import io.springlens.agent.contract.AgentActionRiskLevel;
import io.springlens.agent.contract.RuntimeSafetyPromotionResult;
import io.springlens.agent.contract.RuntimeSafetyDraftBundle;
import io.springlens.model.RuntimeToolDescriptor;
import io.springlens.model.diagnostic.DiagnosticResult;
import io.springlens.spi.DiagnosticTool;
import io.springlens.spi.ToolDescriptor;
import io.springlens.spi.ToolRequest;
import java.util.Map;
import io.springlens.server.tool.ToolRegistry;
import io.springlens.server.tool.ToolRouter;
import java.util.List;
import org.junit.jupiter.api.Test;

class LensMcpToolsTest {

    @Test
    void routesDiagnoseExecutionGraphToToolRouter() {
        CaptureRequestTool captureRequestTool = new CaptureRequestTool();
        LensMcpTools lensMcpTools = new LensMcpTools(new ToolRouter(new ToolRegistry(List.of(captureRequestTool))));

        DiagnosticResult result = lensMcpTools.diagnoseExecutionGraph("orders-app", "graph-9", null);

        assertThat(result.rootCause()).isEqualTo("Captured");
        assertThat(captureRequestTool.lastRequest.applicationId()).isEqualTo("orders-app");
        assertThat(captureRequestTool.lastRequest.instanceId()).isNull();
        assertThat(captureRequestTool.lastRequest.arguments()).containsEntry("executionId", "graph-9");
    }

    @Test
    void routesRuntimeToolOperationsToRenamedToolRouterEntries() {
        CaptureRuntimeListTool listTool = new CaptureRuntimeListTool();
        CaptureRuntimeInvokeTool invokeTool = new CaptureRuntimeInvokeTool();
        CaptureRuntimeSafetyDraftTool draftTool = new CaptureRuntimeSafetyDraftTool();
        CapturePromoteRuntimeSafetyTool promoteTool = new CapturePromoteRuntimeSafetyTool();
        CaptureListPatchDraftsTool listPatchDraftsTool = new CaptureListPatchDraftsTool();
        LensMcpTools lensMcpTools = new LensMcpTools(new ToolRouter(new ToolRegistry(List.of(
                listTool,
                invokeTool,
                draftTool,
                promoteTool,
                listPatchDraftsTool
        ))));

        List<RuntimeToolDescriptor> tools = lensMcpTools.listRuntimeTools("orders-app", null);
        Object result = lensMcpTools.invokeRuntimeTool("orders-app", "count_orders_by_status", null, java.util.Map.of("status", "PAID"));
        RuntimeSafetyDraftBundle draftBundle = lensMcpTools.draftRuntimeSafetyRemediation(
                "orders-app",
                null,
                java.util.Map.of("maxFindings", 5)
        );
        RuntimeSafetyPromotionResult promotionResult = lensMcpTools.promoteRuntimeSafetyRemediation(
                "orders-app",
                null,
                java.util.Map.of("maxFindings", 5),
                "codex"
        );
        List<RegisteredPatchDraft> patchDrafts = lensMcpTools.listPatchDrafts();

        assertThat(tools).extracting(RuntimeToolDescriptor::name).containsExactly("count_orders_by_status");
        assertThat(listTool.lastRequest.applicationId()).isEqualTo("orders-app");
        assertThat(listTool.lastRequest.arguments()).isEmpty();
        assertThat(invokeTool.lastRequest.arguments())
                .containsEntry("toolName", "count_orders_by_status")
                .containsEntry("arguments", java.util.Map.of("status", "PAID"));
        assertThat(result).isEqualTo(java.util.Map.of("count", 1, "status", "PAID"));
        assertThat(draftTool.lastRequest.applicationId()).isEqualTo("orders-app");
        assertThat(draftTool.lastRequest.arguments()).containsEntry("arguments", java.util.Map.of("maxFindings", 5));
        assertThat(draftBundle.patchDrafts()).extracting(PatchProposalDraft::templateId)
                .containsExactly("replace-counter-with-atomic");
        assertThat(promoteTool.lastRequest.arguments())
                .containsEntry("arguments", java.util.Map.of("maxFindings", 5))
                .containsEntry("actor", "codex");
        assertThat(promotionResult.patchDraftRegistrations()).extracting(RegisteredPatchDraft::draftId)
                .containsExactly("runtime-safety-patch-1");
        assertThat(patchDrafts).extracting(RegisteredPatchDraft::status).containsExactly(PatchDraftStatus.PENDING);
        assertThat(listPatchDraftsTool.lastRequest.arguments()).isEmpty();
    }

    @Test
    void routesControlPlaneOperationsToToolRouter() {
        CapturePolicyTool policyTool = new CapturePolicyTool();
        CaptureApplyOverlayTool applyTool = new CaptureApplyOverlayTool();
        CaptureListOverlaysTool listOverlaysTool = new CaptureListOverlaysTool();
        CaptureApproveOverlayTool approveTool = new CaptureApproveOverlayTool();
        CaptureDisableOverlayTool disableTool = new CaptureDisableOverlayTool();
        CaptureAuditEventsTool auditEventsTool = new CaptureAuditEventsTool();
        LensMcpTools lensMcpTools = new LensMcpTools(new ToolRouter(new ToolRegistry(List.of(
                policyTool,
                applyTool,
                listOverlaysTool,
                approveTool,
                disableTool,
                auditEventsTool
        ))));

        PolicySnapshot snapshot = lensMcpTools.getPolicySnapshot();
        RegisteredOverlay applied = lensMcpTools.applyOverlayInstrumentation(
                Map.ofEntries(
                        Map.entry("overlayId", "ovl-order-status"),
                        Map.entry("mode", "HYBRID_APPROVAL"),
                        Map.entry("riskLevel", "MEDIUM"),
                        Map.entry("enabled", true),
                        Map.entry("selectorType", "spring-bean-method")
                ),
                "codex"
        );
        List<RegisteredOverlay> activeOverlays = lensMcpTools.listActiveOverlays();
        RegisteredOverlay approved = lensMcpTools.approveOverlayInstrumentation("ovl-order-status", "reviewer");
        RegisteredOverlay disabled = lensMcpTools.disableOverlayInstrumentation("ovl-order-status", "codex");
        List<AuditEvent> auditEvents = lensMcpTools.listAuditEvents();

        assertThat(snapshot.mode()).isEqualTo(AgentInstrumentationMode.HYBRID_APPROVAL);
        assertThat(policyTool.lastRequest.arguments()).isEmpty();
        assertThat(applyTool.lastRequest.arguments())
                .containsEntry("overlayId", "ovl-order-status")
                .containsEntry("mode", "HYBRID_APPROVAL")
                .containsEntry("actor", "codex");
        assertThat(applied.overlayId()).isEqualTo("ovl-order-status");
        assertThat(activeOverlays).extracting(RegisteredOverlay::overlayId).containsExactly("ovl-order-status");
        assertThat(listOverlaysTool.lastRequest.arguments()).isEmpty();
        assertThat(approveTool.lastRequest.arguments())
                .containsEntry("overlayId", "ovl-order-status")
                .containsEntry("actor", "reviewer");
        assertThat(approved.approvalState()).isEqualTo(ApprovalState.APPROVED);
        assertThat(disableTool.lastRequest.arguments())
                .containsEntry("overlayId", "ovl-order-status")
                .containsEntry("actor", "codex");
        assertThat(disabled.approvalState()).isEqualTo(ApprovalState.DISABLED);
        assertThat(auditEvents).extracting(AuditEvent::eventType).containsExactly(AuditEventType.OVERLAY_APPLIED);
        assertThat(auditEventsTool.lastRequest.arguments()).isEmpty();
    }

    private static final class CaptureRequestTool implements DiagnosticTool {

        private ToolRequest lastRequest;

        @Override
        public ToolDescriptor descriptor() {
            return new ToolDescriptor("diagnose_execution_graph", "Capture tool request for tests.");
        }

        @Override
        public Object execute(ToolRequest request) {
            lastRequest = request;
            return DiagnosticResult.builder()
                    .rootCause("Captured")
                    .summary("Captured request")
                    .confidence(0.9)
                    .build();
        }
    }

    private static final class CaptureRuntimeListTool implements DiagnosticTool {

        private ToolRequest lastRequest;

        @Override
        public ToolDescriptor descriptor() {
            return new ToolDescriptor("list_runtime_tools", "Capture runtime tool listing requests.");
        }

        @Override
        public Object execute(ToolRequest request) {
            lastRequest = request;
            return List.of(new RuntimeToolDescriptor("count_orders_by_status", "Count orders", "demo.orders"));
        }
    }

    private static final class CaptureRuntimeInvokeTool implements DiagnosticTool {

        private ToolRequest lastRequest;

        @Override
        public ToolDescriptor descriptor() {
            return new ToolDescriptor("invoke_runtime_tool", "Capture runtime tool invoke requests.");
        }

        @Override
        public Object execute(ToolRequest request) {
            lastRequest = request;
            return java.util.Map.of("count", 1, "status", "PAID");
        }
    }

    private static final class CaptureRuntimeSafetyDraftTool implements DiagnosticTool {

        private ToolRequest lastRequest;

        @Override
        public ToolDescriptor descriptor() {
            return new ToolDescriptor("draft_runtime_safety_remediation", "Capture runtime safety draft requests.");
        }

        @Override
        public Object execute(ToolRequest request) {
            lastRequest = request;
            return new RuntimeSafetyDraftBundle(
                    request.applicationId(),
                    request.instanceId(),
                    "spring-lens.runtime-safety",
                    List.of(new OverlaySpec(
                            "draft-runtime-queue-depth",
                            AgentInstrumentationMode.HYBRID_APPROVAL,
                            AgentActionRiskLevel.MEDIUM,
                            true,
                            "PT2H",
                            "spring-bean-method",
                            "com.example.demo.DemoSafetyRiskService",
                            "enqueueUnsafeWork",
                            "AFTER_RETURN",
                            "runtime.queue.depth",
                            "result",
                            "Observe queue depth while validating runtime safety findings",
                            List.of("runtime-safety", "draft"),
                            Map.of("sourceTool", "plan_runtime_safety_remediation")
                    )),
                    List.of(new PatchProposalDraft(
                            "runtime-safety-patch-1",
                            AgentActionRiskLevel.HIGH,
                            "replace-counter-with-atomic",
                            "Replace the shared counter with AtomicInteger",
                            "The shared counter is mutated across threads without atomic protection.",
                            List.of("src/main/java/com/example/demo/DemoSafetyRiskService.java"),
                            true,
                            "runtime-safety-remediation",
                            Map.of("sourceRuleId", "singleton-non-atomic-counter-field")
                    )),
                    Map.of("sourceTool", "plan_runtime_safety_remediation")
            );
        }
    }

    private static final class CapturePromoteRuntimeSafetyTool implements DiagnosticTool {

        private ToolRequest lastRequest;

        @Override
        public ToolDescriptor descriptor() {
            return new ToolDescriptor("promote_runtime_safety_remediation", "Capture runtime safety promotion requests.");
        }

        @Override
        public Object execute(ToolRequest request) {
            lastRequest = request;
            return new RuntimeSafetyPromotionResult(
                    request.applicationId(),
                    request.instanceId(),
                    "spring-lens.runtime-safety",
                    List.of(overlay("draft-runtime-queue-depth", ApprovalState.PENDING)),
                    List.of(new RegisteredPatchDraft(
                            "runtime-safety-patch-1",
                            new PatchProposalDraft(
                                    "runtime-safety-patch-1",
                                    AgentActionRiskLevel.HIGH,
                                    "replace-counter-with-atomic",
                                    "Replace the shared counter with AtomicInteger",
                                    "The shared counter is mutated across threads without atomic protection.",
                                    List.of("src/main/java/com/example/demo/DemoSafetyRiskService.java"),
                                    true,
                                    "runtime-safety-remediation",
                                    Map.of("sourceRuleId", "singleton-non-atomic-counter-field")
                            ),
                            PatchDraftStatus.PENDING,
                            "2026-03-31T18:30:00Z",
                            null,
                            Map.of("actor", "codex")
                    )),
                    Map.of("actor", "codex", "sourceTool", "plan_runtime_safety_remediation")
            );
        }
    }

    private static final class CaptureListPatchDraftsTool implements DiagnosticTool {

        private ToolRequest lastRequest;

        @Override
        public ToolDescriptor descriptor() {
            return new ToolDescriptor("list_patch_drafts", "Capture patch draft listing requests.");
        }

        @Override
        public Object execute(ToolRequest request) {
            lastRequest = request;
            return List.of(new RegisteredPatchDraft(
                    "runtime-safety-patch-1",
                    new PatchProposalDraft(
                            "runtime-safety-patch-1",
                            AgentActionRiskLevel.HIGH,
                            "replace-counter-with-atomic",
                            "Replace the shared counter with AtomicInteger",
                            "The shared counter is mutated across threads without atomic protection.",
                            List.of("src/main/java/com/example/demo/DemoSafetyRiskService.java"),
                            true,
                            "runtime-safety-remediation",
                            Map.of("sourceRuleId", "singleton-non-atomic-counter-field")
                    ),
                    PatchDraftStatus.PENDING,
                    "2026-03-31T18:30:00Z",
                    null,
                    Map.of("actor", "codex")
            ));
        }
    }

    private static final class CapturePolicyTool implements DiagnosticTool {

        private ToolRequest lastRequest;

        @Override
        public ToolDescriptor descriptor() {
            return new ToolDescriptor("get_policy_snapshot", "Capture policy snapshot requests.");
        }

        @Override
        public Object execute(ToolRequest request) {
            lastRequest = request;
            return new PolicySnapshot(
                    AgentInstrumentationMode.HYBRID_APPROVAL,
                    true,
                    false,
                    true,
                    Map.of("plane", "agent-control")
            );
        }
    }

    private static final class CaptureApplyOverlayTool implements DiagnosticTool {

        private ToolRequest lastRequest;

        @Override
        public ToolDescriptor descriptor() {
            return new ToolDescriptor("apply_overlay_instrumentation", "Capture overlay apply requests.");
        }

        @Override
        public Object execute(ToolRequest request) {
            lastRequest = request;
            return overlay("ovl-order-status", ApprovalState.PENDING);
        }
    }

    private static final class CaptureListOverlaysTool implements DiagnosticTool {

        private ToolRequest lastRequest;

        @Override
        public ToolDescriptor descriptor() {
            return new ToolDescriptor("list_active_overlays", "Capture active overlay listing requests.");
        }

        @Override
        public Object execute(ToolRequest request) {
            lastRequest = request;
            return List.of(overlay("ovl-order-status", ApprovalState.PENDING));
        }
    }

    private static final class CaptureDisableOverlayTool implements DiagnosticTool {

        private ToolRequest lastRequest;

        @Override
        public ToolDescriptor descriptor() {
            return new ToolDescriptor("disable_overlay_instrumentation", "Capture overlay disable requests.");
        }

        @Override
        public Object execute(ToolRequest request) {
            lastRequest = request;
            return overlay("ovl-order-status", ApprovalState.DISABLED);
        }
    }

    private static final class CaptureApproveOverlayTool implements DiagnosticTool {

        private ToolRequest lastRequest;

        @Override
        public ToolDescriptor descriptor() {
            return new ToolDescriptor("approve_overlay_instrumentation", "Capture overlay approve requests.");
        }

        @Override
        public Object execute(ToolRequest request) {
            lastRequest = request;
            return overlay("ovl-order-status", ApprovalState.APPROVED);
        }
    }

    private static final class CaptureAuditEventsTool implements DiagnosticTool {

        private ToolRequest lastRequest;

        @Override
        public ToolDescriptor descriptor() {
            return new ToolDescriptor("list_audit_events", "Capture audit event listing requests.");
        }

        @Override
        public Object execute(ToolRequest request) {
            lastRequest = request;
            return List.of(new AuditEvent(
                    "audit-1",
                    AuditEventType.OVERLAY_APPLIED,
                    "2026-03-31T15:00:00Z",
                    "codex",
                    "ovl-order-status",
                    Map.of()
            ));
        }
    }

    private static RegisteredOverlay overlay(String overlayId, ApprovalState approvalState) {
        return new RegisteredOverlay(
                overlayId,
                new OverlaySpec(
                        overlayId,
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
                        "Capture status",
                        List.of("orders"),
                        Map.of("applicationId", "orders-app")
                ),
                approvalState,
                "2026-03-31T15:00:00Z",
                approvalState.equals(ApprovalState.DISABLED) ? "2026-03-31T15:00:00Z" : null,
                Map.of()
        );
    }
}
