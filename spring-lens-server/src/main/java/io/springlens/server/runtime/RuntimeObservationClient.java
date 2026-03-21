package io.springlens.server.runtime;

import io.springlens.model.AppRegistration;
import io.springlens.model.diagnostic.ExceptionContextRecord;
import io.springlens.model.core.ExecutionGraph;
import io.springlens.model.ProjectToolDescriptor;
import io.springlens.model.ProjectToolSchemaDescriptor;
import io.springlens.model.diagnostic.ProbeValueRecord;
import io.springlens.model.diagnostic.SlowSqlRecord;
import java.util.List;
import java.util.Map;

public interface RuntimeObservationClient {

    List<SlowSqlRecord> getSlowSql(AppRegistration registration, int limit, long minDurationMs);

    List<ExceptionContextRecord> getExceptionContexts(AppRegistration registration, int limit, String exceptionClass);

    ExecutionGraph getExecutionGraph(AppRegistration registration, String executionId);

    List<ProjectToolDescriptor> listProjectTools(AppRegistration registration);

    List<ProjectToolSchemaDescriptor> listProjectToolSchemas(AppRegistration registration);

    Object invokeProjectTool(AppRegistration registration, String toolName, Map<String, Object> arguments);

    List<ProbeValueRecord> getProbeValues(AppRegistration registration, String probeId, int limit);
}
