package io.springlens.starter.capability;

import io.springlens.model.ProbeDescriptor;
import io.springlens.model.RuntimeCapabilityDescriptor;
import io.springlens.model.RuntimeToolDescriptor;
import io.springlens.model.RuntimeToolSchemaDescriptor;
import io.springlens.spi.CapabilityContribution;
import io.springlens.spi.LensCallableTool;
import io.springlens.spi.LensCapability;
import io.springlens.spi.ToolRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LensCapabilityRegistry {

    private final List<CapabilityContribution> contributions;
    private final Map<String, RegisteredTool> toolsByName;

    public LensCapabilityRegistry(List<LensCapability> capabilities) {
        this.contributions = (capabilities == null ? List.<LensCapability>of() : List.copyOf(capabilities)).stream()
                .map(LensCapability::contribute)
                .sorted((left, right) -> left.descriptor().id().compareTo(right.descriptor().id()))
                .toList();
        this.toolsByName = indexTools(contributions);
    }

    public List<RuntimeCapabilityDescriptor> capabilities() {
        return contributions.stream()
                .map(contribution -> new RuntimeCapabilityDescriptor(
                        contribution.descriptor().id(),
                        contribution.descriptor().name(),
                        contribution.descriptor().description(),
                        contribution.descriptor().kind().value(),
                        contribution.descriptor().source().value()
                ))
                .toList();
    }

    public List<RuntimeToolDescriptor> tools() {
        return toolsByName.values().stream()
                .map(registeredTool -> new RuntimeToolDescriptor(
                        registeredTool.tool().metadata().name(),
                        registeredTool.tool().metadata().description(),
                        registeredTool.capabilityId()
                ))
                .sorted((left, right) -> left.name().compareTo(right.name()))
                .toList();
    }

    public List<RuntimeToolSchemaDescriptor> toolSchemas() {
        return toolsByName.values().stream()
                .map(registeredTool -> new RuntimeToolSchemaDescriptor(
                        registeredTool.tool().schema().name(),
                        registeredTool.tool().schema().description(),
                        registeredTool.tool().schema().inputSchema(),
                        registeredTool.tool().schema().outputSchema(),
                        registeredTool.capabilityId()
                ))
                .sorted((left, right) -> left.name().compareTo(right.name()))
                .toList();
    }

    public List<ProbeDescriptor> probes() {
        return contributions.stream()
                .flatMap(contribution -> contribution.probes().stream())
                .sorted((left, right) -> left.probeId().compareTo(right.probeId()))
                .toList();
    }

    public Object invoke(String toolName, Map<String, Object> arguments) {
        RegisteredTool tool = toolsByName.get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("No runtime tool registered for " + toolName);
        }
        return tool.tool().execute(new ToolRequest(null, null, arguments));
    }

    private Map<String, RegisteredTool> indexTools(List<CapabilityContribution> contributions) {
        Map<String, RegisteredTool> indexed = new LinkedHashMap<>();
        for (CapabilityContribution contribution : contributions) {
            for (LensCallableTool tool : contribution.tools()) {
                RegisteredTool previous = indexed.putIfAbsent(
                        tool.metadata().name(),
                        new RegisteredTool(contribution.descriptor().id(), tool)
                );
                if (previous != null) {
                    throw new IllegalStateException("Duplicate runtime tool registered for " + tool.metadata().name());
                }
            }
        }
        return indexed;
    }

    private record RegisteredTool(String capabilityId, LensCallableTool tool) {
    }
}
