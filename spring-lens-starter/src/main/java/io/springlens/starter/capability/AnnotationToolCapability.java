package io.springlens.starter.capability;

import io.springlens.spi.CapabilityContribution;
import io.springlens.spi.CapabilityDescriptor;
import io.springlens.spi.CapabilityKind;
import io.springlens.spi.CapabilitySource;
import io.springlens.spi.LensCallableTool;
import io.springlens.spi.LensCapability;
import io.springlens.spi.ToolMetadata;
import io.springlens.spi.ToolRequest;
import io.springlens.spi.ToolSchema;
import io.springlens.starter.probe.LensTool;
import io.springlens.starter.probe.LensToolParam;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.ObjectMapper;

public class AnnotationToolCapability implements LensCapability {

    public static final String CAPABILITY_ID = "spring-lens.annotation-tools";

    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;

    public AnnotationToolCapability(ApplicationContext applicationContext, ObjectMapper objectMapper) {
        this.applicationContext = applicationContext;
        this.objectMapper = objectMapper;
    }

    @Override
    public CapabilityContribution contribute() {
        return new CapabilityContribution(
                new CapabilityDescriptor(
                        CAPABILITY_ID,
                        "Annotation Tool Capability",
                        "Expose @LensTool runtime functions discovered in application code.",
                        CapabilityKind.OBSERVABILITY,
                        CapabilitySource.APPLICATION
                ),
                List.of(),
                scanTools()
        );
    }

    private List<LensCallableTool> scanTools() {
        Map<String, LensCallableTool> toolsByName = new LinkedHashMap<>();
        applicationContext.getBeansOfType(Object.class).values().forEach(bean -> {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            for (Method method : targetClass.getMethods()) {
                LensTool annotation = method.getAnnotation(LensTool.class);
                if (annotation == null) {
                    continue;
                }
                LensCallableTool previous = toolsByName.putIfAbsent(
                        annotation.name(),
                        new RegisteredAnnotationTool(bean, method, annotation, objectMapper)
                );
                if (previous != null) {
                    throw new IllegalStateException("Duplicate @LensTool runtime tool registered for " + annotation.name());
                }
            }
        });
        return List.copyOf(toolsByName.values());
    }

    private record RegisteredAnnotationTool(
            Object bean,
            Method method,
            LensTool annotation,
            ObjectMapper objectMapper
    ) implements LensCallableTool {

        @Override
        public ToolMetadata metadata() {
            return ToolMetadata.publiclyExposed(annotation.name(), annotation.description());
        }

        @Override
        public ToolSchema schema() {
            return RuntimeToolSchemaFactory.handwrittenToolSchema(method, annotation.name(), annotation.description());
        }

        @Override
        public Object execute(ToolRequest request) {
            try {
                Map<String, Object> arguments = request.arguments() == null ? Map.of() : request.arguments();
                return method.invoke(bean, resolveArguments(arguments));
            }
            catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Failed to invoke @LensTool runtime tool " + annotation.name(), ex);
            }
        }

        private Object[] resolveArguments(Map<String, Object> arguments) {
            Parameter[] parameters = method.getParameters();
            List<Object> resolved = new ArrayList<>(parameters.length);
            for (Parameter parameter : parameters) {
                LensToolParam annotation = parameter.getAnnotation(LensToolParam.class);
                String argumentName = annotation != null ? annotation.value() : parameter.getName();
                resolved.add(objectMapper.convertValue(arguments.get(argumentName), parameter.getType()));
            }
            return resolved.toArray();
        }
    }
}
