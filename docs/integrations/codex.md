# Spring Lens for Codex

Spring Lens fits Codex best when Codex already has strong source-level context and needs runtime truth before making or validating a change.

## What Codex can do with Spring Lens

- inspect execution graphs instead of guessing from logs
- ask for slow SQL and exception context tied to a real execution
- inspect runtime safety issues before proposing concurrency fixes
- generate remediation drafts that can later be reviewed and promoted

## Suggested workflow

1. Start `spring-lens-server`.
2. Start your Spring Boot app with `spring-lens-starter`.
3. Register the app with the server.
4. Configure Codex to reach the Spring Lens MCP endpoint.
5. Give Codex a narrow instruction such as:

   ```text
   Use Spring Lens before proposing a patch.
   First inspect runtime safety, then draft remediation, then explain which patch draft should be reviewed.
   ```

## Recommended Codex entry prompts

- "Use Spring Lens to inspect why this request path is unstable before changing code."
- "Use Spring Lens to inspect runtime safety risks and only then propose the smallest safe patch."
- "Call `inspect_runtime_safety`, then `draft_runtime_safety_remediation`, and summarize the top two reviewable fixes."

## Included repository skill

Use the repository-local Codex skill here:

- [skills/codex/spring-lens-runtime-safety/SKILL.md](../../skills/codex/spring-lens-runtime-safety/SKILL.md)

If you want to install it into your local Codex environment, copy that folder into your Codex skills directory or adapt it to your own skill registry.

