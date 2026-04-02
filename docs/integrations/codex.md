# Using Spring Lens with Codex

Spring Lens is most useful with Codex when source-level reasoning is not enough and you want runtime evidence before changing code.

## What Codex gets from Spring Lens

- execution graphs instead of guesswork from logs
- slow SQL and exception context tied to real executions
- runtime safety inspection before proposing concurrent-state fixes
- remediation drafts that can be reviewed before anything is promoted

## Recommended workflow

1. Start `spring-lens-server`.
2. Start the target Spring Boot application with `spring-lens-starter`.
3. Make sure the application is registered with the control plane.
4. Connect Codex to the Spring Lens MCP surface.
5. Keep the prompt narrow and evidence-oriented.

Example:

```text
Use Spring Lens before proposing a patch.
Inspect runtime safety first, then draft remediation, then explain which reviewable change should be considered next.
```

## Good first tasks for Codex

- "Inspect why this request path is unstable before changing code."
- "Check runtime safety risks before proposing a concurrency fix."
- "Call inspect_runtime_safety, then draft_runtime_safety_remediation, and summarize the top reviewable actions."

## Repository-local skill

The repository includes a Codex-ready skill here:

- [skills/codex/spring-lens-runtime-safety/SKILL.md](../../skills/codex/spring-lens-runtime-safety/SKILL.md)

You can copy that folder into your local Codex skills directory or adapt it to your own internal skill registry.

