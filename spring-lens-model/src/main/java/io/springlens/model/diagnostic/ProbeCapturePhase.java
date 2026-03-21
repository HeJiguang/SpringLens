package io.springlens.model.diagnostic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

/**
 * Lifecycle phase when a probe value was captured.
 */
public record ProbeCapturePhase(String value) {

    public static final ProbeCapturePhase BEFORE = of(ProbeCapturePhases.BEFORE);
    public static final ProbeCapturePhase AFTER_RETURN = of(ProbeCapturePhases.AFTER_RETURN);
    public static final ProbeCapturePhase AFTER_THROW = of(ProbeCapturePhases.AFTER_THROW);
    public static final ProbeCapturePhase MANUAL = of(ProbeCapturePhases.MANUAL);

    public ProbeCapturePhase {
        value = normalize(value);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ProbeCapturePhase of(String value) {
        return new ProbeCapturePhase(value);
    }

    @Override
    @JsonValue
    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    private static String normalize(String value) {
        String normalized = Objects.requireNonNull(value, "value").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("value must not be blank");
        }
        return normalized;
    }
}
