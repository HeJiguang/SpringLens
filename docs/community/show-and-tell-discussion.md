# Show and tell: using Spring Lens with Codex/Cursor

Spring Lens is now at a point where it is easier to explain with a real workflow than with an architecture diagram.

The shortest useful demo is:

```text
inspect_runtime_safety -> draft_runtime_safety_remediation -> promote_runtime_safety_remediation
```

Why this flow matters:

- it starts from live runtime evidence
- it produces reviewable remediation drafts
- it keeps the path governed instead of turning runtime debugging into ad hoc instrumentation

If you try Spring Lens with Codex, Cursor, CloudCode, or another MCP-capable client, I would be interested in:

- which runtime tool flow you used first
- what the agent understood more clearly with Spring Lens than with logs alone
- where the current runtime surface still feels too low-level or incomplete

Questions I especially want feedback on:

- Which runtime questions should become first-class tools next?
- Where should the control plane stay strict, and where should it become easier to use?
- What would reduce setup friction for teams trying this in a real codebase?

