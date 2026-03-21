package io.springlens.starter;

import io.springlens.model.diagnostic.ExceptionContextRecord;
import io.springlens.model.core.ExecutionGraph;
import io.springlens.model.ProjectToolDescriptor;
import io.springlens.model.ProjectToolSchemaDescriptor;
import io.springlens.model.ProbeDescriptor;
import io.springlens.model.RuntimeCapabilityDescriptor;
import io.springlens.model.RuntimeToolDescriptor;
import io.springlens.model.RuntimeToolSchemaDescriptor;
import io.springlens.model.diagnostic.ProbeValueRecord;
import io.springlens.model.diagnostic.SlowSqlRecord;
import io.springlens.runtime.InMemoryExecutionGraphStore;
import io.springlens.starter.capability.LensCapabilityRegistry;
import io.springlens.starter.probe.LensProbeRegistry;
import io.springlens.starter.probe.LensProjectToolRegistry;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * 运行时查询 Controller (Runtime Query Controller)。
 * 本项目是“拉取(Pull)”模型架构，这个 Controller 被嵌入在你的微服务里。
 * 它对外（主要是给 Server）暴露一系列 REST API，允许 Server 定期或者按需上门来拉取内存环形缓冲里的数据。
 */
@RestController
@RequestMapping("/internal/spring-lens")
public class RuntimeQueryController {

    private final InMemoryExecutionGraphStore graphStore;
    private final LensRuntimeProperties properties;
    private final LensProbeRegistry probeRegistry;
    private final LensProjectToolRegistry projectToolRegistry;
    private final LensCapabilityRegistry capabilityRegistry;

    public RuntimeQueryController(
            InMemoryExecutionGraphStore graphStore,
            LensRuntimeProperties properties,
            LensProbeRegistry probeRegistry,
            LensProjectToolRegistry projectToolRegistry,
            LensCapabilityRegistry capabilityRegistry
    ) {
        this.graphStore = graphStore;
        this.properties = properties;
        this.probeRegistry = probeRegistry;
        this.projectToolRegistry = projectToolRegistry;
        this.capabilityRegistry = capabilityRegistry;
    }

    /**
     * @return 提取内存中最近的慢 SQL。
     */
    @GetMapping("/slow-sql")
    public List<SlowSqlRecord> getSlowSql(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "200") long minDurationMs
    ) {
        return graphStore.findSlowSql(applicationId(), limit, minDurationMs);
    }

    /**
     * @return 提取内存中最近捕获的异常抛出记录。
     */
    @GetMapping("/exception-context")
    public List<ExceptionContextRecord> getExceptionContext(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String exceptionClass
    ) {
        return graphStore.findExceptionContexts(applicationId(), limit, exceptionClass);
    }

    /**
     * @return 提取某一次单一请求请求链路产生的完整执行图。
     */
    @GetMapping("/graphs/{executionId}")
    public ExecutionGraph getExecutionGraph(@PathVariable String executionId) {
        return graphStore.findGraph(executionId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Execution graph not found: " + executionId));
    }

    /**
     * @return 返回应用代码中总共埋设过多少个探针的“静态档案名册”。
     */
    @GetMapping("/probes")
    public List<ProbeDescriptor> getProbes() {
        return probeRegistry.list();
    }

    /**
     * @return 提取内存在某一个探针上抓取到的所有真实值（活体变量）。
     */
    @GetMapping("/capabilities")
    public List<RuntimeCapabilityDescriptor> getCapabilities() {
        return capabilityRegistry.capabilities();
    }

    @GetMapping("/tools")
    public List<RuntimeToolDescriptor> getTools() {
        return capabilityRegistry.tools();
    }

    @GetMapping("/tools/schema")
    public List<RuntimeToolSchemaDescriptor> getToolSchemas() {
        return capabilityRegistry.toolSchemas();
    }

    @PostMapping("/tools/{toolName}:invoke")
    public Object invokeTool(
            @PathVariable String toolName,
            @RequestBody(required = false) ToolInvocationRequest request
    ) {
        return capabilityRegistry.invoke(toolName, request == null ? Map.of() : request.arguments());
    }

    @GetMapping("/probe-values")
    public List<ProbeValueRecord> getProbeValues(
            @RequestParam(required = false) String probeId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return graphStore.findProbeValues(applicationId(), probeId, limit);
    }

    /**
     * @return 获取当前实例提供的可以供 AI 调用的额外诊断工具函数清单。
     */
    @GetMapping("/project-tools")
    public List<ProjectToolDescriptor> getProjectTools() {
        return projectToolRegistry.list();
    }

    @GetMapping("/project-tools/schema")
    public List<ProjectToolSchemaDescriptor> getProjectToolSchemas() {
        return projectToolRegistry.listSchemas();
    }

    /**
     * 【执行工具接口】允许外部 AI Agent 向微服务发起指定函数调用的后门，并将函数的返回值直接通过该 HTTP 请求响应回去。
     */
    @PostMapping("/project-tools/{toolName}:invoke")
    public Object invokeProjectTool(
            @PathVariable String toolName,
            @RequestBody(required = false) ProjectToolInvocationRequest request
    ) {
        return projectToolRegistry.invoke(toolName, request == null ? Map.of() : request.arguments());
    }

    private String applicationId() {
        return properties.getApplicationId();
    }
}
