package io.springlens.starter.probe;

import io.springlens.model.ProjectToolSchemaDescriptor;
import io.springlens.model.ProjectToolSourceType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ProjectToolSchemaFactory {

    private ProjectToolSchemaFactory() {
    }

    static ProjectToolSchemaDescriptor handwrittenToolSchema(Method method, String name, String description) {
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            LensToolParam annotation = parameter.getAnnotation(LensToolParam.class);
            String parameterName = annotation != null ? annotation.value() : parameter.getName();
            properties.put(parameterName, schemaForType(parameter.getType()));
            required.add(parameterName);
        }
        return new ProjectToolSchemaDescriptor(
                name,
                description,
                ProjectToolJsonSchemas.objectSchema(properties, required),
                outputSchemaForType(method.getReturnType()),
                false,
                ProjectToolSourceType.HANDWRITTEN
        );
    }

    private static Map<String, Object> outputSchemaForType(Class<?> type) {
        if (Map.class.isAssignableFrom(type) || type == Object.class) {
            return Map.of(
                    "$schema", "https://json-schema.org/draft/2020-12/schema",
                    "type", "object",
                    "additionalProperties", true
            );
        }
        if (type == void.class || type == Void.class) {
            return Map.of(
                    "$schema", "https://json-schema.org/draft/2020-12/schema",
                    "type", "null"
            );
        }
        Map<String, Object> property = schemaForType(type);
        if (!property.containsKey("type")) {
            return Map.of(
                    "$schema", "https://json-schema.org/draft/2020-12/schema",
                    "type", "object",
                    "additionalProperties", true
            );
        }
        if ("object".equals(property.get("type")) && Boolean.TRUE.equals(property.get("additionalProperties"))) {
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
            schema.putAll(property);
            return schema;
        }
        return Map.of(
                "$schema", "https://json-schema.org/draft/2020-12/schema",
                "type", "object",
                "additionalProperties", true
        );
    }

    static Map<String, Object> schemaForType(Class<?> type) {
        if (type == String.class || type == Character.class || type == char.class || Enum.class.isAssignableFrom(type)) {
            return ProjectToolJsonSchemas.stringProperty();
        }
        if (type == boolean.class || type == Boolean.class) {
            return ProjectToolJsonSchemas.booleanProperty();
        }
        if (type == byte.class || type == Byte.class
                || type == short.class || type == Short.class
                || type == int.class || type == Integer.class
                || type == long.class || type == Long.class
                || type == BigInteger.class) {
            return ProjectToolJsonSchemas.integerProperty();
        }
        if (type == float.class || type == Float.class
                || type == double.class || type == Double.class
                || type == BigDecimal.class
                || Number.class.isAssignableFrom(type)) {
            return ProjectToolJsonSchemas.numberProperty();
        }
        if (type == void.class || type == Void.class) {
            return ProjectToolJsonSchemas.nullProperty();
        }
        if (type.isArray()) {
            return ProjectToolJsonSchemas.arrayProperty(schemaForType(type.getComponentType()));
        }
        if (Collection.class.isAssignableFrom(type)) {
            return ProjectToolJsonSchemas.arrayProperty(ProjectToolJsonSchemas.anyProperty());
        }
        if (Map.class.isAssignableFrom(type) || type == Object.class || !type.getPackageName().startsWith("java.")) {
            return ProjectToolJsonSchemas.objectProperty(true);
        }
        return ProjectToolJsonSchemas.anyProperty();
    }
}
