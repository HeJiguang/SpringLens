package io.springlens.spi;

import java.util.List;

public interface CapabilityToolGenerator {

    List<LensCallableTool> generate(CapabilityToolGenerationRequest request);
}
