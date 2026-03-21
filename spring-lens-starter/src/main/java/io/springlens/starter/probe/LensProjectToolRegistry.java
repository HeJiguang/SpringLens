package io.springlens.starter.probe;

import io.springlens.model.ProjectToolDescriptor;
import io.springlens.model.ProjectToolSchemaDescriptor;
import io.springlens.spi.ControllerMappingMetadata;
import io.springlens.spi.GeneratedSkillTool;
import io.springlens.spi.SkillGenerationRequest;
import io.springlens.spi.SkillGenerator;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tools.jackson.databind.ObjectMapper;

/**
 * Registry for explicit {@link LensTool} methods and generated opt-in diagnostic skills.
 */
public class LensProjectToolRegistry {

    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;
    private final LensProbeRegistry probeRegistry;
    @Nullable
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final List<SkillGenerator> skillGenerators;
    private final Map<String, ProjectToolHandler> tools = new ConcurrentHashMap<>();

    public LensProjectToolRegistry(
            ApplicationContext applicationContext,
            ObjectMapper objectMapper,
            LensProbeRegistry probeRegistry,
            @Nullable RequestMappingHandlerMapping requestMappingHandlerMapping,
            List<SkillGenerator> skillGenerators
    ) {
        this.applicationContext = applicationContext;
        this.objectMapper = objectMapper;
        this.probeRegistry = probeRegistry;
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
        this.skillGenerators = skillGenerators == null ? List.of() : List.copyOf(skillGenerators);
        scan();
    }

    public List<ProjectToolDescriptor> list() {
        registerGeneratedTools();
        return tools.values().stream()
                .map(ProjectToolHandler::descriptor)
                .sorted((left, right) -> left.name().compareTo(right.name()))
                .toList();
    }

    public List<ProjectToolSchemaDescriptor> listSchemas() {
        registerGeneratedTools();
        return tools.values().stream()
                .map(ProjectToolHandler::schemaDescriptor)
                .sorted((left, right) -> left.name().compareTo(right.name()))
                .toList();
    }

    public Object invoke(String toolName, Map<String, Object> arguments) {
        registerGeneratedTools();
        ProjectToolHandler tool = tools.get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("No project tool registered for " + toolName);
        }
        return tool.invoke(arguments == null ? Map.of() : arguments);
    }

    private void scan() {
        applicationContext.getBeansOfType(Object.class).values().forEach(bean -> {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            for (Method method : targetClass.getMethods()) {
                LensTool tool = method.getAnnotation(LensTool.class);
                if (tool == null) {
                    continue;
                }
                tools.put(tool.name(), new RegisteredProjectTool(bean, method, tool, objectMapper));
            }
        });
        registerGeneratedTools();
    }

    private void registerGeneratedTools() {
        if (requestMappingHandlerMapping == null || skillGenerators.isEmpty()) {
            return;
        }

        List<ControllerMappingMetadata> controllerMappings = requestMappingHandlerMapping.getHandlerMethods().entrySet().stream()
                .map(this::toControllerMapping)
                .filter(Objects::nonNull)
                .toList();
        if (controllerMappings.isEmpty()) {
            return;
        }

        SkillGenerationRequest request = new SkillGenerationRequest(controllerMappings, probeRegistry.list());
        for (SkillGenerator skillGenerator : skillGenerators) {
            for (GeneratedSkillTool generatedTool : skillGenerator.generate(request)) {
                tools.putIfAbsent(generatedTool.name(), new GeneratedProjectTool(generatedTool));
            }
        }
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

    private interface ProjectToolHandler {

        ProjectToolDescriptor descriptor();

        ProjectToolSchemaDescriptor schemaDescriptor();

        Object invoke(Map<String, Object> arguments);
    }

    private record RegisteredProjectTool(
            Object bean,
            Method method,
            LensTool annotation,
            ObjectMapper objectMapper
    ) implements ProjectToolHandler {

        @Override
        public ProjectToolDescriptor descriptor() {
            return new ProjectToolDescriptor(annotation.name(), annotation.description());
        }

        @Override
        public ProjectToolSchemaDescriptor schemaDescriptor() {
            return ProjectToolSchemaFactory.handwrittenToolSchema(method, annotation.name(), annotation.description());
        }

        @Override
        public Object invoke(Map<String, Object> arguments) {
            try {
                return method.invoke(bean, resolveArguments(arguments));
            }
            catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Failed to invoke project tool " + annotation.name(), ex);
            }
        }

        private Object[] resolveArguments(Map<String, Object> arguments) {
            Parameter[] parameters = method.getParameters();
            List<Object> resolved = new ArrayList<>(parameters.length);
            for (Parameter parameter : parameters) {
                LensToolParam annotation = parameter.getAnnotation(LensToolParam.class);
                String argumentName = annotation != null ? annotation.value() : parameter.getName();
                Object rawValue = arguments.get(argumentName);
                resolved.add(objectMapper.convertValue(rawValue, parameter.getType()));
            }
            return resolved.toArray();
        }
    }

    private record GeneratedProjectTool(GeneratedSkillTool tool) implements ProjectToolHandler {

        @Override
        public ProjectToolDescriptor descriptor() {
            return tool.descriptor();
        }

        @Override
        public ProjectToolSchemaDescriptor schemaDescriptor() {
            return tool.schemaDescriptor();
        }

        @Override
        public Object invoke(Map<String, Object> arguments) {
            return tool.invoke(arguments);
        }
    }
}
