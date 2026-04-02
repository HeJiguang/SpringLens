# Spring Lens Overview

Spring Lens is an AI runtime system for Spring Boot applications.

Its job is not to replace tracing, APM, or logs. Its job is to give coding agents a safer and higher-level runtime interface for understanding what a Spring application is doing right now.

## Core idea

Most coding agents are strong at:

- reading source code
- proposing patches
- reasoning about framework APIs

Most coding agents are weak at:

- reconstructing runtime behavior from fragmented logs
- knowing whether a bug is still happening after a patch
- asking bounded, high-value runtime questions

Spring Lens closes that gap by turning runtime behavior into:

- structured execution graphs
- task-oriented runtime tools
- governable overlay and patch-draft workflows

## Product shape

Spring Lens is split into two planes:

- application-side runtime capture inside the Spring Boot service
- external control-plane and MCP surface in `spring-lens-server`

This separation matters because it keeps the runtime surface close to the app while keeping governance, routing, and agent-facing workflows outside the business service.

## What makes it useful to coding agents

Spring Lens is optimized for questions an agent can act on:

- What request failed and why?
- Which SQL call was slow?
- What did the execution graph look like?
- Is this service carrying runtime safety risks such as singleton `ThreadLocal` state?
- What remediation should be proposed and promoted?

That is a very different shape from exposing low-level bean dumps or raw framework internals.

## Runtime safety loop

The most compelling end-to-end flow in the current repository is:

1. `inspect_runtime_safety`
2. `draft_runtime_safety_remediation`
3. `promote_runtime_safety_remediation`

This is the shortest path from "the runtime looks unsafe" to "a governable remediation draft exists in the control plane".

## Intended open-source positioning

Spring Lens should be described as:

> The runtime control plane that helps coding agents understand and remediate Spring Boot behavior.

That positioning is stronger than describing it as a generic MCP server or another observability wrapper.

