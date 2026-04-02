---
name: spring-lens-runtime-safety
description: Use Spring Lens to inspect runtime safety risks, draft remediation, and promote reviewed runtime safety changes through the control plane.
---

# Spring Lens Runtime Safety

Use this skill when a Spring Boot issue may involve concurrency, singleton shared state, thread safety, memory retention, or when you want runtime-aware remediation before proposing code changes.

## Goal

Use Spring Lens before patching.

The preferred chain is:

```text
inspect_runtime_safety -> draft_runtime_safety_remediation -> promote_runtime_safety_remediation
```

## Workflow

1. Confirm the target application is registered in Spring Lens.
2. Call `inspect_runtime_safety`.
3. Summarize the highest-risk findings first.
4. Call `draft_runtime_safety_remediation`.
5. Separate overlay suggestions from patch suggestions.
6. Recommend the smallest reviewable remediation path.
7. Only call `promote_runtime_safety_remediation` after explaining what is being promoted and why.

## Output shape

When you use this skill, structure the response as:

- runtime findings
- remediation draft summary
- promotion recommendation
- source-code follow-up, if still needed

## Guardrails

- Do not jump straight to source edits if Spring Lens already exposes a relevant runtime tool.
- Do not describe remediation as "safe" unless the runtime findings support it.
- Prefer reviewed patch drafts over speculative fixes.
- Keep runtime, control-plane, and application-code concerns separate in your explanation.

