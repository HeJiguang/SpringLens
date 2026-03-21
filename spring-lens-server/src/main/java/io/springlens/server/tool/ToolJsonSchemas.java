package io.springlens.server.tool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ToolJsonSchemas {

    private static final String JSON_SCHEMA_DRAFT = "https://json-schema.org/draft/2020-12/schema";

    private ToolJsonSchemas() {
    }

    static Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("$schema", JSON_SCHEMA_DRAFT);
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("additionalProperties", false);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    static Map<String, Object> stringProperty(String description) {
        return scalarProperty("string", description);
    }

    static Map<String, Object> integerProperty(String description) {
        return scalarProperty("integer", description);
    }

    static Map<String, Object> objectProperty(String description) {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("type", "object");
        property.put("description", description);
        property.put("additionalProperties", true);
        return property;
    }

    private static Map<String, Object> scalarProperty(String type, String description) {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("type", type);
        property.put("description", description);
        return property;
    }
}
