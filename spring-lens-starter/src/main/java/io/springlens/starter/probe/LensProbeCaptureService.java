package io.springlens.starter.probe;
import io.springlens.model.diagnostic.ProbeCapturePhase;
import io.springlens.runtime.RuntimeSignalProcessor;
import io.springlens.spi.RuntimeSignal;
import io.springlens.spi.RuntimeSignalType;
import io.springlens.starter.RuntimeExecutionContextHolder;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class LensProbeCaptureService implements Lens.LensOperations {

    private final RuntimeSignalProcessor signalProcessor;
    private final RuntimeExecutionContextHolder contextHolder;
    private final LensProbeRegistry probeRegistry;
    private final LensValueSanitizer valueSanitizer;

    public LensProbeCaptureService(
            RuntimeSignalProcessor signalProcessor,
            RuntimeExecutionContextHolder contextHolder,
            LensProbeRegistry probeRegistry,
            LensValueSanitizer valueSanitizer
    ) {
        this.signalProcessor = signalProcessor;
        this.contextHolder = contextHolder;
        this.probeRegistry = probeRegistry;
        this.valueSanitizer = valueSanitizer;
        Lens.bind(this);
    }

    public void captureAnnotation(LensWatch watch, ProbeCapturePhase phase, Object value) {
        probeRegistry.registerAnnotation(watch);
        emit(watch.id(), watch.description(), phase, "annotation", value, Map.of());
    }

    public void captureAgentOverlay(
            String overlayId,
            String probeId,
            String description,
            ProbeCapturePhase phase,
            Object value
    ) {
        String normalizedProbeId = normalize(probeId, overlayId);
        String normalizedDescription = normalize(description, "");
        probeRegistry.registerAgentOverlay(overlayId, normalizedProbeId, normalizedDescription, phase);
        emit(
                normalizedProbeId,
                normalizedDescription,
                phase,
                "agent-overlay",
                value,
                Map.of("overlayId", overlayId)
        );
    }

    @Override
    public void look(String id, Object value, String description) {
        probeRegistry.registerManual(id, description);
        emit(id, description, ProbeCapturePhase.MANUAL, "manual", value, Map.of());
    }

    private void emit(
            String probeId,
            String description,
            ProbeCapturePhase phase,
            String captureSource,
            Object value,
            Map<String, Object> extraAttributes
    ) {
        contextHolder.currentExecutionId().ifPresent(executionId -> {
            LensValueSanitizer.SanitizedValue sanitized = valueSanitizer.sanitize(value);

            Map<String, Object> attributes = new LinkedHashMap<>();
            attributes.put("probeId", probeId);
            attributes.put("description", description);
            attributes.put("captureSource", captureSource);
            attributes.put("capturePhase", phase.value());
            attributes.put("value", sanitized.value());
            attributes.put("valueType", sanitized.valueType());
            attributes.putAll(extraAttributes);

            signalProcessor.process(new RuntimeSignal(
                    executionId,
                    RuntimeSignalType.PROBE_VALUE_CAPTURED,
                    Instant.now(),
                    attributes
            ));
        });
    }

    private String normalize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
