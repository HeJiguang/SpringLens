package io.springlens.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ProjectToolSchemaDescriptor(
        String name,
        String description,
        Map<String, Object> inputSchema,
        Map<String, Object> outputSchema,
        boolean generated,
        ProjectToolSourceType sourceType
) {

    public ProjectToolSchemaDescriptor {
        name = Objects.requireNonNull(name, "name");
        description = Objects.requireNonNull(description, "description");
        inputSchema = immutableJsonObject(inputSchema);
        outputSchema = immutableJsonObject(outputSchema);
        sourceType = Objects.requireNonNull(sourceType, "sourceType");
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
                    .map(ProjectToolSchemaDescriptor::immutableJsonValue)
                    .toList();
        }
        return value;
    }
}
