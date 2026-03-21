package io.springlens.starter.probe;

import io.springlens.model.ProbeDescriptor;
import io.springlens.model.ProjectToolSourceType;
import io.springlens.model.core.ExecutionGraph;
import io.springlens.model.diagnostic.ExceptionContextRecord;
import io.springlens.model.diagnostic.ProbeValueRecord;
import io.springlens.model.diagnostic.SlowSqlRecord;
import io.springlens.runtime.InMemoryExecutionGraphStore;
import io.springlens.spi.ControllerMappingMetadata;
import io.springlens.spi.GeneratedSkillTool;
import io.springlens.spi.SkillGenerationRequest;
import io.springlens.spi.SkillGenerator;
import io.springlens.starter.LensRuntimeProperties;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

public class DefaultSkillGenerator implements SkillGenerator {

    private final InMemoryExecutionGraphStore graphStore;
    private final LensRuntimeProperties properties;
    private final PathPatternParser pathPatternParser;

    public DefaultSkillGenerator(InMemoryExecutionGraphStore graphStore, LensRuntimeProperties properties) {
        this.graphStore = graphStore;
        this.properties = properties;
        this.pathPatternParser = new PathPatternParser();
    }

    @Override
    public List<GeneratedSkillTool> generate(SkillGenerationRequest request) {
        Map<String, List<ControllerMappingMetadata>> groupedMappings = new LinkedHashMap<>();
        for (ControllerMappingMetadata mapping : request.controllerMappings()) {
            groupedMappings.computeIfAbsent(mapping.controllerClassName(), ignored -> new ArrayList<>()).add(mapping);
        }

        Map<String, GeneratedSkillTool> tools = new LinkedHashMap<>();
        for (List<ControllerMappingMetadata> mappings : groupedMappings.values()) {
            if (!mappings.isEmpty()) {
                GeneratedSkillTool traceTool = buildTraceTool(mappings, request.probeDefinitions());
                tools.putIfAbsent(traceTool.name(), traceTool);
                for (GeneratedSkillTool probeTool : buildProbeTools(mappings, request.probeDefinitions())) {
                    tools.putIfAbsent(probeTool.name(), probeTool);
                }
            }
        }
        return List.copyOf(tools.values());
    }

    private GeneratedSkillTool buildTraceTool(List<ControllerMappingMetadata> mappings, List<ProbeDescriptor> probes) {
        String controllerClassName = mappings.getFirst().controllerClassName();
        String controllerSimpleName = simpleName(controllerClassName);
        String resourceName = normalizeName(stripControllerSuffix(controllerSimpleName));
        String toolName = "trace_" + resourceName + "_flow";
        List<ProbeDescriptor> relatedProbes = relatedProbes(mappings, probes);
        String description = "Trace request flow for " + controllerSimpleName + " using matching routes and probes.";

        return new GeneratedSkillTool(
                toolName,
                description,
                ProjectToolSourceType.GENERATED_TRACE,
                traceToolInputSchema(),
                traceToolOutputSchema(),
                arguments -> inspectFlow(
                        toolName,
                        controllerClassName,
                        mappings,
                        relatedProbes,
                        arguments
                )
        );
    }

    private List<GeneratedSkillTool> buildProbeTools(List<ControllerMappingMetadata> mappings, List<ProbeDescriptor> probes) {
        return relatedProbes(mappings, probes).stream()
                .map(probe -> buildProbeTool(mappings, probe))
                .toList();
    }

    private GeneratedSkillTool buildProbeTool(List<ControllerMappingMetadata> mappings, ProbeDescriptor probe) {
        String toolName = "query_" + normalizeName(probe.probeId());
        String description = probe.description() == null || probe.description().isBlank()
                ? "Query recent values captured by probe " + probe.probeId() + "."
                : "Query recent values captured by probe " + probe.probeId() + ": " + probe.description();
        return new GeneratedSkillTool(
                toolName,
                description,
                ProjectToolSourceType.GENERATED_PROBE,
                probeToolInputSchema(),
                probeToolOutputSchema(),
                arguments -> queryProbe(toolName, probe, mappings, arguments)
        );
    }

    private Map<String, Object> inspectFlow(
            String toolName,
            String controllerClassName,
            List<ControllerMappingMetadata> mappings,
            List<ProbeDescriptor> relatedProbes,
            Map<String, Object> arguments
    ) {
        int limit = integerValue(arguments.get("limit"), 5);
        String executionId = stringValue(arguments.get("executionId"));
        Set<String> probeIds = relatedProbes.stream().map(ProbeDescriptor::probeId).collect(LinkedHashSet::new, Set::add, Set::addAll);
        List<ProbeValueRecord> recentProbeValues = recentProbeValues(mappings, limit);
        recentProbeValues.stream().map(ProbeValueRecord::probeId).forEach(probeIds::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("toolName", toolName);
        result.put("controllerClass", controllerClassName);
        result.put("routes", routeSummaries(mappings));
        result.put("probeIds", List.copyOf(probeIds));
        result.put("probes", probeSummaries(relatedProbes, recentProbeValues));

        if (executionId != null && !executionId.isBlank()) {
            ExecutionGraph graph = graphStore.findGraph(executionId)
                    .orElseThrow(() -> new IllegalArgumentException("Execution graph not found: " + executionId));
            String requestPath = graph.context().tags().getOrDefault("path", "");
            result.put("executionId", executionId);
            result.put("routeMatched", matchesAnyPath(requestPath, mappings));
            result.put("executionGraph", graph);
            return result;
        }

        result.put("recentProbeValues", recentProbeValues);
        result.put("recentExceptions", recentExceptions(mappings, limit));
        result.put("recentSlowSql", recentSlowSql(mappings, limit));
        return result;
    }

    private Map<String, Object> queryProbe(
            String toolName,
            ProbeDescriptor probe,
            List<ControllerMappingMetadata> mappings,
            Map<String, Object> arguments
    ) {
        int limit = integerValue(arguments.get("limit"), 10);
        String executionId = stringValue(arguments.get("executionId"));
        List<ProbeValueRecord> recentValues = graphStore.findProbeValues(applicationId(), probe.probeId(), sourceLimit(limit)).stream()
                .filter(record -> matchesAnyPath(record.requestPath(), mappings))
                .filter(record -> executionId == null || executionId.isBlank() || executionId.equals(record.graphId()))
                .limit(limit)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("toolName", toolName);
        result.put("probeId", probe.probeId());
        result.put("description", probe.description());
        result.put("captureSource", probe.captureSource());
        result.put("phase", probe.phase().value());
        result.put("routes", routeSummaries(mappings));
        result.put("recentValues", recentValues);
        if (!recentValues.isEmpty()) {
            result.put("latestValue", recentValues.getFirst().value());
        }
        if (executionId != null && !executionId.isBlank()) {
            result.put("executionId", executionId);
        }
        return result;
    }

    private List<ProbeDescriptor> relatedProbes(List<ControllerMappingMetadata> mappings, List<ProbeDescriptor> probes) {
        Set<String> keywords = new LinkedHashSet<>();
        for (ControllerMappingMetadata mapping : mappings) {
            String controllerSimpleName = simpleName(mapping.controllerClassName());
            addKeywordVariants(keywords, stripControllerSuffix(controllerSimpleName));
            for (String path : mapping.paths()) {
                for (String segment : path.split("/")) {
                    String cleaned = segment.replace("{", "").replace("}", "").trim();
                    if (!cleaned.isEmpty()) {
                        addKeywordVariants(keywords, cleaned);
                    }
                }
            }
        }

        return probes.stream()
                .filter(probe -> matchesProbe(probe, keywords))
                .toList();
    }

    private boolean matchesProbe(ProbeDescriptor probe, Set<String> keywords) {
        String searchable = (probe.probeId() + " " + probe.description() + " " + probe.captureSource()).toLowerCase(Locale.ROOT);
        return keywords.stream()
                .filter(keyword -> !keyword.isBlank())
                .anyMatch(searchable::contains);
    }

    private void addKeywordVariants(Set<String> keywords, String rawValue) {
        String normalized = normalizeName(rawValue).replace('_', ' ').trim().toLowerCase(Locale.ROOT);
        if (!normalized.isBlank()) {
            keywords.add(normalized);
        }
        String compact = normalized.replace(" ", "");
        if (!compact.isBlank()) {
            keywords.add(compact);
        }
        if (compact.endsWith("s") && compact.length() > 3) {
            keywords.add(compact.substring(0, compact.length() - 1));
        }
    }

    private List<Map<String, Object>> routeSummaries(List<ControllerMappingMetadata> mappings) {
        return mappings.stream()
                .map(mapping -> Map.<String, Object>of(
                        "handlerMethod", mapping.handlerMethodName(),
                        "httpMethods", mapping.httpMethods(),
                        "paths", mapping.paths()
                ))
                .toList();
    }

    private Map<String, Object> probeSummary(ProbeDescriptor probe) {
        return Map.of(
                "probeId", probe.probeId(),
                "description", probe.description(),
                "captureSource", probe.captureSource(),
                    "phase", probe.phase().value()
        );
    }

    private List<Map<String, Object>> probeSummaries(List<ProbeDescriptor> staticProbes, List<ProbeValueRecord> recentProbeValues) {
        Map<String, Map<String, Object>> summaries = new LinkedHashMap<>();
        for (ProbeDescriptor probe : staticProbes) {
            summaries.put(probe.probeId(), probeSummary(probe));
        }
        for (ProbeValueRecord record : recentProbeValues) {
            summaries.putIfAbsent(record.probeId(), Map.of(
                    "probeId", record.probeId(),
                    "description", record.description(),
                    "captureSource", record.captureSource(),
                    "phase", record.phase().value()
            ));
        }
        return List.copyOf(summaries.values());
    }

    private List<ProbeValueRecord> recentProbeValues(List<ControllerMappingMetadata> mappings, int limit) {
        return graphStore.findProbeValues(applicationId(), null, sourceLimit(limit)).stream()
                .filter(record -> matchesAnyPath(record.requestPath(), mappings))
                .limit(limit)
                .toList();
    }

    private List<ExceptionContextRecord> recentExceptions(List<ControllerMappingMetadata> mappings, int limit) {
        return graphStore.findExceptionContexts(applicationId(), sourceLimit(limit), null).stream()
                .filter(record -> matchesAnyPath(record.requestPath(), mappings))
                .limit(limit)
                .toList();
    }

    private List<SlowSqlRecord> recentSlowSql(List<ControllerMappingMetadata> mappings, int limit) {
        return graphStore.findSlowSql(applicationId(), sourceLimit(limit), 0L).stream()
                .filter(record -> matchesAnyPath(record.requestPath(), mappings))
                .limit(limit)
                .toList();
    }

    private boolean matchesAnyPath(String requestPath, List<ControllerMappingMetadata> mappings) {
        if (requestPath == null || requestPath.isBlank()) {
            return false;
        }
        for (ControllerMappingMetadata mapping : mappings) {
            for (String pattern : mapping.paths()) {
                if (matchesPathPattern(pattern, requestPath)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesPathPattern(String pattern, String requestPath) {
        if (pattern == null || pattern.isBlank()) {
            return false;
        }
        try {
            PathPattern pathPattern = pathPatternParser.parse(pattern);
            return pathPattern.matches(PathContainer.parsePath(requestPath));
        }
        catch (RuntimeException ignored) {
            return pattern.equals(requestPath);
        }
    }

    private String applicationId() {
        String applicationId = properties.getApplicationId();
        return applicationId == null || applicationId.isBlank() ? "spring-lens-app" : applicationId;
    }

    private Map<String, Object> traceToolInputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("limit", ProjectToolJsonSchemas.integerProperty());
        properties.put("executionId", ProjectToolJsonSchemas.stringProperty());
        return ProjectToolJsonSchemas.objectSchema(properties, List.of());
    }

    private Map<String, Object> traceToolOutputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("toolName", ProjectToolJsonSchemas.stringProperty());
        properties.put("controllerClass", ProjectToolJsonSchemas.stringProperty());
        properties.put("routes", ProjectToolJsonSchemas.arrayProperty(ProjectToolJsonSchemas.objectProperty(true)));
        properties.put("probeIds", ProjectToolJsonSchemas.arrayProperty(ProjectToolJsonSchemas.stringProperty()));
        properties.put("probes", ProjectToolJsonSchemas.arrayProperty(ProjectToolJsonSchemas.objectProperty(true)));
        properties.put("recentProbeValues", ProjectToolJsonSchemas.arrayProperty(ProjectToolJsonSchemas.objectProperty(true)));
        properties.put("recentExceptions", ProjectToolJsonSchemas.arrayProperty(ProjectToolJsonSchemas.objectProperty(true)));
        properties.put("recentSlowSql", ProjectToolJsonSchemas.arrayProperty(ProjectToolJsonSchemas.objectProperty(true)));
        properties.put("executionId", ProjectToolJsonSchemas.stringProperty());
        properties.put("routeMatched", ProjectToolJsonSchemas.booleanProperty());
        properties.put("executionGraph", ProjectToolJsonSchemas.objectProperty(true));
        return ProjectToolJsonSchemas.objectSchema(properties, List.of("toolName", "controllerClass"), true);
    }

    private Map<String, Object> probeToolInputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("limit", ProjectToolJsonSchemas.integerProperty());
        properties.put("executionId", ProjectToolJsonSchemas.stringProperty());
        return ProjectToolJsonSchemas.objectSchema(properties, List.of());
    }

    private Map<String, Object> probeToolOutputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("toolName", ProjectToolJsonSchemas.stringProperty());
        properties.put("probeId", ProjectToolJsonSchemas.stringProperty());
        properties.put("description", ProjectToolJsonSchemas.stringProperty());
        properties.put("captureSource", ProjectToolJsonSchemas.stringProperty());
        properties.put("phase", ProjectToolJsonSchemas.stringProperty());
        properties.put("routes", ProjectToolJsonSchemas.arrayProperty(ProjectToolJsonSchemas.objectProperty(true)));
        properties.put("recentValues", ProjectToolJsonSchemas.arrayProperty(ProjectToolJsonSchemas.objectProperty(true)));
        properties.put("latestValue", ProjectToolJsonSchemas.anyProperty());
        properties.put("executionId", ProjectToolJsonSchemas.stringProperty());
        return ProjectToolJsonSchemas.objectSchema(properties, List.of("toolName", "probeId"), true);
    }

    private int sourceLimit(int limit) {
        return Math.max(limit * 5, limit);
    }

    private int integerValue(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String simpleName(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot >= 0 ? className.substring(lastDot + 1) : className;
    }

    private String stripControllerSuffix(String simpleName) {
        return simpleName.endsWith("Controller")
                ? simpleName.substring(0, simpleName.length() - "Controller".length())
                : simpleName;
    }

    private String normalizeName(String value) {
        StringBuilder builder = new StringBuilder();
        boolean previousWasUnderscore = false;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isUpperCase(current) && builder.length() > 0 && !previousWasUnderscore) {
                builder.append('_');
                previousWasUnderscore = true;
            }
            if (Character.isLetterOrDigit(current)) {
                builder.append(Character.toLowerCase(current));
                previousWasUnderscore = false;
            }
            else if (!previousWasUnderscore && builder.length() > 0) {
                builder.append('_');
                previousWasUnderscore = true;
            }
        }
        int end = builder.length();
        while (end > 0 && builder.charAt(end - 1) == '_') {
            end--;
        }
        return builder.substring(0, end);
    }
}
