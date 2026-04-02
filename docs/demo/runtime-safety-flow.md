# Runtime Safety Demo Storyboard

Use this storyboard to capture a 30 to 60 second GIF for the repository.

## Goal

Show one strong chain, not a catalog:

```text
inspect_runtime_safety -> draft_runtime_safety_remediation -> promote_runtime_safety_remediation
```

## Scene plan

1. Show the demo app and server both running.
2. Hit `GET /orders/fail` once so there is live runtime state.
3. In the agent client, call `inspect_runtime_safety`.
4. Pause on the returned findings list.
5. Call `draft_runtime_safety_remediation`.
6. Pause on overlay and patch draft output.
7. Call `promote_runtime_safety_remediation`.
8. Pause on the promoted result or audit confirmation.

## Recording notes

- Keep the camera tight on the tool calls and results.
- Do not spend time on Maven output.
- Use one application id throughout the demo.
- Prefer readable terminal font size over showing too much screen area.

## Suggested caption

```text
Spring Lens helps coding agents inspect runtime safety, generate remediation drafts, and promote reviewed fixes through a governed control plane.
```

