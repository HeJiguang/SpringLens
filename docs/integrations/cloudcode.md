# Spring Lens for CloudCode

Spring Lens also works as a runtime companion for CloudCode-style coding agents when they can consume MCP tools or follow repository-local instructions.

## What CloudCode can do with Spring Lens

- inspect runtime failures without hand-written debug endpoints
- retrieve a structured execution graph for a real request
- identify runtime safety risks before suggesting concurrent-state fixes
- create remediation drafts that can be reviewed by humans

## Suggested workflow

1. Start `spring-lens-server`.
2. Start your Spring Boot app with `spring-lens-starter`.
3. Ensure the app is registered in the control plane.
4. Configure your CloudCode environment to reach the Spring Lens server or carry the prompt pattern below.
5. Use the runtime safety chain as the default high-signal demo:

   ```text
   inspect_runtime_safety -> draft_runtime_safety_remediation -> promote_runtime_safety_remediation
   ```

## Recommended prompt snippet

```text
When debugging Spring Boot behavior, use Spring Lens first.
Prefer execution graphs, runtime safety findings, and remediation drafts over raw log speculation.
Do not propose a patch until you have checked whether Spring Lens already exposes a higher-level runtime tool for the issue.
```

## Included repository skill

Use the repository-local CloudCode guide here:

- [skills/cloudcode/spring-lens-runtime-safety.md](../../skills/cloudcode/spring-lens-runtime-safety.md)

This file is intentionally plain Markdown so it can be pasted into environments that do not use Codex-style `SKILL.md` packaging.

