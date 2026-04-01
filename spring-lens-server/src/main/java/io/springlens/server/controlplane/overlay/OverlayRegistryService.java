package io.springlens.server.controlplane.overlay;

import io.springlens.agent.contract.ApprovalState;
import io.springlens.agent.contract.AuditEventType;
import io.springlens.agent.contract.OverlaySpec;
import io.springlens.agent.contract.RegisteredOverlay;
import io.springlens.server.controlplane.audit.AuditTrailService;
import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OverlayRegistryService {

    private final Clock clock;
    private final AuditTrailService auditTrailService;
    private final ConcurrentHashMap<String, RegisteredOverlay> overlays = new ConcurrentHashMap<>();

    @Autowired
    public OverlayRegistryService(AuditTrailService auditTrailService) {
        this(Clock.systemUTC(), auditTrailService);
    }

    public OverlayRegistryService(Clock clock, AuditTrailService auditTrailService) {
        this.clock = clock;
        this.auditTrailService = auditTrailService;
    }

    public RegisteredOverlay apply(OverlaySpec spec, String actor) {
        Map<String, String> metadata = new LinkedHashMap<>(spec.metadata());
        metadata.put("actor", actor);
        metadata.put("selectorType", spec.selectorType());
        RegisteredOverlay registered = new RegisteredOverlay(
                spec.overlayId(),
                spec,
                ApprovalState.PENDING,
                Instant.now(clock).toString(),
                null,
                Map.copyOf(metadata)
        );
        overlays.put(registered.overlayId(), registered);
        auditTrailService.record(AuditEventType.OVERLAY_APPLIED, actor, registered.overlayId(), registered.metadata());
        return registered;
    }

    public RegisteredOverlay disable(String overlayId, String actor) {
        RegisteredOverlay current = overlays.get(overlayId);
        if (current == null) {
            throw new IllegalArgumentException("No overlay registered for " + overlayId);
        }
        RegisteredOverlay disabled = new RegisteredOverlay(
                current.overlayId(),
                current.spec(),
                ApprovalState.DISABLED,
                current.createdAt(),
                Instant.now(clock).toString(),
                current.metadata()
        );
        overlays.put(disabled.overlayId(), disabled);
        auditTrailService.record(AuditEventType.OVERLAY_DISABLED, actor, disabled.overlayId(), disabled.metadata());
        return disabled;
    }

    public RegisteredOverlay approve(String overlayId, String actor) {
        RegisteredOverlay current = overlays.get(overlayId);
        if (current == null) {
            throw new IllegalArgumentException("No overlay registered for " + overlayId);
        }
        if (ApprovalState.DISABLED.equals(current.approvalState())) {
            throw new IllegalStateException("Overlay " + overlayId + " has been disabled");
        }
        if (ApprovalState.APPROVED.equals(current.approvalState())) {
            return current;
        }
        RegisteredOverlay approved = new RegisteredOverlay(
                current.overlayId(),
                current.spec(),
                ApprovalState.APPROVED,
                current.createdAt(),
                current.disabledAt(),
                current.metadata()
        );
        overlays.put(approved.overlayId(), approved);
        auditTrailService.record(AuditEventType.OVERLAY_APPROVED, actor, approved.overlayId(), approved.metadata());
        return approved;
    }

    public List<RegisteredOverlay> listActive() {
        return overlays.values().stream()
                .filter(overlay -> !ApprovalState.DISABLED.equals(overlay.approvalState()))
                .sorted(Comparator.comparing(RegisteredOverlay::overlayId))
                .toList();
    }

    public List<RegisteredOverlay> listActive(String applicationId, String instanceId) {
        return listActive().stream()
                .filter(overlay -> matchesTarget(overlay, applicationId, instanceId))
                .toList();
    }

    public List<RegisteredOverlay> listDeliverable(String applicationId, String instanceId) {
        return listActive(applicationId, instanceId).stream()
                .filter(this::isDeliverable)
                .toList();
    }

    private boolean matchesTarget(RegisteredOverlay overlay, String applicationId, String instanceId) {
        String overlayApplicationId = overlay.spec().metadata().get("applicationId");
        if (overlayApplicationId == null || overlayApplicationId.isBlank()) {
            return false;
        }
        if (!overlayApplicationId.equals(applicationId)) {
            return false;
        }
        String overlayInstanceId = overlay.spec().metadata().get("instanceId");
        if (instanceId == null || instanceId.isBlank()) {
            return true;
        }
        return overlayInstanceId == null || overlayInstanceId.isBlank() || overlayInstanceId.equals(instanceId);
    }

    private boolean isDeliverable(RegisteredOverlay overlay) {
        if (!ApprovalState.APPROVED.equals(overlay.approvalState())) {
            return false;
        }
        if (!overlay.spec().enabled()) {
            return false;
        }
        String ttl = overlay.spec().ttl();
        if (ttl == null || ttl.isBlank()) {
            return true;
        }
        try {
            Instant expiresAt = Instant.parse(overlay.createdAt()).plus(Duration.parse(ttl));
            return expiresAt.isAfter(Instant.now(clock));
        }
        catch (RuntimeException ex) {
            return false;
        }
    }
}
