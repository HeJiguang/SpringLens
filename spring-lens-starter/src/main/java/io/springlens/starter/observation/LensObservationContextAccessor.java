package io.springlens.starter.observation;

import io.micrometer.observation.Observation;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.server.observation.ServerRequestObservationContext;

public class LensObservationContextAccessor {

    static final String EXECUTION_ID_KEY = LensObservationContextAccessor.class.getName() + ".executionId";
    static final String STARTED_AT_KEY = LensObservationContextAccessor.class.getName() + ".startedAt";
    static final String TRACE_ID_KEY = LensObservationContextAccessor.class.getName() + ".traceId";
    static final String SPAN_ID_KEY = LensObservationContextAccessor.class.getName() + ".spanId";
    static final String PARENT_SPAN_ID_KEY = LensObservationContextAccessor.class.getName() + ".parentSpanId";

    public boolean supports(Observation.Context context) {
        return context instanceof ServerRequestObservationContext;
    }

    public String executionId(Observation.Context context) {
        return context.computeIfAbsent(EXECUTION_ID_KEY, ignored -> UUID.randomUUID().toString());
    }

    public String traceId(Observation.Context context, String fallback) {
        return stringValue(context.getOrDefault(TRACE_ID_KEY, fallback), fallback);
    }

    public String spanId(Observation.Context context) {
        return stringValue(context.get(SPAN_ID_KEY), null);
    }

    public String parentSpanId(Observation.Context context) {
        return stringValue(context.get(PARENT_SPAN_ID_KEY), null);
    }

    public void markStarted(Observation.Context context, Instant startedAt) {
        context.put(STARTED_AT_KEY, startedAt);
    }

    public long durationMs(Observation.Context context, Instant finishedAt) {
        Instant startedAt = context.get(STARTED_AT_KEY);
        return startedAt == null ? 0L : Math.max(0L, Duration.between(startedAt, finishedAt).toMillis());
    }

    public String requestPath(Observation.Context context) {
        if (context instanceof ServerRequestObservationContext serverContext) {
            if (serverContext.getPathPattern() != null && !serverContext.getPathPattern().isBlank()) {
                return serverContext.getPathPattern();
            }
            if (serverContext.getCarrier() != null) {
                return serverContext.getCarrier().getRequestURI();
            }
        }
        return stringValue(context.getOrDefault("path", "unknown"), "unknown");
    }

    public String requestMethod(Observation.Context context) {
        if (context instanceof ServerRequestObservationContext serverContext && serverContext.getCarrier() != null) {
            return serverContext.getCarrier().getMethod();
        }
        return stringValue(context.getOrDefault("method", "GET"), "GET");
    }

    public int statusCode(Observation.Context context) {
        if (context instanceof ServerRequestObservationContext serverContext && serverContext.getResponse() != null) {
            return serverContext.getResponse().getStatus();
        }
        Object status = context.get("status");
        return status instanceof Number number ? number.intValue() : 200;
    }

    private String stringValue(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String stringValue = String.valueOf(value).trim();
        return stringValue.isEmpty() ? fallback : stringValue;
    }
}
