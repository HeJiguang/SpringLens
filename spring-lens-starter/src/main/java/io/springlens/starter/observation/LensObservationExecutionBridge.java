package io.springlens.starter.observation;

import io.micrometer.observation.Observation;
import io.springlens.model.core.ExecutionContext;
import io.springlens.model.core.ExecutionEntrypointKind;
import io.springlens.model.core.ExecutionOriginKind;
import io.springlens.model.core.ExecutionTransportKind;
import io.springlens.runtime.RuntimeSignalProcessor;
import io.springlens.spi.RuntimeSignal;
import io.springlens.spi.RuntimeSignalType;
import io.springlens.starter.LensRuntimeProperties;
import io.springlens.starter.RuntimeExecutionContextHolder;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.core.env.Environment;

public class LensObservationExecutionBridge {

    private final RuntimeSignalProcessor signalProcessor;
    private final RuntimeExecutionContextHolder contextHolder;
    private final LensRuntimeProperties properties;
    private final Environment environment;
    private final LensObservationContextAccessor contextAccessor;

    public LensObservationExecutionBridge(
            RuntimeSignalProcessor signalProcessor,
            RuntimeExecutionContextHolder contextHolder,
            LensRuntimeProperties properties,
            Environment environment,
            LensObservationContextAccessor contextAccessor
    ) {
        this.signalProcessor = signalProcessor;
        this.contextHolder = contextHolder;
        this.properties = properties;
        this.environment = environment;
        this.contextAccessor = contextAccessor;
    }

    public void onStart(Observation.Context context) {
        Instant startedAt = Instant.now();
        String executionId = contextAccessor.executionId(context);
        String applicationId = applicationId();
        String requestPath = contextAccessor.requestPath(context);
        String requestMethod = contextAccessor.requestMethod(context);
        contextAccessor.markStarted(context, startedAt);
        contextHolder.set(executionId);

        signalProcessor.start(new ExecutionContext(
                applicationId,
                properties.getInstanceId(),
                executionId,
                contextAccessor.traceId(context, executionId),
                contextAccessor.spanId(context),
                contextAccessor.parentSpanId(context),
                ExecutionEntrypointKind.HTTP_SERVER,
                ExecutionTransportKind.HTTP,
                applicationId,
                environment.getProperty("spring.profiles.active"),
                startedAt,
                Map.of("path", requestPath, "method", requestMethod),
                List.of(),
                List.of(),
                "observation-native"
        ));
        signalProcessor.process(new RuntimeSignal(
                executionId,
                RuntimeSignalType.HTTP_REQUEST_STARTED,
                startedAt,
                Map.of(
                        "method", requestMethod,
                        "path", requestPath,
                        "_originKind", ExecutionOriginKind.OBSERVATION.value(),
                        "_sourceRef", "org.springframework.http.server.observation.ServerRequestObservationContext"
                )
        ));
    }

    public void onStop(Observation.Context context) {
        Instant finishedAt = Instant.now();
        String executionId = contextAccessor.executionId(context);
        signalProcessor.process(new RuntimeSignal(
                executionId,
                RuntimeSignalType.HTTP_REQUEST_COMPLETED,
                finishedAt,
                Map.of(
                        "status", contextAccessor.statusCode(context),
                        "durationMs", contextAccessor.durationMs(context, finishedAt)
                )
        ));
        contextHolder.clear();
    }

    private String applicationId() {
        if (properties.getApplicationId() != null && !properties.getApplicationId().isBlank()) {
            return properties.getApplicationId();
        }
        return environment.getProperty("spring.application.name", "spring-lens-app");
    }
}
