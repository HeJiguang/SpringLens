package io.springlens.spi;

public record ToolMetadata(
        String name,
        String description,
        ToolExposureLevel exposureLevel
) {

    public static ToolMetadata internal(String name, String description) {
        return new ToolMetadata(name, description, ToolExposureLevel.INTERNAL);
    }

    public static ToolMetadata publiclyExposed(String name, String description) {
        return new ToolMetadata(name, description, ToolExposureLevel.PUBLIC);
    }
}
