package io.springlens.server.tool;

import io.springlens.agent.contract.RuntimeSafetyDraftBundle;
import io.springlens.model.AppRegistration;
import io.springlens.server.registry.ApplicationRegistryService;
import io.springlens.server.runtime.RuntimeObservationClient;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RuntimeSafetyDraftPlanner {

    private static final String SOURCE_TOOL_NAME = "plan_runtime_safety_remediation";

    private final ApplicationRegistryService registryService;
    private final RuntimeObservationClient runtimeObservationClient;
    private final RuntimeSafetyDraftMapper draftMapper;

    public RuntimeSafetyDraftPlanner(
            ApplicationRegistryService registryService,
            RuntimeObservationClient runtimeObservationClient,
            RuntimeSafetyDraftMapper draftMapper
    ) {
        this.registryService = registryService;
        this.runtimeObservationClient = runtimeObservationClient;
        this.draftMapper = draftMapper;
    }

    public RuntimeSafetyDraftBundle plan(String applicationId, String instanceId, Map<String, Object> arguments) {
        AppRegistration registration = registryService.resolve(applicationId, instanceId);
        return plan(registration, arguments);
    }

    public RuntimeSafetyDraftBundle plan(AppRegistration registration, Map<String, Object> arguments) {
        Object runtimeResult = runtimeObservationClient.invokeRuntimeTool(
                registration,
                SOURCE_TOOL_NAME,
                arguments == null ? Map.of() : arguments
        );
        return draftMapper.map(registration, runtimeResult);
    }
}
