package io.springlens.agent.starter;

import io.springlens.model.diagnostic.ProbeCapturePhase;
import io.springlens.starter.probe.LensProbeCaptureService;
import java.lang.reflect.Method;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;

public class AgentOverlayMethodInterceptor implements MethodInterceptor {

    private final AgentOverlayEngine overlayEngine;
    private final LensProbeCaptureService captureService;
    private final AgentOverlayValueResolver valueResolver;

    public AgentOverlayMethodInterceptor(
            AgentOverlayEngine overlayEngine,
            LensProbeCaptureService captureService,
            AgentOverlayValueResolver valueResolver
    ) {
        this.overlayEngine = overlayEngine;
        this.captureService = captureService;
        this.valueResolver = valueResolver;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Class<?> targetClass = resolveTargetClass(invocation);
        Method method = invocation.getMethod();

        overlayEngine.beanMethodOverlays(targetClass, method, ProbeCapturePhase.BEFORE)
                .forEach(overlay -> captureService.captureAgentOverlay(
                        overlay.overlayId(),
                        overlay.probeId(),
                        overlay.description(),
                        ProbeCapturePhase.BEFORE,
                        valueResolver.resolveMethodExpression(overlay.expression(), invocation.getArguments(), null, null)
                ));

        try {
            Object result = invocation.proceed();
            overlayEngine.beanMethodOverlays(targetClass, method, ProbeCapturePhase.AFTER_RETURN)
                    .forEach(overlay -> captureService.captureAgentOverlay(
                            overlay.overlayId(),
                            overlay.probeId(),
                            overlay.description(),
                            ProbeCapturePhase.AFTER_RETURN,
                            valueResolver.resolveMethodExpression(overlay.expression(), invocation.getArguments(), result, null)
                    ));
            return result;
        }
        catch (Throwable error) {
            overlayEngine.beanMethodOverlays(targetClass, method, ProbeCapturePhase.AFTER_THROW)
                    .forEach(overlay -> captureService.captureAgentOverlay(
                            overlay.overlayId(),
                            overlay.probeId(),
                            overlay.description(),
                            ProbeCapturePhase.AFTER_THROW,
                            valueResolver.resolveMethodExpression(overlay.expression(), invocation.getArguments(), null, error)
                    ));
            throw error;
        }
    }

    private Class<?> resolveTargetClass(MethodInvocation invocation) {
        Object thisObject = invocation.getThis();
        return thisObject == null ? invocation.getMethod().getDeclaringClass() : AopUtils.getTargetClass(thisObject);
    }
}
