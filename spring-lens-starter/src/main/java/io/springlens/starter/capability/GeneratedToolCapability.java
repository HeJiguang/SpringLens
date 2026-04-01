package io.springlens.starter.capability;

import io.springlens.model.ProbeDescriptor;
import io.springlens.spi.CapabilityContribution;
import io.springlens.spi.CapabilityDescriptor;
import io.springlens.spi.CapabilityKind;
import io.springlens.spi.CapabilitySource;
import io.springlens.spi.CapabilityToolGenerationRequest;
import io.springlens.spi.CapabilityToolGenerator;
import io.springlens.spi.ControllerMappingMetadata;
import io.springlens.spi.LensCallableTool;
import io.springlens.spi.LensCapability;
import io.springlens.starter.probe.LensProbeRegistry;
import io.springlens.starter.probe.LensSkillSource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

public class GeneratedToolCapability implements LensCapability {

    public static final String CAPABILITY_ID = "spring-lens.generated-tools";

    private final ApplicationContext applicationContext;
    private final LensProbeRegistry probeRegistry;
    @Nullable
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final List<CapabilityToolGenerator> capabilityToolGenerators;

    public GeneratedToolCapability(
            ApplicationContext applicationContext,
            LensProbeRegistry probeRegistry,
            @Nullable RequestMappingHandlerMapping requestMappingHandlerMapping,
            List<CapabilityToolGenerator> capabilityToolGenerators
    ) {
        this.applicationContext = applicationContext;
        this.probeRegistry = probeRegistry;
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
        this.capabilityToolGenerators = capabilityToolGenerators == null ? List.of() : List.copyOf(capabilityToolGenerators);
    }

    @Override
    public CapabilityContribution contribute() {
        return new CapabilityContribution(
                new CapabilityDescriptor(
                        CAPABILITY_ID,
                        "Generated Tool Capability",
                        "Expose generated runtime tools derived from controller mappings and registered probes.",
                        CapabilityKind.OBSERVABILITY,
                        CapabilitySource.GENERATED
                ),
                List.of(),
                generateTools()
        );
    }

    private List<LensCallableTool> generateTools() {
        if (requestMappingHandlerMapping == null || capabilityToolGenerators.isEmpty()) {
            return List.of();
        }

        List<ControllerMappingMetadata> controllerMappings = requestMappingHandlerMapping.getHandlerMethods().entrySet().stream()
                .map(this::toControllerMapping)
                .filter(Objects::nonNull)
                .toList();
        if (controllerMappings.isEmpty()) {
            return List.of();
        }

        CapabilityToolGenerationRequest request = new CapabilityToolGenerationRequest(
                controllerMappings,
                combinedProbeDefinitions()
        );
        Map<String, LensCallableTool> toolsByName = new LinkedHashMap<>();
        for (CapabilityToolGenerator capabilityToolGenerator : capabilityToolGenerators) {
            for (LensCallableTool tool : capabilityToolGenerator.generate(request)) {
                LensCallableTool previous = toolsByName.putIfAbsent(tool.metadata().name(), tool);
                if (previous != null) {
                    throw new IllegalStateException("Duplicate generated runtime tool registered for " + tool.metadata().name());
                }
            }
        }
        return List.copyOf(toolsByName.values());
    }

    private List<ProbeDescriptor> combinedProbeDefinitions() {
        Map<String, ProbeDescriptor> probesById = new LinkedHashMap<>();
        probeRegistry.list().forEach(probe -> probesById.putIfAbsent(probe.probeId(), probe));
        applicationContext.getBeansOfType(LensCapability.class).values().stream()
                .filter(capability -> capability != this)
                .map(LensCapability::contribute)
                .flatMap(contribution -> contribution.probes().stream())
                .forEach(probe -> probesById.putIfAbsent(probe.probeId(), probe));
        return List.copyOf(probesById.values());
    }

    @Nullable
    private ControllerMappingMetadata toControllerMapping(Map.Entry<RequestMappingInfo, HandlerMethod> entry) {
        HandlerMethod handlerMethod = entry.getValue();
        if (!isSkillSource(handlerMethod)) {
            return null;
        }
        List<String> httpMethods = entry.getKey().getMethodsCondition().getMethods().stream()
                .map(Enum::name)
                .sorted()
                .toList();
        List<String> paths = entry.getKey().getPatternValues().stream()
                .sorted()
                .toList();
        return new ControllerMappingMetadata(
                handlerMethod.getBeanType().getName(),
                handlerMethod.getMethod().getName(),
                httpMethods.isEmpty() ? List.of("ALL") : httpMethods,
                paths.isEmpty() ? List.of("/") : paths
        );
    }

    private boolean isSkillSource(HandlerMethod handlerMethod) {
        return AnnotationUtils.findAnnotation(handlerMethod.getMethod(), LensSkillSource.class) != null
                || AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), LensSkillSource.class) != null;
    }
}
