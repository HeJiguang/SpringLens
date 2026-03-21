package io.springlens.spi;

import java.util.Objects;

public record CapabilityDescriptor(
        String id,
        String name,
        String description,
        CapabilityKind kind,
        CapabilitySource source
) {

    public CapabilityDescriptor {
        id = requireText(id, "id");
        name = requireText(name, "name");
        description = requireText(description, "description");
        kind = Objects.requireNonNull(kind, "kind");
        source = Objects.requireNonNull(source, "source");
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
