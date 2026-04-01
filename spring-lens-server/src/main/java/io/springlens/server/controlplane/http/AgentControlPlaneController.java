package io.springlens.server.controlplane.http;

import io.springlens.agent.contract.OverlayDeliverySnapshot;
import io.springlens.server.controlplane.overlay.OverlayRegistryService;
import io.springlens.server.controlplane.policy.AgentPolicyService;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/apps")
public class AgentControlPlaneController {

    private final AgentPolicyService agentPolicyService;
    private final OverlayRegistryService overlayRegistryService;

    public AgentControlPlaneController(
            AgentPolicyService agentPolicyService,
            OverlayRegistryService overlayRegistryService
    ) {
        this.agentPolicyService = agentPolicyService;
        this.overlayRegistryService = overlayRegistryService;
    }

    @GetMapping("/{applicationId}/agent-overlays")
    public OverlayDeliverySnapshot agentOverlays(
            @PathVariable String applicationId,
            @RequestParam(required = false) String instanceId
    ) {
        return new OverlayDeliverySnapshot(
                applicationId,
                instanceId,
                agentPolicyService.snapshot(),
                overlayRegistryService.listDeliverable(applicationId, instanceId),
                Instant.now().toString()
        );
    }
}
