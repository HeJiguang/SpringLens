package io.springlens.model;

import java.util.Objects;

public record RuntimeCapabilityDescriptor(
        String id,
        String name,
        String description,
        String kind,
        String source
) {

    public RuntimeCapabilityDescriptor {
        id = requireText(id, "id");
        name = requireText(name, "name");
        description = requireText(description, "description");
        kind = requireText(kind, "kind");
        source = requireText(source, "source");
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
