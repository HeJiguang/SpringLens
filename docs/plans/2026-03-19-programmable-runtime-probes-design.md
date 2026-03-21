# Programmable Runtime Probes Design

## Goal

Add a second-stage capability to Spring Lens so application developers or AI coding agents can define project-specific runtime observations and project-specific callable tools without turning the system into a traditional debugger.

## Product Boundary

- Default exposure mode:
  - `@LensWatch` and `Lens.look(...)` write structured probe values into the runtime graph
  - only `@LensTool` is treated as a project-defined callable tool
- Topology remains unchanged:
  - application-side capture in starter/runtime
  - external MCP server remains the AI-facing control plane
- Non-goals for this phase:
  - runtime hot-insertion of new top-level MCP tool names without restart
  - local-variable annotations
  - pausepoints or execution control
  - complex expression engines and rule DSLs

## Recommended Capability Model

### Probe

A probe is a declared runtime observation point. It has a stable id, a description, a source, and a capture phase. Probes are not tools by default.

### Probe Value

A probe value is a captured runtime fact associated with a concrete execution. Probe values become nodes in the `ExecutionGraph` so AI systems can correlate them with HTTP requests, SQL, and exceptions.

### Project Tool

A project tool is an explicit callable capability declared by application code. It is listed and invoked through Spring Lens, and then exposed to AI systems through generic MCP entry points on the external server.

## Programming Model

### 1. Annotation-driven watchpoints

`@LensWatch` is method-scoped and supports:

- `BEFORE`
- `AFTER_RETURN`
- `AFTER_THROW`

The first implementation supports only stable capture targets:

- `#args`
- `#arg0`, `#arg1`, ...
- `#result`
- `#exception`

### 2. API-driven variable observation

`Lens.look(id, value, description)` captures local or temporary values inside an active execution context. This is the sanctioned path for observing local variables.

### 3. Project-defined tools

`@LensTool` marks a Spring bean method as a project tool. It is not expanded into a top-level MCP name in this phase. Instead, the external server exposes generic tools:

- `list_project_tools`
- `invoke_project_tool`
- `query_probe_values`

## Runtime Design

### Application Side

Starter/runtime gains:

- a probe registry for `@LensWatch` and `Lens.look(...)`
- a project-tool registry for `@LensTool`
- an aspect for method-level watchpoint capture
- a static API bridge for `Lens.look(...)`
- internal HTTP APIs to list probes, query probe values, list project tools, and invoke project tools

### Server Side

The control plane remains generic. It queries and invokes project-defined capabilities through the runtime HTTP API of each registered application instance.

## Data Model Additions

- `NodeType.WATCH_VALUE`
- `ProbeDescriptor`
- `ProbeValueRecord`
- `ProjectToolDescriptor`

Probe values carry:

- `probeId`
- `description`
- `captureSource`
- `capturePhase`
- `value`
- `valueType`
- `occurredAt`

## Safety and Constraints

- `@LensWatch` is method-only
- `Lens.look(...)` is best-effort and no-ops outside an active execution
- project tools are limited to JSON-serializable parameters and return values
- captured values are sanitized and truncated before persistence

## Testing Strategy

- starter-level tests for `@LensWatch`
- demo integration tests for `Lens.look(...)`
- demo integration tests for project tool discovery and invocation
- server tests for routing of generic project-tool and probe-query tools
