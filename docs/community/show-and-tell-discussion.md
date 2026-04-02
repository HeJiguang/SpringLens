# Show and tell: using Spring Lens with Codex/Cursor

Spring Lens is now at the point where it can be shown as a real coding-agent runtime workflow rather than a pure architecture sketch.

The shortest useful demo is:

```text
inspect_runtime_safety -> draft_runtime_safety_remediation -> promote_runtime_safety_remediation
```

Why this flow matters:

- it starts from live runtime truth
- it produces reviewable remediation drafts
- it keeps the loop governed instead of letting the agent improvise unchecked instrumentation

If you try Spring Lens with Codex, Cursor, CloudCode, or another MCP-capable client, share:

- which tool flow you used
- what the agent understood better with Spring Lens than with logs alone
- where the current runtime tools still feel too low-level or incomplete

Questions I especially want feedback on:

- Which runtime questions should become first-class tools next?
- Where should the control plane stay strict, and where should it be more ergonomic?
- What would make the setup friction lower for agent users?

