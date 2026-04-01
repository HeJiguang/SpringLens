package io.springlens.agent.starter;

import io.springlens.model.diagnostic.ProbeCapturePhase;
import io.springlens.starter.probe.LensProbeCaptureService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class AgentOverlayHttpInterceptor implements HandlerInterceptor {

    private final AgentOverlayEngine overlayEngine;
    private final LensProbeCaptureService captureService;
    private final AgentOverlayValueResolver valueResolver;

    public AgentOverlayHttpInterceptor(
            AgentOverlayEngine overlayEngine,
            LensProbeCaptureService captureService,
            AgentOverlayValueResolver valueResolver
    ) {
        this.overlayEngine = overlayEngine;
        this.captureService = captureService;
        this.valueResolver = valueResolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        overlayEngine.httpRouteOverlays(request.getRequestURI(), request.getMethod(), ProbeCapturePhase.BEFORE)
                .forEach(overlay -> captureService.captureAgentOverlay(
                        overlay.overlayId(),
                        overlay.probeId(),
                        overlay.description(),
                        ProbeCapturePhase.BEFORE,
                        valueResolver.resolveHttpExpression(overlay.expression(), request, response, null)
                ));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        ProbeCapturePhase phase = ex == null ? ProbeCapturePhase.AFTER_RETURN : ProbeCapturePhase.AFTER_THROW;
        overlayEngine.httpRouteOverlays(request.getRequestURI(), request.getMethod(), phase)
                .forEach(overlay -> captureService.captureAgentOverlay(
                        overlay.overlayId(),
                        overlay.probeId(),
                        overlay.description(),
                        phase,
                        valueResolver.resolveHttpExpression(overlay.expression(), request, response, ex)
                ));
    }
}
