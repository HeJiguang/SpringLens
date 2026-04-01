package io.springlens.server.tool;

import io.springlens.agent.contract.PatchProposalDraft;
import io.springlens.agent.contract.RegisteredOverlay;
import io.springlens.agent.contract.RegisteredPatchDraft;
import io.springlens.agent.contract.RuntimeSafetyDraftBundle;
import io.springlens.agent.contract.RuntimeSafetyPromotionResult;
import io.springlens.server.controlplane.overlay.OverlayRegistryService;
import io.springlens.server.controlplane.patch.PatchDraftRegistryService;
import io.springlens.spi.DiagnosticTool;
import io.springlens.spi.ToolDescriptor;
import io.springlens.spi.ToolMetadata;
import io.springlens.spi.ToolRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PromoteRuntimeSafetyRemediationTool implements DiagnosticTool {

    private final RuntimeSafetyDraftPlanner runtimeSafetyDraftPlanner;
    private final OverlayRegistryService overlayRegistryService;
    private final PatchDraftRegistryService patchDraftRegistryService;

    public PromoteRuntimeSafetyRemediationTool(
            RuntimeSafetyDraftPlanner runtimeSafetyDraftPlanner,
            OverlayRegistryService overlayRegistryService,
            PatchDraftRegistryService patchDraftRegistryService
    ) {
        this.runtimeSafetyDraftPlanner = runtimeSafetyDraftPlanner;
        this.overlayRegistryService = overlayRegistryService;
        this.patchDraftRegistryService = patchDraftRegistryService;
    }

    @Override
    public ToolDescriptor descriptor() {
        return new ToolDescriptor(
                "promote_runtime_safety_remediation",
                "Submit runtime safety overlay drafts and patch drafts into the control plane."
        );
    }

    @Override
    public ToolMetadata metadata() {
        ToolDescriptor descriptor = descriptor();
        return ToolMetadata.publiclyExposed(descriptor.name(), descriptor.description());
    }

    @Override
    public Map<String, Object> inputSchema() {
        return ToolJsonSchemas.objectSchema(
                Map.of(
                        "applicationId", ToolJsonSchemas.stringProperty("Application id registered in Spring Lens."),
                        "instanceId", ToolJsonSchemas.stringProperty("Optional instance id."),
                        "actor", ToolJsonSchemas.stringProperty("Actor promoting the runtime safety drafts."),
                        "arguments", ToolJsonSchemas.objectProperty("Optional runtime safety remediation arguments.")
                ),
                List.of("applicationId")
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object execute(ToolRequest request) {
        Object arguments = request.arguments().get("arguments");
        Map<String, Object> runtimeArguments = arguments instanceof Map<?, ?> values
                ? (Map<String, Object>) values
                : Map.of();
        String actor = optionalString(request.arguments(), "actor", "system");
        RuntimeSafetyDraftBundle bundle = runtimeSafetyDraftPlanner.plan(request.applicationId(), request.instanceId(), runtimeArguments);
        List<RegisteredOverlay> overlayRegistrations = bundle.overlayDrafts().stream()
                .map(spec -> overlayRegistryService.apply(spec, actor))
                .toList();
        List<RegisteredPatchDraft> patchDraftRegistrations = new ArrayList<>();
        for (PatchProposalDraft draft : bundle.patchDrafts()) {
            patchDraftRegistrations.add(patchDraftRegistryService.submit(enrichDraft(draft, bundle), actor));
        }
        Map<String, String> metadata = new LinkedHashMap<>(bundle.metadata());
        metadata.put("actor", actor);
        return new RuntimeSafetyPromotionResult(
                bundle.applicationId(),
                bundle.instanceId(),
                bundle.capabilityId(),
                overlayRegistrations,
                List.copyOf(patchDraftRegistrations),
                Map.copyOf(metadata)
        );
    }

    private PatchProposalDraft enrichDraft(PatchProposalDraft draft, RuntimeSafetyDraftBundle bundle) {
        Map<String, String> metadata = new LinkedHashMap<>(draft.metadata());
        metadata.putIfAbsent("applicationId", bundle.applicationId());
        if (bundle.instanceId() != null && !bundle.instanceId().isBlank()) {
            metadata.putIfAbsent("instanceId", bundle.instanceId());
        }
        metadata.putIfAbsent("capabilityId", bundle.capabilityId());
        metadata.putIfAbsent("sourceTool", bundle.metadata().getOrDefault("sourceTool", "plan_runtime_safety_remediation"));
        return new PatchProposalDraft(
                draft.draftId(),
                draft.riskLevel(),
                draft.templateId(),
                draft.title(),
                draft.reason(),
                draft.targetFiles(),
                draft.requiresApproval(),
                draft.originSkillId(),
                Map.copyOf(metadata)
        );
    }

    private static String optionalString(Map<String, Object> arguments, String key, String fallback) {
        Object value = arguments.get(key);
        return value instanceof String text && !text.isBlank() ? text : fallback;
    }
}
