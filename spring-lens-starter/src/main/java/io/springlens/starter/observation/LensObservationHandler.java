package io.springlens.starter.observation;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;

public class LensObservationHandler implements ObservationHandler<Observation.Context> {

    private final LensObservationExecutionBridge executionBridge;
    private final LensObservationContextAccessor contextAccessor;

    public LensObservationHandler(
            LensObservationExecutionBridge executionBridge,
            LensObservationContextAccessor contextAccessor
    ) {
        this.executionBridge = executionBridge;
        this.contextAccessor = contextAccessor;
    }

    @Override
    public void onStart(Observation.Context context) {
        if (supportsContext(context)) {
            executionBridge.onStart(context);
        }
    }

    @Override
    public void onStop(Observation.Context context) {
        if (supportsContext(context)) {
            executionBridge.onStop(context);
        }
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return contextAccessor.supports(context);
    }
}
