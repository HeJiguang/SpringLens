package io.springlens.server.tool;

import io.springlens.agent.contract.AgentActionRiskLevel;
import io.springlens.agent.contract.AgentInstrumentationMode;
import io.springlens.agent.contract.OverlaySpec;
import io.springlens.agent.contract.PatchProposalDraft;
import io.springlens.agent.contract.RuntimeSafetyDraftBundle;
import io.springlens.model.AppRegistration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
final class RuntimeSafetyDraftMapper {

    private static final String SOURCE_TOOL_NAME = "plan_runtime_safety_remediation";
    private static final String DEFAULT_CAPABILITY_ID = "spring-lens.runtime-safety";

    RuntimeSafetyDraftBundle map(AppRegistration registration, Object runtimeResult) {
        Map<String, Object> rawBundle = castMap(runtimeResult);
        String capabilityId = optionalString(rawBundle, "capabilityId", DEFAULT_CAPABILITY_ID);
        List<OverlaySpec> overlayDrafts = mapOverlayDrafts(registration, rawBundle.get("overlaySuggestions"));
        List<PatchProposalDraft> patchDrafts = mapPatchDrafts(rawBundle.get("patchSuggestions"));
        Map<String, String> metadata = new LinkedHashMap<>(stringMap(rawBundle.get("metadata")));
        metadata.putIfAbsent("sourceTool", SOURCE_TOOL_NAME);
        metadata.putIfAbsent("applicationId", registration.applicationId());
        metadata.putIfAbsent("instanceId", registration.instanceId());
        return new RuntimeSafetyDraftBundle(
                registration.applicationId(),
                registration.instanceId(),
                capabilityId,
                overlayDrafts,
                patchDrafts,
                Map.copyOf(metadata)
        );
    }

    private List<OverlaySpec> mapOverlayDrafts(AppRegistration registration, Object value) {
        List<Map<String, Object>> suggestions = mapList(value);
        List<OverlaySpec> drafts = new ArrayList<>();
        for (Map<String, Object> suggestion : suggestions) {
            Map<String, String> parameters = stringMap(suggestion.get("parameters"));
            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put("sourceTool", SOURCE_TOOL_NAME);
            putIfText(metadata, "sourceSuggestionId", optionalString(suggestion, "suggestionId", null));
            putIfText(metadata, "sourceRuleId", optionalString(suggestion, "basedOnRuleId", null));
            putIfText(metadata, "title", optionalString(suggestion, "title", null));
            metadata.put("applicationId", registration.applicationId());
            metadata.put("instanceId", registration.instanceId());
            drafts.add(new OverlaySpec(
                    firstNonBlank(parameters.get("overlayId"), optionalString(suggestion, "suggestionId", null)),
                    AgentInstrumentationMode.of(firstNonBlank(parameters.get("mode"), AgentInstrumentationMode.HYBRID_APPROVAL.value())),
                    AgentActionRiskLevel.of(firstNonBlank(parameters.get("riskLevel"), AgentActionRiskLevel.MEDIUM.value())),
                    true,
                    firstNonBlank(parameters.get("ttl"), "PT2H"),
                    requireString(suggestion, "selectorType"),
                    optionalString(suggestion, "targetClassName", null),
                    optionalString(suggestion, "targetMethodName", null),
                    firstNonBlank(parameters.get("capturePhase"), "AFTER_RETURN"),
                    optionalString(suggestion, "probeId", null),
                    firstNonBlank(parameters.get("expression"), "result"),
                    firstNonBlank(parameters.get("description"), optionalString(suggestion, "rationale", optionalString(suggestion, "title", null))),
                    List.of("runtime-safety", "draft"),
                    Map.copyOf(metadata)
            ));
        }
        return List.copyOf(drafts);
    }

    private List<PatchProposalDraft> mapPatchDrafts(Object value) {
        List<Map<String, Object>> suggestions = mapList(value);
        List<PatchProposalDraft> drafts = new ArrayList<>();
        for (Map<String, Object> suggestion : suggestions) {
            Map<String, String> parameters = stringMap(suggestion.get("parameters"));
            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put("sourceTool", SOURCE_TOOL_NAME);
            putIfText(metadata, "sourceSuggestionId", optionalString(suggestion, "suggestionId", null));
            putIfText(metadata, "sourceRuleId", optionalString(suggestion, "basedOnRuleId", null));
            putIfText(metadata, "targetClassName", optionalString(suggestion, "targetClassName", null));
            putIfText(metadata, "targetFieldName", optionalString(suggestion, "targetFieldName", null));
            drafts.add(new PatchProposalDraft(
                    requireString(suggestion, "suggestionId"),
                    AgentActionRiskLevel.of(firstNonBlank(parameters.get("riskLevel"), AgentActionRiskLevel.HIGH.value())),
                    requireString(suggestion, "templateId"),
                    requireString(suggestion, "title"),
                    requireString(suggestion, "reason"),
                    targetFiles(parameters),
                    optionalBoolean(suggestion.get("requiresApproval"), true),
                    firstNonBlank(parameters.get("originSkillId"), "runtime-safety-remediation"),
                    Map.copyOf(metadata)
            ));
        }
        return List.copyOf(drafts);
    }

    private List<String> targetFiles(Map<String, String> parameters) {
        String singleFile = normalizeNullable(parameters.get("targetFile"));
        if (singleFile != null) {
            return List.of(singleFile);
        }
        String fileList = normalizeNullable(parameters.get("targetFiles"));
        if (fileList == null) {
            return List.of();
        }
        List<String> files = new ArrayList<>();
        for (String candidate : fileList.split(",")) {
            String normalized = normalizeNullable(candidate);
            if (normalized != null) {
                files.add(normalized);
            }
        }
        return List.copyOf(files);
    }

    private List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof Iterable<?> items)) {
            return List.of();
        }
        List<Map<String, Object>> mapped = new ArrayList<>();
        for (Object item : items) {
            mapped.add(castMap(item));
        }
        return List.copyOf(mapped);
    }

    private Map<String, Object> castMap(Object value) {
        if (!(value instanceof Map<?, ?> rawMap)) {
            throw new IllegalArgumentException("Runtime safety remediation result must be a JSON object.");
        }
        Map<String, Object> values = new LinkedHashMap<>();
        rawMap.forEach((key, mapValue) -> {
            if (key != null) {
                values.put(String.valueOf(key), mapValue);
            }
        });
        return Map.copyOf(values);
    }

    private Map<String, String> stringMap(Object value) {
        if (!(value instanceof Map<?, ?> rawMap)) {
            return Map.of();
        }
        Map<String, String> values = new LinkedHashMap<>();
        rawMap.forEach((key, mapValue) -> {
            if (key == null || mapValue == null) {
                return;
            }
            String normalizedKey = normalizeNullable(String.valueOf(key));
            String normalizedValue = normalizeNullable(String.valueOf(mapValue));
            if (normalizedKey != null && normalizedValue != null) {
                values.put(normalizedKey, normalizedValue);
            }
        });
        return Map.copyOf(values);
    }

    private String requireString(Map<String, Object> arguments, String key) {
        String value = optionalString(arguments, key, null);
        if (value == null) {
            throw new IllegalArgumentException(key + " must not be blank");
        }
        return value;
    }

    private String optionalString(Map<String, Object> arguments, String key, String fallback) {
        Object value = arguments.get(key);
        return firstNonBlank(value == null ? null : String.valueOf(value), fallback);
    }

    private boolean optionalBoolean(Object value, boolean fallback) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        String normalized = normalizeNullable(value == null ? null : String.valueOf(value));
        return normalized == null ? fallback : Boolean.parseBoolean(normalized);
    }

    private String firstNonBlank(String preferred, String fallback) {
        String normalizedPreferred = normalizeNullable(preferred);
        return normalizedPreferred != null ? normalizedPreferred : normalizeNullable(fallback);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void putIfText(Map<String, String> metadata, String key, String value) {
        String normalized = normalizeNullable(value);
        if (normalized != null) {
            metadata.put(key, normalized);
        }
    }
}
