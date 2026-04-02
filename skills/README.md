# Spring Lens Skills

This repository includes reusable prompt assets for workflows that use Spring Lens as a runtime companion.

## Included assets

- `skills/codex/spring-lens-runtime-safety/SKILL.md`
  A packaged Codex skill for the runtime safety inspection and remediation flow.
- `skills/cloudcode/spring-lens-runtime-safety.md`
  Plain Markdown guidance for CloudCode-style environments.

## Scope

These assets are intentionally narrow. They focus on the strongest current path in the repository:

```text
inspect_runtime_safety -> draft_runtime_safety_remediation -> promote_runtime_safety_remediation
```

That keeps onboarding concrete and gives new users a realistic first workflow instead of a broad but shallow prompt bundle.

