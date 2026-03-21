package io.springlens.server.tool;

import io.springlens.spi.DiagnosticTool;
import io.springlens.spi.ToolDescriptor;
import io.springlens.spi.ToolRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ToolRouterTest {

    @Test
    void routesToolCallsByToolName() {
        ToolRegistry registry = new ToolRegistry(List.of(new FakeTool()));
        ToolRouter router = new ToolRouter(registry);

        Object result = router.invoke("fake_tool", new ToolRequest("orders-app", null, Map.of("value", "ok")));

        assertThat(result).isEqualTo("handled:ok");
    }

    private static final class FakeTool implements DiagnosticTool {

        @Override
        public ToolDescriptor descriptor() {
            return new ToolDescriptor("fake_tool", "Fake tool for routing tests");
        }

        @Override
        public Object execute(ToolRequest request) {
            return "handled:" + request.arguments().get("value");
        }
    }
}
