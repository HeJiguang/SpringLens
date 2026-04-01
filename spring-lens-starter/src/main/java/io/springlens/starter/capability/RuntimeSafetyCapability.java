package io.springlens.starter.capability;

import io.springlens.spi.CapabilityContribution;
import io.springlens.spi.CapabilityDescriptor;
import io.springlens.spi.CapabilityKind;
import io.springlens.spi.CapabilitySource;
import io.springlens.spi.LensCallableTool;
import io.springlens.spi.LensCapability;
import io.springlens.spi.ToolMetadata;
import io.springlens.spi.ToolRequest;
import io.springlens.spi.ToolSchema;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RuntimeSafetyCapability implements LensCapability {

    public static final String CAPABILITY_ID = "spring-lens.runtime-safety";

    private final RuntimeSafetyInspector inspector;
    private final RuntimeSafetyRemediationPlanner remediationPlanner;

    public RuntimeSafetyCapability(RuntimeSafetyInspector inspector) {
        this(inspector, new RuntimeSafetyRemediationPlanner());
    }

    public RuntimeSafetyCapability(
            RuntimeSafetyInspector inspector,
            RuntimeSafetyRemediationPlanner remediationPlanner
    ) {
        this.inspector = inspector;
        this.remediationPlanner = remediationPlanner;
    }

    @Override
    public CapabilityContribution contribute() {
        return new CapabilityContribution(
                new CapabilityDescriptor(
                        CAPABILITY_ID,
                        "Runtime Safety Capability",
                        "Inspect singleton bean patterns that can lead to thread-safety and memory-retention bugs.",
                        CapabilityKind.DIAGNOSIS,
                        CapabilitySource.BUILT_IN
                ),
                List.of(),
                List.of(
                        new InspectRuntimeSafetyTool(inspector),
                        new PlanRuntimeSafetyRemediationTool(inspector, remediationPlanner)
                )
        );
    }

    private static final class InspectRuntimeSafetyTool implements LensCallableTool {

        private final RuntimeSafetyInspector inspector;

        private InspectRuntimeSafetyTool(RuntimeSafetyInspector inspector) {
            this.inspector = inspector;
        }

        @Override
        public ToolMetadata metadata() {
            return ToolMetadata.publiclyExposed(
                    "inspect_runtime_safety",
                    "Inspect singleton/shared-state patterns for memory-safety and thread-safety risks."
            );
        }

        @Override
        public ToolSchema schema() {
            Map<String, Object> inputProperties = new LinkedHashMap<>();
            inputProperties.put("maxFindings", Map.of("type", "integer", "minimum", 1));
            return new ToolSchema(
                    metadata().name(),
                    metadata().description(),
                    Map.of(
                            "$schema", "https://json-schema.org/draft/2020-12/schema",
                            "type", "object",
                            "properties", inputProperties,
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
            return inspector.inspect(optionalInt(request.arguments().get("maxFindings"), 20));
        }

        private int optionalInt(Object value, int fallback) {
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value instanceof String text && !text.isBlank()) {
                return Integer.parseInt(text.trim());
            }
            return fallback;
        }
    }

    private static final class PlanRuntimeSafetyRemediationTool implements LensCallableTool {

        private final RuntimeSafetyInspector inspector;
        private final RuntimeSafetyRemediationPlanner remediationPlanner;

        private PlanRuntimeSafetyRemediationTool(
                RuntimeSafetyInspector inspector,
                RuntimeSafetyRemediationPlanner remediationPlanner
        ) {
            this.inspector = inspector;
            this.remediationPlanner = remediationPlanner;
        }

        @Override
        public ToolMetadata metadata() {
            return ToolMetadata.publiclyExposed(
                    "plan_runtime_safety_remediation",
                    "Turn runtime safety findings into overlay suggestions and reviewable patch suggestions."
            );
        }

        @Override
        public ToolSchema schema() {
            Map<String, Object> inputProperties = new LinkedHashMap<>();
            inputProperties.put("maxFindings", Map.of("type", "integer", "minimum", 1));
            return new ToolSchema(
                    metadata().name(),
                    metadata().description(),
                    Map.of(
                            "$schema", "https://json-schema.org/draft/2020-12/schema",
                            "type", "object",
                            "properties", inputProperties,
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
            RuntimeSafetyInspectionReport report = inspector.inspect(optionalInt(request.arguments().get("maxFindings"), 20));
            return remediationPlanner.plan(report);
        }

        private int optionalInt(Object value, int fallback) {
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value instanceof String text && !text.isBlank()) {
                return Integer.parseInt(text.trim());
            }
            return fallback;
        }
    }
}
