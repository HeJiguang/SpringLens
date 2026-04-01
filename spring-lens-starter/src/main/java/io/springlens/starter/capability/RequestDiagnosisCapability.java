package io.springlens.starter.capability;

import io.springlens.model.diagnostic.DiagnosticResult;
import io.springlens.runtime.InMemoryExecutionGraphStore;
import io.springlens.spi.CapabilityContribution;
import io.springlens.spi.CapabilityDescriptor;
import io.springlens.spi.CapabilityKind;
import io.springlens.spi.CapabilitySource;
import io.springlens.spi.DiagnosticEngine;
import io.springlens.spi.LensCallableTool;
import io.springlens.spi.LensCapability;
import io.springlens.spi.ToolMetadata;
import io.springlens.spi.ToolRequest;
import io.springlens.spi.ToolSchema;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RequestDiagnosisCapability implements LensCapability {

    public static final String CAPABILITY_ID = "spring-lens.diagnosis";

    private final DiagnoseRequestTool diagnoseRequestTool;

    public RequestDiagnosisCapability(
            InMemoryExecutionGraphStore graphStore,
            DiagnosticEngine diagnosticEngine
    ) {
        this.diagnoseRequestTool = new DiagnoseRequestTool(graphStore, diagnosticEngine);
    }

    @Override
    public CapabilityContribution contribute() {
        return new CapabilityContribution(
                new CapabilityDescriptor(
                        CAPABILITY_ID,
                        "Request Diagnosis Capability",
                        "Analyze captured execution graphs and return structured diagnoses.",
                        CapabilityKind.DIAGNOSIS,
                        CapabilitySource.BUILT_IN
                ),
                List.of(),
                List.of(diagnoseRequestTool)
        );
    }

    private static final class DiagnoseRequestTool implements LensCallableTool {

        private final InMemoryExecutionGraphStore graphStore;
        private final DiagnosticEngine diagnosticEngine;

        private DiagnoseRequestTool(InMemoryExecutionGraphStore graphStore, DiagnosticEngine diagnosticEngine) {
            this.graphStore = graphStore;
            this.diagnosticEngine = diagnosticEngine;
        }

        @Override
        public ToolMetadata metadata() {
            return ToolMetadata.publiclyExposed(
                    "diagnose_request",
                    "Analyze a captured request by execution id and return a structured diagnosis."
            );
        }

        @Override
        public ToolSchema schema() {
            Map<String, Object> inputProperties = new LinkedHashMap<>();
            inputProperties.put("executionId", Map.of("type", "string"));
            return new ToolSchema(
                    metadata().name(),
                    metadata().description(),
                    Map.of(
                            "$schema", "https://json-schema.org/draft/2020-12/schema",
                            "type", "object",
                            "properties", inputProperties,
                            "required", List.of("executionId"),
                            "additionalProperties", false
                    ),
                    Map.of(
                            "$schema", "https://json-schema.org/draft/2020-12/schema",
                            "type", "object",
                            "additionalProperties", true
                    )
            );
        }

        @Override
        public Object execute(ToolRequest request) {
            String executionId = String.valueOf(request.arguments().get("executionId"));
            if (executionId == null || executionId.isBlank() || "null".equals(executionId)) {
                throw new IllegalArgumentException("executionId is required");
            }
            return diagnosticEngine.analyze(graphStore.findGraph(executionId)
                    .orElseThrow(() -> new IllegalArgumentException("Execution graph not found: " + executionId)));
        }
    }
}
