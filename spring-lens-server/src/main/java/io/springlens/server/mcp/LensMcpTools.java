package io.springlens.server.mcp;

import io.springlens.agent.contract.AuditEvent;
import io.springlens.agent.contract.PolicySnapshot;
import io.springlens.agent.contract.RegisteredOverlay;
import io.springlens.agent.contract.RegisteredPatchDraft;
import io.springlens.agent.contract.RuntimeSafetyDraftBundle;
import io.springlens.agent.contract.RuntimeSafetyPromotionResult;
import io.springlens.model.AppRegistration;
import io.springlens.model.RuntimeToolDescriptor;
import io.springlens.model.core.ExecutionGraph;
import io.springlens.model.diagnostic.DiagnosticResult;
import io.springlens.model.diagnostic.ExceptionContextRecord;
import io.springlens.model.diagnostic.ProbeValueRecord;
import io.springlens.model.diagnostic.SlowSqlRecord;
import io.springlens.server.tool.ToolRouter;
import io.springlens.spi.ToolRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class LensMcpTools {

    private final ToolRouter toolRouter;

    public LensMcpTools(ToolRouter toolRouter) {
        this.toolRouter = toolRouter;
    }

    @SuppressWarnings("unchecked")
    @McpTool(name = "list_registered_apps", description = "List registered Spring Lens runtime applications.")
    public List<AppRegistration> listRegisteredApps() {
        return (List<AppRegistration>) toolRouter.invoke("list_registered_apps", new ToolRequest(null, null, Map.of()));
    }

    @SuppressWarnings("unchecked")
    @McpTool(name = "get_slow_sql", description = "Query recent slow SQL observations from a Spring Lens runtime.")
    public List<SlowSqlRecord> getSlowSql(
            @McpToolParam(description = "Application id registered in Spring Lens.") String applicationId,
            @McpToolParam(description = "Optional instance id.", required = false) String instanceId,
            @McpToolParam(description = "Maximum number of records to return.", required = false) Integer limit,
            @McpToolParam(description = "Minimum SQL duration in milliseconds.", required = false) Long minDurationMs
    ) {
        return (List<SlowSqlRecord>) toolRouter.invoke("get_slow_sql", new ToolRequest(
                applicationId,
                instanceId,
                Map.of(
                        "limit", limit == null ? 10 : limit,
                        "minDurationMs", minDurationMs == null ? 200L : minDurationMs
                )
        ));
    }

    @SuppressWarnings("unchecked")
    @McpTool(name = "get_exception_context", description = "Query recent exception contexts from a Spring Lens runtime.")
    public List<ExceptionContextRecord> getExceptionContext(
            @McpToolParam(description = "Application id registered in Spring Lens.") String applicationId,
            @McpToolParam(description = "Optional instance id.", required = false) String instanceId,
            @McpToolParam(description = "Maximum number of records to return.", required = false) Integer limit,
            @McpToolParam(description = "Optional exception class filter.", required = false) String exceptionClass
    ) {
        return (List<ExceptionContextRecord>) toolRouter.invoke("get_exception_context", new ToolRequest(
                applicationId,
                instanceId,
                exceptionClass == null
                        ? Map.of("limit", limit == null ? 10 : limit)
                        : Map.of("limit", limit == null ? 10 : limit, "exceptionClass", exceptionClass)
        ));
    }

    @McpTool(name = "get_execution_graph", description = "Fetch a structured execution graph by execution id.")
    public ExecutionGraph getExecutionGraph(
            @McpToolParam(description = "Application id registered in Spring Lens.") String applicationId,
            @McpToolParam(description = "Execution id to load.") String executionId,
            @McpToolParam(description = "Optional instance id.", required = false) String instanceId
    ) {
        return (ExecutionGraph) toolRouter.invoke("get_execution_graph", new ToolRequest(
                applicationId,
                instanceId,
                Map.of("executionId", executionId)
        ));
    }

    @McpTool(name = "diagnose_execution_graph", description = "Analyze an execution graph and return a structured diagnosis.")
    public DiagnosticResult diagnoseExecutionGraph(
            @McpToolParam(description = "Application id registered in Spring Lens.") String applicationId,
            @McpToolParam(description = "Execution id to analyze.") String executionId,
            @McpToolParam(description = "Optional instance id.", required = false) String instanceId
    ) {
        return (DiagnosticResult) toolRouter.invoke("diagnose_execution_graph", new ToolRequest(
                applicationId,
                instanceId,
                Map.of("executionId", executionId)
        ));
    }

    @McpTool(name = "get_diagnostic_playbook", description = "Fetch a diagnostic playbook by problem id.")
    public Object getDiagnosticPlaybook(
            @McpToolParam(description = "Optional playbook id.", required = false) String playbookId
    ) {
        return toolRouter.invoke("get_diagnostic_playbook", new ToolRequest(
                null,
                null,
                playbookId == null ? Map.of() : Map.of("playbookId", playbookId)
        ));
    }

    @SuppressWarnings("unchecked")
    @McpTool(name = "list_runtime_tools", description = "List capability-contributed runtime tools from a registered application.")
    public List<RuntimeToolDescriptor> listRuntimeTools(
            @McpToolParam(description = "Application id registered in Spring Lens.") String applicationId,
            @McpToolParam(description = "Optional instance id.", required = false) String instanceId
    ) {
        return (List<RuntimeToolDescriptor>) toolRouter.invoke("list_runtime_tools", new ToolRequest(
                applicationId,
                instanceId,
                Map.of()
        ));
    }

    @McpTool(name = "invoke_runtime_tool", description = "Invoke a capability-contributed runtime tool on a registered application.")
    public Object invokeRuntimeTool(
            @McpToolParam(description = "Application id registered in Spring Lens.") String applicationId,
            @McpToolParam(description = "Runtime tool name.") String toolName,
            @McpToolParam(description = "Optional instance id.", required = false) String instanceId,
            @McpToolParam(description = "Tool arguments as a JSON object.", required = false) Map<String, Object> arguments
    ) {
        return toolRouter.invoke("invoke_runtime_tool", new ToolRequest(
                applicationId,
                instanceId,
                Map.of(
                        "toolName", toolName,
                        "arguments", arguments == null ? Map.of() : arguments
                )
        ));
    }

    @McpTool(name = "draft_runtime_safety_remediation", description = "Generate overlay and patch drafts from runtime safety remediation findings.")
    public RuntimeSafetyDraftBundle draftRuntimeSafetyRemediation(
            @McpToolParam(description = "Application id registered in Spring Lens.") String applicationId,
            @McpToolParam(description = "Optional instance id.", required = false) String instanceId,
            @McpToolParam(description = "Optional remediation planning arguments.", required = false) Map<String, Object> arguments
    ) {
        return (RuntimeSafetyDraftBundle) toolRouter.invoke("draft_runtime_safety_remediation", new ToolRequest(
                applicationId,
                instanceId,
                Map.of("arguments", arguments == null ? Map.of() : arguments)
        ));
    }

    @McpTool(name = "promote_runtime_safety_remediation", description = "Submit runtime safety overlay drafts and patch drafts into the control plane.")
    public RuntimeSafetyPromotionResult promoteRuntimeSafetyRemediation(
            @McpToolParam(description = "Application id registered in Spring Lens.") String applicationId,
            @McpToolParam(description = "Optional instance id.", required = false) String instanceId,
            @McpToolParam(description = "Optional remediation planning arguments.", required = false) Map<String, Object> arguments,
            @McpToolParam(description = "Actor promoting the runtime safety drafts.", required = false) String actor
    ) {
        Map<String, Object> requestArguments = new LinkedHashMap<>();
        requestArguments.put("arguments", arguments == null ? Map.of() : arguments);
        if (actor != null && !actor.isBlank()) {
            requestArguments.put("actor", actor);
        }
        return (RuntimeSafetyPromotionResult) toolRouter.invoke("promote_runtime_safety_remediation", new ToolRequest(
                applicationId,
                instanceId,
                Map.copyOf(requestArguments)
        ));
    }

    @SuppressWarnings("unchecked")
    @McpTool(name = "query_probe_values", description = "Query captured probe values from a registered application.")
    public List<ProbeValueRecord> queryProbeValues(
            @McpToolParam(description = "Application id registered in Spring Lens.") String applicationId,
            @McpToolParam(description = "Probe id to query.") String probeId,
            @McpToolParam(description = "Optional instance id.", required = false) String instanceId,
            @McpToolParam(description = "Maximum number of records to return.", required = false) Integer limit
    ) {
        return (List<ProbeValueRecord>) toolRouter.invoke("query_probe_values", new ToolRequest(
                applicationId,
                instanceId,
                Map.of(
                        "probeId", probeId,
                        "limit", limit == null ? 10 : limit
                )
        ));
    }

    @McpTool(name = "get_policy_snapshot", description = "Fetch the current agent instrumentation policy snapshot.")
    public PolicySnapshot getPolicySnapshot() {
        return (PolicySnapshot) toolRouter.invoke("get_policy_snapshot", new ToolRequest(null, null, Map.of()));
    }

    @SuppressWarnings("unchecked")
    @McpTool(name = "list_active_overlays", description = "List active overlay registrations in the control plane.")
    public List<RegisteredOverlay> listActiveOverlays() {
        return (List<RegisteredOverlay>) toolRouter.invoke("list_active_overlays", new ToolRequest(null, null, Map.of()));
    }

    @SuppressWarnings("unchecked")
    @McpTool(name = "list_patch_drafts", description = "List patch drafts currently registered in the control plane.")
    public List<RegisteredPatchDraft> listPatchDrafts() {
        return (List<RegisteredPatchDraft>) toolRouter.invoke("list_patch_drafts", new ToolRequest(null, null, Map.of()));
    }

    @McpTool(name = "apply_overlay_instrumentation", description = "Register a new overlay specification in the control plane.")
    public RegisteredOverlay applyOverlayInstrumentation(
            @McpToolParam(description = "Overlay specification fields as a JSON object.") Map<String, Object> overlay,
            @McpToolParam(description = "Actor requesting the overlay application.", required = false) String actor
    ) {
        Map<String, Object> arguments = new LinkedHashMap<>();
        if (overlay != null) {
            arguments.putAll(overlay);
        }
        if (actor != null && !actor.isBlank()) {
            arguments.put("actor", actor);
        }
        return (RegisteredOverlay) toolRouter.invoke(
                "apply_overlay_instrumentation",
                new ToolRequest(null, null, Map.copyOf(arguments))
        );
    }

    @McpTool(name = "approve_overlay_instrumentation", description = "Approve a pending overlay registration.")
    public RegisteredOverlay approveOverlayInstrumentation(
            @McpToolParam(description = "Overlay id to approve.") String overlayId,
            @McpToolParam(description = "Actor approving the overlay.", required = false) String actor
    ) {
        return (RegisteredOverlay) toolRouter.invoke("approve_overlay_instrumentation", new ToolRequest(
                null,
                null,
                actor == null || actor.isBlank()
                        ? Map.of("overlayId", overlayId)
                        : Map.of("overlayId", overlayId, "actor", actor)
        ));
    }

    @McpTool(name = "disable_overlay_instrumentation", description = "Disable an active overlay registration.")
    public RegisteredOverlay disableOverlayInstrumentation(
            @McpToolParam(description = "Overlay id to disable.") String overlayId,
            @McpToolParam(description = "Actor requesting the disable action.", required = false) String actor
    ) {
        return (RegisteredOverlay) toolRouter.invoke("disable_overlay_instrumentation", new ToolRequest(
                null,
                null,
                actor == null || actor.isBlank()
                        ? Map.of("overlayId", overlayId)
                        : Map.of("overlayId", overlayId, "actor", actor)
        ));
    }

    @SuppressWarnings("unchecked")
    @McpTool(name = "list_audit_events", description = "List control-plane audit events emitted for agent actions.")
    public List<AuditEvent> listAuditEvents() {
        return (List<AuditEvent>) toolRouter.invoke("list_audit_events", new ToolRequest(null, null, Map.of()));
    }
}
