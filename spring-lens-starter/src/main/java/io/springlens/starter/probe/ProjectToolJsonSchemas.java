package io.springlens.starter.probe;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ProjectToolJsonSchemas {

    private static final String JSON_SCHEMA_DRAFT = "https://json-schema.org/draft/2020-12/schema";

    private ProjectToolJsonSchemas() {
    }

    static Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
        return objectSchema(properties, required, false);
    }

    static Map<String, Object> objectSchema(
            Map<String, Object> properties,
            List<String> required,
            boolean additionalProperties
    ) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("$schema", JSON_SCHEMA_DRAFT);
        schema.put("type", "object");
        schema.put("properties", properties == null ? Map.of() : properties);
        schema.put("additionalProperties", additionalProperties);
        if (required != null && !required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    static Map<String, Object> stringProperty() {
        return Map.of("type", "string");
    }

    static Map<String, Object> integerProperty() {
        return Map.of("type", "integer");
    }

    static Map<String, Object> numberProperty() {
        return Map.of("type", "number");
    }

    static Map<String, Object> booleanProperty() {
        return Map.of("type", "boolean");
    }

    static Map<String, Object> nullProperty() {
        return Map.of("type", "null");
    }

    static Map<String, Object> arrayProperty(Map<String, Object> items) {
        return Map.of(
                "type", "array",
                "items", items == null ? Map.of() : items
        );
    }

    static Map<String, Object> objectProperty(boolean additionalProperties) {
        return Map.of(
                "type", "object",
                "additionalProperties", additionalProperties
        );
    }

    static Map<String, Object> anyProperty() {
        return Map.of();
    }
}
