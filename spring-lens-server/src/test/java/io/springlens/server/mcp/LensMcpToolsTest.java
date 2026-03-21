package io.springlens.server.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import io.springlens.model.diagnostic.DiagnosticResult;
import io.springlens.spi.DiagnosticTool;
import io.springlens.spi.ToolDescriptor;
import io.springlens.spi.ToolRequest;
import io.springlens.server.tool.ToolRegistry;
import io.springlens.server.tool.ToolRouter;
import java.util.List;
import org.junit.jupiter.api.Test;

class LensMcpToolsTest {

    @Test
    void routesDiagnoseExecutionGraphToToolRouter() {
        CaptureRequestTool captureRequestTool = new CaptureRequestTool();
        LensMcpTools lensMcpTools = new LensMcpTools(new ToolRouter(new ToolRegistry(List.of(captureRequestTool))));

        DiagnosticResult result = lensMcpTools.diagnoseExecutionGraph("orders-app", "graph-9", null);

        assertThat(result.rootCause()).isEqualTo("Captured");
        assertThat(captureRequestTool.lastRequest.applicationId()).isEqualTo("orders-app");
        assertThat(captureRequestTool.lastRequest.instanceId()).isNull();
        assertThat(captureRequestTool.lastRequest.arguments()).containsEntry("executionId", "graph-9");
    }

    private static final class CaptureRequestTool implements DiagnosticTool {

        private ToolRequest lastRequest;

        @Override
        public ToolDescriptor descriptor() {
            return new ToolDescriptor("diagnose_execution_graph", "Capture tool request for tests.");
        }

        @Override
        public Object execute(ToolRequest request) {
            lastRequest = request;
            return DiagnosticResult.builder()
                    .rootCause("Captured")
                    .summary("Captured request")
                    .confidence(0.9)
                    .build();
        }
    }
}
