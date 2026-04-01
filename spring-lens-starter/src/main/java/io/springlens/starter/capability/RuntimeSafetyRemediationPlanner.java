package io.springlens.starter.capability;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RuntimeSafetyRemediationPlanner {

    public RuntimeSafetyRemediationPlan plan(RuntimeSafetyInspectionReport report) {
        List<RuntimeSafetyOverlaySuggestion> overlaySuggestions = new ArrayList<>();
        List<RuntimeSafetyPatchSuggestion> patchSuggestions = new ArrayList<>();

        for (RuntimeSafetyFinding finding : report.findings()) {
            switch (finding.ruleId()) {
                case "singleton-thread-local-field" -> patchSuggestions.add(patchSuggestion(
                        finding,
                        "replace-threadlocal-state",
                        "Replace singleton ThreadLocal state",
                        "Singleton ThreadLocal state is brittle under pooled threads and should be removed or scoped explicitly.",
                        Map.of("replacement", "request-scope-or-method-scope")
                ));
                case "singleton-manual-executor-field" -> patchSuggestions.add(patchSuggestion(
                        finding,
                        "shutdown-manual-executor",
                        "Add executor shutdown lifecycle",
                        "Manual executors on singleton beans should be closed explicitly to avoid leaking threads and memory.",
                        Map.of("lifecycleHook", "@PreDestroy")
                ));
                case "singleton-non-thread-safe-collection" -> patchSuggestions.add(patchSuggestion(
                        finding,
                        "replace-with-concurrent-collection",
                        "Replace shared collection with concurrent variant",
                        "Shared mutable collections on singleton beans need concurrent implementations or explicit coordination.",
                        Map.of("replacement", "ConcurrentHashMap-or-confined-state")
                ));
                case "singleton-thread-unsafe-formatter" -> patchSuggestions.add(patchSuggestion(
                        finding,
                        "replace-simpledateformat-with-datetimeformatter",
                        "Replace legacy formatter",
                        "Thread-unsafe formatters on singleton beans should be replaced with java.time formatters or per-call instances.",
                        Map.of("replacement", "DateTimeFormatter")
                ));
                case "singleton-async-threadlocal-context-risk" -> {
                    overlaySuggestions.add(new RuntimeSafetyOverlaySuggestion(
                            "overlay-" + finding.ruleId() + "-" + sanitize(finding.beanName()),
                            finding.ruleId(),
                            "spring-bean-method",
                            finding.className(),
                            null,
                            "safety.async.context." + sanitize(finding.beanName()),
                            "Trace async context handoff",
                            "Add method-level probes around async entry points to verify whether request or tenant context is preserved.",
                            Map.of(
                                    "capturePhase", "AFTER_RETURN",
                                    "expressionHint", "result",
                                    "targetField", finding.fieldName()
                            )
                    ));
                    patchSuggestions.add(patchSuggestion(
                            finding,
                            "remove-threadlocal-from-async-bean",
                            "Remove ThreadLocal from async singleton",
                            "Async methods combined with ThreadLocal state usually lose context across threads unless propagation is explicit.",
                            Map.of("replacement", "explicit-context-argument")
                    ));
                }
                case "singleton-unbounded-queue-field" -> {
                    overlaySuggestions.add(new RuntimeSafetyOverlaySuggestion(
                            "overlay-" + finding.ruleId() + "-" + sanitize(finding.beanName()),
                            finding.ruleId(),
                            "spring-bean-method",
                            finding.className(),
                            null,
                            "safety.queue.depth." + sanitize(finding.beanName()),
                            "Observe queue depth around producers and consumers",
                            "Add method-level probes on queue producer or consumer methods so the agent can verify whether backlog is growing.",
                            Map.of(
                                    "capturePhase", "AFTER_RETURN",
                                    "expressionHint", "queue-size-or-backlog",
                                    "targetField", finding.fieldName()
                            )
                    ));
                    patchSuggestions.add(patchSuggestion(
                            finding,
                            "bound-queue-capacity",
                            "Bound queue capacity",
                            "Unbounded queues on singleton beans can retain memory indefinitely and should be replaced with bounded backpressure-aware buffers.",
                            Map.of("replacement", "bounded-blocking-queue")
                    ));
                }
                case "singleton-non-atomic-counter-field" -> patchSuggestions.add(patchSuggestion(
                        finding,
                        "replace-counter-with-atomic",
                        "Replace counter with atomic type",
                        "Shared counter-like fields on singleton beans should use atomic primitives or explicit synchronization.",
                        Map.of("replacement", "AtomicInteger-or-LongAdder")
                ));
                default -> {
                    // No remediation template for this finding yet.
                }
            }
        }

        return new RuntimeSafetyRemediationPlan(
                report.capabilityId(),
                report.findings(),
                overlaySuggestions,
                patchSuggestions
        );
    }

    private RuntimeSafetyPatchSuggestion patchSuggestion(
            RuntimeSafetyFinding finding,
            String templateId,
            String title,
            String reason,
            Map<String, String> parameters
    ) {
        return new RuntimeSafetyPatchSuggestion(
                "patch-" + templateId + "-" + sanitize(finding.beanName()) + "-" + sanitize(finding.fieldName()),
                finding.ruleId(),
                templateId,
                finding.className(),
                finding.fieldName(),
                title,
                reason,
                true,
                parameters
        );
    }

    private String sanitize(String value) {
        return value == null ? "unknown" : value.replaceAll("[^a-zA-Z0-9]+", "-").replaceAll("(^-+|-+$)", "").toLowerCase();
    }
}
