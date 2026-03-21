package io.springlens.spi;

public interface DiagnosticTool extends LensCallableTool {

    ToolDescriptor descriptor();

    @Override
    default ToolMetadata metadata() {
        ToolDescriptor descriptor = descriptor();
        return ToolMetadata.internal(descriptor.name(), descriptor.description());
    }
}
