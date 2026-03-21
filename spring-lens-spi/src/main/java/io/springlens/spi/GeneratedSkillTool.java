package io.springlens.spi;

import io.springlens.model.ProjectToolDescriptor;
import io.springlens.model.ProjectToolSchemaDescriptor;
import io.springlens.model.ProjectToolSourceType;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record GeneratedSkillTool(
        String name,
        String description,
        ProjectToolSourceType sourceType,
        Map<String, Object> inputSchema,
        Map<String, Object> outputSchema,
        Handler handler
) {

    public GeneratedSkillTool(String name, String description, Handler handler) {
        this(
                name,
                description,
                ProjectToolSourceType.GENERATED_TRACE,
                Map.of(
                        "$schema", "https://json-schema.org/draft/2020-12/schema",
                        "type", "object",
                        "additionalProperties", false
                ),
                Map.of(
                        "$schema", "https://json-schema.org/draft/2020-12/schema",
                        "type", "object",
                        "additionalProperties", true
                ),
                handler
        );
    }

    public GeneratedSkillTool {
        name = Objects.requireNonNull(name, "name");
        description = Objects.requireNonNull(description, "description");
        sourceType = Objects.requireNonNull(sourceType, "sourceType");
        inputSchema = immutableJsonObject(inputSchema);
        outputSchema = immutableJsonObject(outputSchema);
        handler = Objects.requireNonNull(handler, "handler");
    }

    public ProjectToolDescriptor descriptor() {
        return new ProjectToolDescriptor(name, description);
    }

    public ProjectToolSchemaDescriptor schemaDescriptor() {
        return new ProjectToolSchemaDescriptor(name, description, inputSchema, outputSchema, true, sourceType);
    }

    public Object invoke(Map<String, Object> arguments) {
        return handler.invoke(arguments == null ? Map.of() : Map.copyOf(arguments));
    }

    private static Map<String, Object> immutableJsonObject(Map<String, Object> schema) {
        if (schema == null || schema.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        schema.forEach((key, value) -> copy.put(String.valueOf(key), immutableJsonValue(value)));
        return Collections.unmodifiableMap(copy);
    }

    private static Object immutableJsonValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, nestedValue) -> copy.put(String.valueOf(key), immutableJsonValue(nestedValue)));
            return Collections.unmodifiableMap(copy);
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(GeneratedSkillTool::immutableJsonValue)
                    .toList();
        }
        return value;
    }

    @FunctionalInterface
    public interface Handler {

        Object invoke(Map<String, Object> arguments);
    }
}
