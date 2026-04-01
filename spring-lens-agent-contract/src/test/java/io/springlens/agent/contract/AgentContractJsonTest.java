package io.springlens.agent.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AgentContractJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void preservesStandardValuesAndSerializesOverlaySpecs() throws Exception {
        assertEquals("HYBRID_APPROVAL", AgentInstrumentationMode.HYBRID_APPROVAL.value());
        assertEquals(AgentInstrumentationMode.of("FULL_TRUST"), objectMapper.readValue("\"FULL_TRUST\"", AgentInstrumentationMode.class));
        assertEquals(AgentActionRiskLevel.of("critical"), objectMapper.readValue("\"critical\"", AgentActionRiskLevel.class));

        OverlaySpec overlay = new OverlaySpec(
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
                Map.of("createdBy", "codex", "skillId", "order-diagnosis")
        );

        String json = objectMapper.writeValueAsString(overlay);

        assertTrue(json.contains("\"overlayId\":\"ovl-order-status\""));
        assertTrue(json.contains("\"mode\":\"HYBRID_APPROVAL\""));
        assertTrue(json.contains("\"selectorType\":\"spring-bean-method\""));
        assertTrue(json.contains("\"probeId\":\"order.submit.status\""));
    }

    @Test
    void serializesPatchProposalWithOptionalApprovalMetadata() throws Exception {
        PatchProposal proposal = new PatchProposal(
                "patch-order-service-status",
                AgentActionRiskLevel.HIGH,
                "Add Lens-origin status capture after submitOrder returns",
                List.of("src/main/java/com/example/order/OrderService.java"),
                "*** Begin Patch\n*** End Patch\n",
                "*** Begin Patch\n*** End Patch\n",
                false,
                "order-diagnosis",
                Map.of("requestedBy", "codex", "approvalState", "pending")
        );

        String json = objectMapper.writeValueAsString(proposal);

        assertTrue(json.contains("\"patchId\":\"patch-order-service-status\""));
        assertTrue(json.contains("\"riskLevel\":\"HIGH\""));
        assertTrue(json.contains("\"requiresApproval\":false"));
        assertTrue(json.contains("\"approvalState\":\"pending\""));
    }

    @Test
    void serializesPolicySnapshotOverlayRegistrationAndAuditEvents() throws Exception {
        assertEquals("PENDING", ApprovalState.PENDING.value());
        assertEquals(ApprovalState.of("APPROVED"), objectMapper.readValue("\"APPROVED\"", ApprovalState.class));
        assertEquals(AuditEventType.of("OVERLAY_DISABLED"), objectMapper.readValue("\"OVERLAY_DISABLED\"", AuditEventType.class));
        assertEquals(AuditEventType.of("OVERLAY_APPROVED"), objectMapper.readValue("\"OVERLAY_APPROVED\"", AuditEventType.class));

        PolicySnapshot policySnapshot = new PolicySnapshot(
                AgentInstrumentationMode.HYBRID_APPROVAL,
                true,
                false,
                true,
                Map.of("environment", "local")
        );
        RegisteredOverlay overlay = new RegisteredOverlay(
                "ovl-order-status",
                new OverlaySpec(
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
                        Map.of("createdBy", "codex")
                ),
                ApprovalState.PENDING,
                "2026-03-31T13:45:00Z",
                null,
                Map.of("createdBy", "codex")
        );
        AuditEvent auditEvent = new AuditEvent(
                "audit-1",
                AuditEventType.OVERLAY_APPLIED,
                "2026-03-31T13:45:00Z",
                "codex",
                "ovl-order-status",
                Map.of("applicationId", "orders-app")
        );

        assertTrue(objectMapper.writeValueAsString(policySnapshot).contains("\"sourceEditEnabled\":true"));
        assertTrue(objectMapper.writeValueAsString(policySnapshot).contains("\"approvalRequired\":true"));
        assertTrue(objectMapper.writeValueAsString(overlay).contains("\"approvalState\":\"PENDING\""));
        assertTrue(objectMapper.writeValueAsString(overlay).contains("\"overlayId\":\"ovl-order-status\""));
        assertTrue(objectMapper.writeValueAsString(auditEvent).contains("\"eventType\":\"OVERLAY_APPLIED\""));
        assertTrue(objectMapper.writeValueAsString(auditEvent).contains("\"targetId\":\"ovl-order-status\""));
    }

    @Test
    void serializesOverlayDeliverySnapshotForRuntimePull() throws Exception {
        OverlayDeliverySnapshot snapshot = new OverlayDeliverySnapshot(
                "orders-app",
                "orders-1",
                new PolicySnapshot(
                        AgentInstrumentationMode.HYBRID_APPROVAL,
                        true,
                        false,
                        true,
                        Map.of("plane", "agent-control")
                ),
                List.of(new RegisteredOverlay(
                        "ovl-order-status",
                        new OverlaySpec(
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
                        ),
                        ApprovalState.PENDING,
                        "2026-03-31T16:00:00Z",
                        null,
                        Map.of("selectorType", "spring-bean-method")
                )),
                "2026-03-31T16:00:05Z"
        );

        String json = objectMapper.writeValueAsString(snapshot);

        assertTrue(json.contains("\"applicationId\":\"orders-app\""));
        assertTrue(json.contains("\"instanceId\":\"orders-1\""));
        assertTrue(json.contains("\"activeOverlays\":["));
        assertTrue(json.contains("\"overlayId\":\"ovl-order-status\""));
        assertTrue(json.contains("\"deliveredAt\":\"2026-03-31T16:00:05Z\""));
    }

    @Test
    void serializesRuntimeSafetyDraftBundleWithOverlayAndPatchDrafts() throws Exception {
        RuntimeSafetyDraftBundle bundle = new RuntimeSafetyDraftBundle(
                "orders-app",
                "orders-1",
                "spring-lens.runtime-safety",
                List.of(new OverlaySpec(
                        "draft-runtime-safety-overlay-1",
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
                        "draft-runtime-safety-patch-1",
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

        String json = objectMapper.writeValueAsString(bundle);

        assertTrue(json.contains("\"applicationId\":\"orders-app\""));
        assertTrue(json.contains("\"capabilityId\":\"spring-lens.runtime-safety\""));
        assertTrue(json.contains("\"overlayDraftCount\":1"));
        assertTrue(json.contains("\"patchDraftCount\":1"));
        assertTrue(json.contains("\"draftId\":\"draft-runtime-safety-patch-1\""));
        assertTrue(json.contains("\"templateId\":\"replace-counter-with-atomic\""));
    }

    @Test
    void serializesRegisteredPatchDraftAndRuntimeSafetyPromotionResult() throws Exception {
        assertEquals("PENDING", PatchDraftStatus.PENDING.value());
        assertEquals(PatchDraftStatus.of("ACCEPTED"), objectMapper.readValue("\"ACCEPTED\"", PatchDraftStatus.class));

        RegisteredPatchDraft registeredPatchDraft = new RegisteredPatchDraft(
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
                "2026-03-31T18:00:00Z",
                null,
                Map.of("actor", "codex", "applicationId", "orders-app")
        );

        RuntimeSafetyPromotionResult result = new RuntimeSafetyPromotionResult(
                "orders-app",
                "orders-1",
                "spring-lens.runtime-safety",
                List.of(new RegisteredOverlay(
                        "draft-runtime-safety-overlay-1",
                        new OverlaySpec(
                                "draft-runtime-safety-overlay-1",
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
                        ),
                        ApprovalState.PENDING,
                        "2026-03-31T18:00:00Z",
                        null,
                        Map.of("actor", "codex")
                )),
                List.of(registeredPatchDraft),
                Map.of("actor", "codex", "sourceTool", "plan_runtime_safety_remediation")
        );

        String patchJson = objectMapper.writeValueAsString(registeredPatchDraft);
        String resultJson = objectMapper.writeValueAsString(result);

        assertTrue(patchJson.contains("\"status\":\"PENDING\""));
        assertTrue(patchJson.contains("\"draftId\":\"runtime-safety-patch-1\""));
        assertTrue(resultJson.contains("\"overlayRegistrationCount\":1"));
        assertTrue(resultJson.contains("\"patchDraftRegistrationCount\":1"));
        assertTrue(resultJson.contains("\"applicationId\":\"orders-app\""));
        assertTrue(resultJson.contains("\"sourceTool\":\"plan_runtime_safety_remediation\""));
    }
}
