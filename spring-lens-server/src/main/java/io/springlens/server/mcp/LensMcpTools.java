package io.springlens.server.mcp;

import io.springlens.model.AppRegistration;
import io.springlens.model.diagnostic.ExceptionContextRecord;
import io.springlens.model.core.ExecutionGraph;
import io.springlens.model.ProjectToolDescriptor;
import io.springlens.model.diagnostic.DiagnosticResult;
import io.springlens.model.diagnostic.ProbeValueRecord;
import io.springlens.model.diagnostic.SlowSqlRecord;
import io.springlens.server.tool.ToolRouter;
import io.springlens.spi.ToolRequest;
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
    @McpTool(name = "list_project_tools", description = "List project-defined runtime tools from a registered application.")
    public List<ProjectToolDescriptor> listProjectTools(
            @McpToolParam(description = "Application id registered in Spring Lens.") String applicationId,
            @McpToolParam(description = "Optional instance id.", required = false) String instanceId
    ) {
        return (List<ProjectToolDescriptor>) toolRouter.invoke("list_project_tools", new ToolRequest(
                applicationId,
                instanceId,
                Map.of()
        ));
    }

    @McpTool(name = "invoke_project_tool", description = "Invoke a project-defined runtime tool on a registered application.")
    public Object invokeProjectTool(
            @McpToolParam(description = "Application id registered in Spring Lens.") String applicationId,
            @McpToolParam(description = "Project tool name.") String toolName,
            @McpToolParam(description = "Optional instance id.", required = false) String instanceId,
            @McpToolParam(description = "Tool arguments as a JSON object.", required = false) Map<String, Object> arguments
    ) {
        return toolRouter.invoke("invoke_project_tool", new ToolRequest(
                applicationId,
                instanceId,
                Map.of(
                        "toolName", toolName,
                        "arguments", arguments == null ? Map.of() : arguments
                )
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
}
