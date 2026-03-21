package io.springlens.server.runtime;

import io.springlens.model.AppRegistration;
import io.springlens.model.core.ExecutionGraph;
import io.springlens.model.ProjectToolDescriptor;
import io.springlens.model.ProjectToolSchemaDescriptor;
import io.springlens.model.RuntimeToolDescriptor;
import io.springlens.model.RuntimeToolSchemaDescriptor;
import io.springlens.model.diagnostic.ExceptionContextRecord;
import io.springlens.model.diagnostic.ProbeValueRecord;
import io.springlens.model.diagnostic.SlowSqlRecord;
import java.util.List;
import java.util.Map;

public interface RuntimeObservationClient {

    List<SlowSqlRecord> getSlowSql(AppRegistration registration, int limit, long minDurationMs);

    List<ExceptionContextRecord> getExceptionContexts(AppRegistration registration, int limit, String exceptionClass);

    ExecutionGraph getExecutionGraph(AppRegistration registration, String executionId);

    default List<ProjectToolDescriptor> listProjectTools(AppRegistration registration) {
        throw new UnsupportedOperationException("Legacy project tool listing is not implemented");
    }

    default List<ProjectToolSchemaDescriptor> listProjectToolSchemas(AppRegistration registration) {
        throw new UnsupportedOperationException("Legacy project tool schema listing is not implemented");
    }

    default Object invokeProjectTool(AppRegistration registration, String toolName, Map<String, Object> arguments) {
        return invokeRuntimeTool(registration, toolName, arguments);
    }

    default List<RuntimeToolDescriptor> listRuntimeTools(AppRegistration registration) {
        return listProjectTools(registration).stream()
                .map(tool -> new RuntimeToolDescriptor(tool.name(), tool.description(), "legacy.project-tools"))
                .toList();
    }

    default List<RuntimeToolSchemaDescriptor> listRuntimeToolSchemas(AppRegistration registration) {
        return listProjectToolSchemas(registration).stream()
                .map(tool -> new RuntimeToolSchemaDescriptor(
                        tool.name(),
                        tool.description(),
                        tool.inputSchema(),
                        tool.outputSchema(),
                        "legacy.project-tools"
                ))
                .toList();
    }

    default Object invokeRuntimeTool(AppRegistration registration, String toolName, Map<String, Object> arguments) {
        return invokeProjectTool(registration, toolName, arguments);
    }

    List<ProbeValueRecord> getProbeValues(AppRegistration registration, String probeId, int limit);
}
