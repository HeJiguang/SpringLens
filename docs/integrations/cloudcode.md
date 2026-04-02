# Using Spring Lens with CloudCode

Spring Lens works well with CloudCode-style workflows when you want runtime evidence to shape the next code change instead of relying on source inspection alone.

## What CloudCode gets from Spring Lens

- structured execution context for real requests
- high-signal runtime tools instead of one-off debug endpoints
- runtime safety findings before proposing risky concurrency changes
- reviewable remediation drafts that humans can inspect before promotion

## Recommended workflow

1. Start `spring-lens-server`.
2. Start the target Spring Boot application with `spring-lens-starter`.
3. Confirm that the application is registered in the control plane.
4. Point CloudCode at the Spring Lens server or carry the prompt pattern below into the session.
5. Use the runtime safety chain as the first serious demo:

   ```text
   inspect_runtime_safety -> draft_runtime_safety_remediation -> promote_runtime_safety_remediation
   ```

## Prompt pattern

```text
When debugging Spring Boot behavior, use Spring Lens first.
Prefer execution graphs, runtime safety findings, and remediation drafts over log-only speculation.
Do not propose a patch until you have checked whether Spring Lens already exposes a relevant runtime tool for the issue.
```

## Repository-local guidance

The repository includes a CloudCode-oriented guidance file here:

- [skills/cloudcode/spring-lens-runtime-safety.md](../../skills/cloudcode/spring-lens-runtime-safety.md)

It is plain Markdown so it can be pasted into environments that do not support Codex-style packaged skills.

