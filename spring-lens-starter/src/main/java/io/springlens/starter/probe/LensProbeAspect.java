package io.springlens.starter.probe;

import io.springlens.model.diagnostic.ProbeCapturePhase;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * Annotation-based probe aspect.
 */
@Aspect
public class LensProbeAspect {

    private final LensProbeCaptureService captureService;

    public LensProbeAspect(LensProbeCaptureService captureService) {
        this.captureService = captureService;
    }

    @Around("@annotation(watch)")
    public Object capture(ProceedingJoinPoint joinPoint, LensWatch watch) throws Throwable {
        ProbeCapturePhase phase = ProbeCapturePhase.of(watch.phase());
        if (ProbeCapturePhase.BEFORE.equals(phase)) {
            captureService.captureAnnotation(watch, ProbeCapturePhase.BEFORE, resolveValue(watch.target(), joinPoint.getArgs(), null, null));
        }

        try {
            Object result = joinPoint.proceed();
            if (ProbeCapturePhase.AFTER_RETURN.equals(phase)) {
                captureService.captureAnnotation(watch, ProbeCapturePhase.AFTER_RETURN, resolveValue(watch.target(), joinPoint.getArgs(), result, null));
            }
            return result;
        }
        catch (Throwable ex) {
            if (ProbeCapturePhase.AFTER_THROW.equals(phase)) {
                captureService.captureAnnotation(watch, ProbeCapturePhase.AFTER_THROW, resolveValue(watch.target(), joinPoint.getArgs(), null, ex));
            }
            throw ex;
        }
    }

    private Object resolveValue(String target, Object[] args, Object result, Throwable ex) {
        if ("#args".equals(target)) {
            return args;
        }
        if (target != null && target.startsWith("#arg")) {
            int index = Integer.parseInt(target.substring(4));
            return index >= 0 && index < args.length ? args[index] : null;
        }
        if ("#result".equals(target)) {
            return result;
        }
        if ("#exception".equals(target)) {
            return ex == null ? null : ex.toString();
        }
        return result;
    }
}
