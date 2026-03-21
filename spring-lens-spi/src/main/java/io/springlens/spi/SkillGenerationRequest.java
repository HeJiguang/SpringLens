package io.springlens.spi;

import io.springlens.model.ProbeDescriptor;
import java.util.List;

public record SkillGenerationRequest(
        List<ControllerMappingMetadata> controllerMappings,
        List<ProbeDescriptor> probeDefinitions
) {

    public SkillGenerationRequest {
        controllerMappings = controllerMappings == null ? List.of() : List.copyOf(controllerMappings);
        probeDefinitions = probeDefinitions == null ? List.of() : List.copyOf(probeDefinitions);
    }
}
