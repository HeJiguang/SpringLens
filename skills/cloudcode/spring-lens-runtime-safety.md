# Spring Lens Runtime Safety for CloudCode

Use Spring Lens before proposing source changes for Spring Boot runtime-safety issues.

## Preferred chain

```text
inspect_runtime_safety -> draft_runtime_safety_remediation -> promote_runtime_safety_remediation
```

## Prompt block

```text
You have access to Spring Lens.
Before proposing a fix, inspect runtime safety findings for the target Spring Boot application.
Then draft remediation and summarize the smallest reviewable change.
Only promote remediation after explaining the tradeoff and expected effect.
Prefer execution-graph and runtime-safety evidence over raw log speculation.
```

## Expected response shape

- Top runtime safety findings
- Drafted overlays and patch suggestions
- What should be promoted now
- What still needs human review

## Guardrails

- Do not bypass Spring Lens if the issue is clearly runtime-behavior related.
- Do not claim a remediation is verified unless Spring Lens evidence supports it.
- Keep the explanation tied to the application id and the tool results you observed.

