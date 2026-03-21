package io.springlens.spi;

import io.springlens.model.ProbeDescriptor;
import java.util.List;

public record CapabilityToolGenerationRequest(
        List<ControllerMappingMetadata> controllerMappings,
        List<ProbeDescriptor> probeDefinitions
) {

    public CapabilityToolGenerationRequest {
        controllerMappings = controllerMappings == null ? List.of() : List.copyOf(controllerMappings);
        probeDefinitions = probeDefinitions == null ? List.of() : List.copyOf(probeDefinitions);
    }
}
