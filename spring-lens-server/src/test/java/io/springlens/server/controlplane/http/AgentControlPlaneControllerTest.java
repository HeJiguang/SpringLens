package io.springlens.server.controlplane.http;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.springlens.agent.contract.AgentActionRiskLevel;
import io.springlens.agent.contract.AgentInstrumentationMode;
import io.springlens.agent.contract.OverlaySpec;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AgentControlPlaneControllerTest {

    private OverlayRegistryService overlayRegistryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-31T16:10:00Z"), ZoneOffset.UTC);
        overlayRegistryService = new OverlayRegistryService(clock, new AuditTrailService(clock));
        AgentPolicyService agentPolicyService = new AgentPolicyService();
        mockMvc = MockMvcBuilders.standaloneSetup(
                new AgentControlPlaneController(agentPolicyService, overlayRegistryService)
        ).build();
    }

    @Test
    void returnsPolicyAndFilteredActiveOverlaysForApplicationInstance() throws Exception {
        overlayRegistryService.apply(overlay("ovl-orders-default", "orders-app", null), "codex");
        overlayRegistryService.approve("ovl-orders-default", "reviewer");
        overlayRegistryService.apply(overlay("ovl-orders-other-instance", "orders-app", "orders-2"), "codex");
        overlayRegistryService.approve("ovl-orders-other-instance", "reviewer");
        overlayRegistryService.apply(overlay("ovl-billing-default", "billing-app", null), "codex");
        overlayRegistryService.approve("ovl-billing-default", "reviewer");
        overlayRegistryService.apply(overlay("ovl-orders-pending", "orders-app", null), "codex");

        mockMvc.perform(get("/internal/apps/orders-app/agent-overlays")
                        .queryParam("instanceId", "orders-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value("orders-app"))
                .andExpect(jsonPath("$.instanceId").value("orders-1"))
                .andExpect(jsonPath("$.policy.mode").value("HYBRID_APPROVAL"))
                .andExpect(jsonPath("$.activeOverlays.length()").value(1))
                .andExpect(jsonPath("$.activeOverlays[0].overlayId").value("ovl-orders-default"))
                .andExpect(jsonPath("$.activeOverlays[0].spec.metadata.applicationId").value("orders-app"));
    }

    @Test
    void returnsApplicationScopedOverlaysWhenInstanceIsNotProvided() throws Exception {
        overlayRegistryService.apply(overlay("ovl-orders-default", "orders-app", null), "codex");
        overlayRegistryService.approve("ovl-orders-default", "reviewer");
        overlayRegistryService.apply(overlay("ovl-orders-other-instance", "orders-app", "orders-2"), "codex");
        overlayRegistryService.approve("ovl-orders-other-instance", "reviewer");
        overlayRegistryService.apply(overlay("ovl-orders-pending", "orders-app", null), "codex");

        mockMvc.perform(get("/internal/apps/orders-app/agent-overlays"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value("orders-app"))
                .andExpect(jsonPath("$.activeOverlays.length()").value(2));
    }

    private OverlaySpec overlay(String overlayId, String applicationId, String instanceId) {
        Map<String, String> metadata = instanceId == null
                ? Map.of("applicationId", applicationId)
                : Map.of("applicationId", applicationId, "instanceId", instanceId);
        return new OverlaySpec(
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
                "Capture the order status after submission",
                List.of("orders"),
                metadata
        );
    }
}
