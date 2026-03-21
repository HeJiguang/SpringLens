package io.springlens.server.tool;

import static org.assertj.core.api.Assertions.assertThat;

import io.springlens.spi.DiagnosticTool;
import io.springlens.spi.ToolDescriptor;
import io.springlens.spi.ToolExposureLevel;
import io.springlens.spi.ToolMetadata;
import io.springlens.spi.ToolRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ToolRegistryExposureTest {

    @Test
    void exposesOnlyPublicToolsInDescriptorListing() {
        ToolRegistry registry = new ToolRegistry(List.of(new InternalTool(), new PublicTool()));

        assertThat(registry.descriptors())
                .extracting(ToolDescriptor::name)
                .containsExactly("public_tool");
    }

    @Test
    void keepsInternalToolsAvailableForDirectLookup() {
        ToolRegistry registry = new ToolRegistry(List.of(new InternalTool(), new PublicTool()));

        assertThat(registry.get("internal_tool")).isInstanceOf(InternalTool.class);
        assertThat(registry.get("public_tool")).isInstanceOf(PublicTool.class);
    }

    private static final class InternalTool implements DiagnosticTool {

        @Override
        public ToolDescriptor descriptor() {
            return new ToolDescriptor("internal_tool", "Internal tool");
        }

        @Override
        public Object execute(ToolRequest request) {
            return "internal";
        }
    }

    private static final class PublicTool implements DiagnosticTool {

        @Override
        public ToolDescriptor descriptor() {
            return new ToolDescriptor("public_tool", "Public tool");
        }

        @Override
        public ToolMetadata metadata() {
            return new ToolMetadata("public_tool", "Public tool", ToolExposureLevel.PUBLIC);
        }

        @Override
        public Object execute(ToolRequest request) {
            return Map.of();
        }
    }
}
