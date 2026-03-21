package io.springlens.spi;

import io.springlens.model.ProbeDescriptor;
import java.util.List;
import java.util.Objects;

public record CapabilityContribution(
        CapabilityDescriptor descriptor,
        List<ProbeDescriptor> probes,
        List<LensCallableTool> tools
) {

    public CapabilityContribution {
        descriptor = Objects.requireNonNull(descriptor, "descriptor");
        probes = probes == null ? List.of() : List.copyOf(probes);
        tools = tools == null ? List.of() : List.copyOf(tools);
    }
}
