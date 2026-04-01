package io.springlens.starter.probe;

import io.springlens.model.ProbeDescriptor;
import io.springlens.model.diagnostic.ProbeCapturePhase;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;

public class LensProbeRegistry {

    private final Map<String, ProbeDescriptor> probes = new ConcurrentHashMap<>();

    public LensProbeRegistry(ApplicationContext applicationContext) {
        applicationContext.getBeansOfType(Object.class).values().forEach(bean -> {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            for (Method method : targetClass.getMethods()) {
                LensWatch watch = method.getAnnotation(LensWatch.class);
                if (watch != null) {
                    registerAnnotation(watch);
                }
            }
        });
    }

    public ProbeDescriptor registerAnnotation(LensWatch watch) {
        return register(watch.id(), watch.description(), "annotation", ProbeCapturePhase.of(watch.phase()));
    }

    public ProbeDescriptor registerManual(String id, String description) {
        return register(id, description, "manual", ProbeCapturePhase.MANUAL);
    }

    public ProbeDescriptor registerAgentOverlay(String overlayId, String id, String description, ProbeCapturePhase phase) {
        return register(id, description, "agent-overlay:" + overlayId, phase);
    }

    public List<ProbeDescriptor> list() {
        return probes.values().stream()
                .sorted((left, right) -> left.probeId().compareTo(right.probeId()))
                .toList();
    }

    private ProbeDescriptor register(String id, String description, String captureSource, ProbeCapturePhase phase) {
        return probes.computeIfAbsent(id, ignored -> new ProbeDescriptor(
                id,
                description,
                captureSource,
                phase
        ));
    }
}
