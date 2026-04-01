package io.springlens.starter.capability;

import static org.assertj.core.api.Assertions.assertThat;

import io.springlens.spi.LensCallableTool;
import io.springlens.spi.ToolRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import testfixtures.safety.SharedStateComponent;

class RuntimeSafetyCapabilityTest {

    @Test
    void reportsHighValueThreadAndMemorySafetyFindingsForSingletonBeans() {
        GenericApplicationContext applicationContext = new GenericApplicationContext();
        applicationContext.registerBean("sharedStateComponent", SharedStateComponent.class);
        applicationContext.refresh();

        try {
            RuntimeSafetyCapability capability = new RuntimeSafetyCapability(new RuntimeSafetyInspector(applicationContext));

            assertThat(capability.contribute().descriptor().id()).isEqualTo(RuntimeSafetyCapability.CAPABILITY_ID);
            assertThat(capability.contribute().tools()).extracting(tool -> tool.metadata().name())
                    .containsExactly("inspect_runtime_safety", "plan_runtime_safety_remediation");
            LensCallableTool tool = capability.contribute().tools().getFirst();

            RuntimeSafetyInspectionReport report = (RuntimeSafetyInspectionReport) tool.execute(
                    new ToolRequest(null, null, Map.of("maxFindings", 10))
            );

            assertThat(report.inspectedBeanCount()).isEqualTo(1);
            assertThat(report.findings()).extracting(RuntimeSafetyFinding::ruleId)
                    .contains(
                            "singleton-thread-local-field",
                            "singleton-manual-executor-field",
                            "singleton-non-thread-safe-collection",
                            "singleton-thread-unsafe-formatter",
                            "singleton-async-threadlocal-context-risk",
                            "singleton-unbounded-queue-field",
                            "singleton-non-atomic-counter-field"
                    );
            assertThat(report.findings()).extracting(RuntimeSafetyFinding::beanName)
                    .containsOnly("sharedStateComponent");

            RuntimeSafetyRemediationPlan plan = (RuntimeSafetyRemediationPlan) capability.contribute().tools().get(1)
                    .execute(new ToolRequest(null, null, Map.of("maxFindings", 10)));

            assertThat(plan.capabilityId()).isEqualTo(RuntimeSafetyCapability.CAPABILITY_ID);
            assertThat(plan.findingCount()).isGreaterThanOrEqualTo(7);
            assertThat(plan.overlaySuggestions()).extracting(RuntimeSafetyOverlaySuggestion::basedOnRuleId)
                    .contains(
                            "singleton-async-threadlocal-context-risk",
                            "singleton-unbounded-queue-field"
                    );
            assertThat(plan.patchSuggestions()).extracting(RuntimeSafetyPatchSuggestion::templateId)
                    .contains(
                            "replace-threadlocal-state",
                            "shutdown-manual-executor",
                            "replace-with-concurrent-collection",
                            "replace-simpledateformat-with-datetimeformatter",
                            "replace-counter-with-atomic"
                    );
        }
        finally {
            applicationContext.close();
        }
    }

}
