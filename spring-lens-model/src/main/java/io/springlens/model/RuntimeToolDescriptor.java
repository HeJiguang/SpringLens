package io.springlens.model;

import java.util.Objects;

public record RuntimeToolDescriptor(
        String name,
        String description,
        String capabilityId
) {

    public RuntimeToolDescriptor {
        name = requireText(name, "name");
        description = requireText(description, "description");
        capabilityId = requireText(capabilityId, "capabilityId");
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
