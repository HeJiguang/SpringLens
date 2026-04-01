package io.springlens.server.tool;

import io.springlens.agent.contract.AgentActionRiskLevel;
import io.springlens.agent.contract.PatchDraftStatus;
import io.springlens.agent.contract.RuntimeSafetyPromotionResult;
import io.springlens.agent.contract.RuntimeSafetyDraftBundle;
import io.springlens.model.AppRegistration;
import io.springlens.model.RuntimeToolDescriptor;
import io.springlens.model.RuntimeToolSchemaDescriptor;
import io.springlens.model.diagnostic.ExceptionContextRecord;
import io.springlens.model.core.ExecutionContext;
import io.springlens.model.core.ExecutionGraph;
import io.springlens.model.diagnostic.ProbeCapturePhase;
import io.springlens.model.diagnostic.ProbeValueRecord;
import io.springlens.model.diagnostic.SlowSqlRecord;
import io.springlens.server.controlplane.audit.AuditTrailService;
import io.springlens.server.controlplane.overlay.OverlayRegistryService;
import io.springlens.server.controlplane.patch.PatchDraftRegistryService;
import io.springlens.server.registry.ApplicationRegistryService;
import io.springlens.server.runtime.RuntimeObservationClient;
import io.springlens.spi.ToolRequest;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeToolGatewayTest {

    private ApplicationRegistryService registryService;
    private RuntimeObservationClient runtimeObservationClient;

    @BeforeEach
    void setUp() {
        registryService = new ApplicationRegistryService();
        registryService.register(new AppRegistration(
                "orders-app",
                "orders-1",
                URI.create("http://localhost:8081"),
                Instant.parse("2026-03-19T10:15:30Z"),
                Map.of()
        ));
        runtimeObservationClient = new FakeRuntimeObservationClient();
    }

    @Test
    void listsRuntimeToolsThroughRuntimeClient() {
        ListRuntimeToolsTool tool = new ListRuntimeToolsTool(registryService, runtimeObservationClient);

        Object result = tool.execute(new ToolRequest("orders-app", null, Map.of()));

        assertThat(result).asList().singleElement().satisfies(value -> {
            RuntimeToolDescriptor descriptor = (RuntimeToolDescriptor) value;
            assertThat(descriptor.name()).isEqualTo("count_orders_by_status");
            assertThat(descriptor.capabilityId()).isEqualTo("demo.orders");
        });
    }

    @Test
    void invokesRuntimeToolThroughRuntimeClient() {
        InvokeRuntimeToolTool tool = new InvokeRuntimeToolTool(registryService, runtimeObservationClient);

        Object result = tool.execute(new ToolRequest(
                "orders-app",
                null,
                Map.of("toolName", "count_orders_by_status", "arguments", Map.of("status", "PAID"))
        ));

        assertThat(result).isEqualTo(Map.of("status", "PAID", "count", 1, "capabilityId", "demo.orders"));
    }

    @Test
    void draftsRuntimeSafetyRemediationIntoControlPlaneContracts() {
        RuntimeSafetyDraftPlanner planner = new RuntimeSafetyDraftPlanner(
                registryService,
                runtimeObservationClient,
                new RuntimeSafetyDraftMapper()
        );
        DraftRuntimeSafetyRemediationTool tool = new DraftRuntimeSafetyRemediationTool(planner);

        RuntimeSafetyDraftBundle result = (RuntimeSafetyDraftBundle) tool.execute(new ToolRequest(
                "orders-app",
                null,
                Map.of("arguments", Map.of("maxFindings", 3))
        ));

        assertThat(result.applicationId()).isEqualTo("orders-app");
        assertThat(result.instanceId()).isEqualTo("orders-1");
        assertThat(result.capabilityId()).isEqualTo("spring-lens.runtime-safety");
        assertThat(result.overlayDrafts()).singleElement().satisfies(value -> {
            assertThat(value.overlayId()).isEqualTo("draft-runtime-queue-depth");
            assertThat(value.selectorType()).isEqualTo("spring-bean-method");
            assertThat(value.targetClassName()).isEqualTo("com.example.demo.DemoSafetyRiskService");
            assertThat(value.targetMethodName()).isEqualTo("enqueueUnsafeWork");
            assertThat(value.riskLevel()).isEqualTo(AgentActionRiskLevel.MEDIUM);
            assertThat(value.metadata()).containsEntry("sourceSuggestionId", "runtime-safety-overlay-1");
        });
        assertThat(result.patchDrafts()).singleElement().satisfies(value -> {
            assertThat(value.draftId()).isEqualTo("runtime-safety-patch-1");
            assertThat(value.templateId()).isEqualTo("replace-counter-with-atomic");
            assertThat(value.riskLevel()).isEqualTo(AgentActionRiskLevel.HIGH);
            assertThat(value.targetFiles()).containsExactly("src/main/java/com/example/demo/DemoSafetyRiskService.java");
        });
    }

    @Test
    void promotesRuntimeSafetyRemediationIntoControlPlaneRegistrations() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-31T18:30:00Z"), ZoneOffset.UTC);
        AuditTrailService auditTrailService = new AuditTrailService(clock);
        OverlayRegistryService overlayRegistryService = new OverlayRegistryService(clock, auditTrailService);
        PatchDraftRegistryService patchDraftRegistryService = new PatchDraftRegistryService(clock, auditTrailService);
        RuntimeSafetyDraftPlanner planner = new RuntimeSafetyDraftPlanner(
                registryService,
                runtimeObservationClient,
                new RuntimeSafetyDraftMapper()
        );
        PromoteRuntimeSafetyRemediationTool tool = new PromoteRuntimeSafetyRemediationTool(
                planner,
                overlayRegistryService,
                patchDraftRegistryService
        );

        RuntimeSafetyPromotionResult result = (RuntimeSafetyPromotionResult) tool.execute(new ToolRequest(
                "orders-app",
                null,
                Map.of(
                        "arguments", Map.of("maxFindings", 3),
                        "actor", "codex"
                )
        ));

        assertThat(result.applicationId()).isEqualTo("orders-app");
        assertThat(result.overlayRegistrations()).singleElement().satisfies(value -> {
            assertThat(value.overlayId()).isEqualTo("draft-runtime-queue-depth");
            assertThat(value.approvalState().value()).isEqualTo("PENDING");
        });
        assertThat(result.patchDraftRegistrations()).singleElement().satisfies(value -> {
            assertThat(value.draftId()).isEqualTo("runtime-safety-patch-1");
            assertThat(value.status()).isEqualTo(PatchDraftStatus.PENDING);
            assertThat(value.metadata()).containsEntry("actor", "codex");
        });
        assertThat(patchDraftRegistryService.list()).extracting(value -> value.draftId())
                .containsExactly("runtime-safety-patch-1");
        assertThat(overlayRegistryService.listActive()).extracting(value -> value.overlayId())
                .containsExactly("draft-runtime-queue-depth");
        assertThat(auditTrailService.list()).extracting(value -> value.eventType().value())
                .containsExactly("OVERLAY_APPLIED", "PATCH_DRAFT_SUBMITTED");
    }

    private static final class FakeRuntimeObservationClient implements RuntimeObservationClient {

        @Override
        public List<SlowSqlRecord> getSlowSql(AppRegistration registration, int limit, long minDurationMs) {
            return List.of();
        }

        @Override
        public List<ExceptionContextRecord> getExceptionContexts(AppRegistration registration, int limit, String exceptionClass) {
            return List.of();
        }

        @Override
        public ExecutionGraph getExecutionGraph(AppRegistration registration, String executionId) {
            return new ExecutionGraph(new ExecutionContext("orders-app", "orders-1", executionId, Instant.now(), Map.of()), List.of(), List.of());
        }

        @Override
        public List<RuntimeToolDescriptor> listRuntimeTools(AppRegistration registration) {
            return List.of(new RuntimeToolDescriptor("count_orders_by_status", "Count by status", "demo.orders"));
        }

        @Override
        public List<RuntimeToolSchemaDescriptor> listRuntimeToolSchemas(AppRegistration registration) {
            return List.of(new RuntimeToolSchemaDescriptor(
                    "count_orders_by_status",
                    "Count by status",
                    Map.of("type", "object"),
                    Map.of("type", "object"),
                    "demo.orders"
            ));
        }

        @Override
        public Object invokeRuntimeTool(AppRegistration registration, String toolName, Map<String, Object> arguments) {
            if ("plan_runtime_safety_remediation".equals(toolName)) {
                return Map.of(
                        "capabilityId", "spring-lens.runtime-safety",
                        "findingCount", 2,
                        "overlaySuggestionCount", 1,
                        "patchSuggestionCount", 1,
                        "overlaySuggestions", List.of(Map.of(
                                "suggestionId", "runtime-safety-overlay-1",
                                "basedOnRuleId", "singleton-unbounded-queue-field",
                                "selectorType", "spring-bean-method",
                                "targetClassName", "com.example.demo.DemoSafetyRiskService",
                                "targetMethodName", "enqueueUnsafeWork",
                                "probeId", "runtime.queue.depth",
                                "title", "Track queue depth",
                                "rationale", "Observe queue depth while the risky queue is in use.",
                                "parameters", Map.of(
                                        "overlayId", "draft-runtime-queue-depth",
                                        "ttl", "PT2H",
                                        "capturePhase", "AFTER_RETURN",
                                        "expression", "result",
                                        "description", "Observe queue depth while validating runtime safety findings"
                                )
                        )),
                        "patchSuggestions", List.of(Map.of(
                                "suggestionId", "runtime-safety-patch-1",
                                "basedOnRuleId", "singleton-non-atomic-counter-field",
                                "templateId", "replace-counter-with-atomic",
                                "targetClassName", "com.example.demo.DemoSafetyRiskService",
                                "targetFieldName", "sharedCounter",
                                "title", "Replace the shared counter with AtomicInteger",
                                "reason", "The shared counter is mutated across threads without atomic protection.",
                                "requiresApproval", true,
                                "parameters", Map.of(
                                        "targetFile", "src/main/java/com/example/demo/DemoSafetyRiskService.java"
                                )
                        ))
                );
            }
            return Map.of("status", arguments.get("status"), "count", 1, "capabilityId", "demo.orders");
        }

        @Override
        public List<ProbeValueRecord> getProbeValues(AppRegistration registration, String probeId, int limit) {
            return List.of(new ProbeValueRecord(
                    "graph-1",
                    "node-1",
                    probeId,
                    "Local status",
                    "PAID",
                    String.class.getName(),
                    "/orders/probe/1",
                    Instant.parse("2026-03-19T10:15:30Z"),
                    "manual",
                    ProbeCapturePhase.MANUAL
            ));
        }
    }
}
