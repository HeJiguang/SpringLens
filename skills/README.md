# Spring Lens Agent Skills

This repository includes reusable prompt assets for coding-agent workflows that use Spring Lens.

## Included assets

- `skills/codex/spring-lens-runtime-safety/SKILL.md`
  Codex-style packaged skill for the runtime safety inspection and remediation flow.
- `skills/cloudcode/spring-lens-runtime-safety.md`
  Plain Markdown guidance for CloudCode-like environments.

## Intent

These skills are intentionally narrow.

They focus on the strongest current product loop:

```text
inspect_runtime_safety -> draft_runtime_safety_remediation -> promote_runtime_safety_remediation
```

That keeps the onboarding story concrete and useful.

