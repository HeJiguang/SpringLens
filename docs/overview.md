# Spring Lens Overview

Spring Lens is a runtime control plane for Spring Boot applications.

It is built for a simple but important problem: coding agents can read source code well, but they are much less reliable when they have to infer live behavior from logs, stack traces, and framework internals.

Spring Lens gives them a better interface.

## What Spring Lens exposes

Instead of exposing low-level implementation detail directly, Spring Lens exposes:

- execution graphs that describe what happened
- runtime tools that answer task-oriented questions
- remediation workflows that connect findings to reviewable next steps

That shape matters. It lets an agent move from "something looks wrong" to "here is a concrete finding and a reviewable remediation path" without inventing its own instrumentation story.

## Core design

Spring Lens is split into two planes:

- application-side capture inside the Spring Boot service
- an external control plane and MCP surface in `spring-lens-server`

That separation keeps runtime truth close to the application while preserving a clean boundary for governance, routing, and agent-facing workflows.

## Where it is strongest today

The strongest current path in the repository is runtime safety inspection and remediation drafting:

1. `inspect_runtime_safety`
2. `draft_runtime_safety_remediation`
3. `promote_runtime_safety_remediation`

That path is a good summary of the project itself. Spring Lens is not only about observing runtime behavior. It is about making runtime findings actionable and governable.

## Intended positioning

The most useful way to describe Spring Lens is:

> A runtime control plane that helps coding agents understand and remediate Spring Boot behavior.

That is more precise than calling it a generic MCP server, observability wrapper, or agent plugin framework.

