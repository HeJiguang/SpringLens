package io.springlens.agent.starter;

import io.springlens.agent.contract.AgentInstrumentationMode;
import io.springlens.agent.contract.ApprovalState;
import io.springlens.agent.contract.OverlayDeliverySnapshot;
import io.springlens.agent.contract.OverlaySpec;
import io.springlens.agent.contract.PolicySnapshot;
import io.springlens.agent.contract.RegisteredOverlay;
import io.springlens.model.diagnostic.ProbeCapturePhase;
import io.springlens.starter.LensRuntimeProperties;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.util.AntPathMatcher;

public class AgentOverlayEngine {

    private static final String SELECTOR_BEAN_METHOD = "spring-bean-method";
    private static final String SELECTOR_HTTP_ROUTE = "http-route";

    private final AgentInstrumentationProperties properties;
    private final LensRuntimeProperties runtimeProperties;
    private final Clock clock;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private volatile List<OverlaySpec> activeOverlays = List.of();
    private volatile PolicySnapshot lastPolicy;
    private volatile String lastDeliveredAt;

    public AgentOverlayEngine(AgentInstrumentationProperties properties, LensRuntimeProperties runtimeProperties) {
        this(properties, runtimeProperties, Clock.systemUTC());
    }

    public AgentOverlayEngine(
            AgentInstrumentationProperties properties,
            LensRuntimeProperties runtimeProperties,
            Clock clock
    ) {
        this.properties = properties;
        this.runtimeProperties = runtimeProperties;
        this.clock = clock;
    }

    public boolean isEnabled() {
        return properties.isEnabled() && !AgentInstrumentationMode.OFF.equals(properties.getMode());
    }

    public AgentInstrumentationMode mode() {
        return properties.getMode();
    }

    public List<OverlaySpec> activeOverlays() {
        return activeOverlays;
    }

    public PolicySnapshot lastPolicy() {
        return lastPolicy;
    }

    public String lastDeliveredAt() {
        return lastDeliveredAt;
    }

    public void applySnapshot(OverlayDeliverySnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        activeOverlays = snapshot.activeOverlays().stream()
                .filter(this::accepts)
                .map(RegisteredOverlay::spec)
                .filter(this::accepts)
                .toList();
        lastPolicy = snapshot.policy();
        lastDeliveredAt = snapshot.deliveredAt();
    }

    public boolean accepts(RegisteredOverlay overlay) {
        if (overlay == null || !ApprovalState.APPROVED.equals(overlay.approvalState())) {
            return false;
        }
        return !isExpired(overlay);
    }

    public boolean accepts(OverlaySpec overlaySpec) {
        if (!isEnabled() || overlaySpec == null || !overlaySpec.enabled()) {
            return false;
        }
        String applicationId = normalize(runtimeProperties.getApplicationId());
        String overlayApplicationId = normalize(overlaySpec.metadata().get("applicationId"));
        if (applicationId == null || !Objects.equals(applicationId, overlayApplicationId)) {
            return false;
        }
        String instanceId = normalize(runtimeProperties.getInstanceId());
        String overlayInstanceId = normalize(overlaySpec.metadata().get("instanceId"));
        return overlayInstanceId == null || instanceId == null || overlayInstanceId.equals(instanceId);
    }

    public List<OverlaySpec> beanMethodOverlays(Class<?> targetClass, Method method, ProbeCapturePhase phase) {
        return activeOverlays.stream()
                .filter(overlay -> SELECTOR_BEAN_METHOD.equals(overlay.selectorType()))
                .filter(overlay -> matchesPhase(overlay, phase))
                .filter(overlay -> matchesBeanMethod(overlay, targetClass, method))
                .toList();
    }

    public List<OverlaySpec> httpRouteOverlays(String requestPath, String httpMethod, ProbeCapturePhase phase) {
        return activeOverlays.stream()
                .filter(overlay -> SELECTOR_HTTP_ROUTE.equals(overlay.selectorType()))
                .filter(overlay -> matchesPhase(overlay, phase))
                .filter(overlay -> matchesHttpRoute(overlay, requestPath, httpMethod))
                .toList();
    }

    private boolean matchesPhase(OverlaySpec overlay, ProbeCapturePhase phase) {
        String capturePhase = normalize(overlay.capturePhase());
        return capturePhase != null && phase.equals(ProbeCapturePhase.of(capturePhase));
    }

    private boolean matchesBeanMethod(OverlaySpec overlay, Class<?> targetClass, Method method) {
        String targetClassName = normalize(overlay.targetClassName());
        if (targetClassName != null && !targetClassName.equals(targetClass.getName())) {
            return false;
        }
        String targetMethodName = normalize(overlay.targetMethodName());
        return targetMethodName == null || targetMethodName.equals(method.getName());
    }

    private boolean matchesHttpRoute(OverlaySpec overlay, String requestPath, String httpMethod) {
        String pathPattern = normalize(overlay.targetClassName());
        if (pathPattern == null || !pathMatcher.match(pathPattern, requestPath)) {
            return false;
        }
        String targetMethodName = normalize(overlay.targetMethodName());
        return targetMethodName == null || targetMethodName.equalsIgnoreCase(httpMethod);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean isExpired(RegisteredOverlay overlay) {
        String ttl = normalize(overlay.spec().ttl());
        if (ttl == null) {
            return false;
        }
        try {
            Instant expiresAt = Instant.parse(overlay.createdAt()).plus(Duration.parse(ttl));
            return !expiresAt.isAfter(Instant.now(clock));
        }
        catch (RuntimeException ex) {
            return true;
        }
    }
}
