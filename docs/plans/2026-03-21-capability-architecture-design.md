# Capability Architecture Design

## Goal

Refactor Spring Lens so `capability` becomes the single plugin and discovery concept across starter, runtime, and server, while preserving `tool` as the AI-facing task surface and `probe` as the runtime semantic signal surface.

## Product Boundary

- This is an internal architecture reset, not a compatibility release.
- Historical `project tool` terminology and APIs may be removed outright.
- `Capability` is the packaging and discovery unit.
- `Tool` remains task-oriented and AI-facing.
- `Probe` remains a passive semantic signal captured into the `ExecutionGraph`.
- The hybrid topology does not change:
  - capture stays inside the Spring Boot application
  - `spring-lens-server` stays the external control plane and MCP adapter

## Problem Statement

The current implementation splits extensibility across three different models:

- starter-side annotation scanning and generated tools are wired through `LensProjectToolRegistry`
- server-side MCP exposure is wired through `DiagnosticTool` and `ToolRegistry`
- runtime-to-server transport still uses `ProjectTool*` DTOs

This makes capability discovery non-uniform, leaks historical naming into the public surface, and prevents starter plugins from being first-class citizens in the architecture.

## Recommended Architecture

### 1. Capability Layer

`Capability` becomes the primary extension unit. A capability is a plugin that contributes one or both of:

- probes
- tools

Each capability has stable metadata:

- `id`
- `name`
- `description`
- `kind`
- `source`

Starter auto-configuration discovers `List<LensCapability>` and materializes a single `LensCapabilityRegistry`.

This means built-ins and user-defined extensions share the exact same path:

- built-in runtime capabilities
- annotation-backed tool capabilities
- generated capabilities
- user-provided application capabilities

### 2. Tool Layer

`Tool` remains the AI interface and stays explicitly task-oriented. A tool is callable, schematized, and routable. It is not renamed to `capability`, because that would blur the boundary between plugin packaging and AI interaction.

The current split between starter-generated callable types and server-side `DiagnosticTool` should be collapsed into one shared callable contract in SPI.

That shared contract should own:

- tool metadata
- exposure level
- input schema
- output schema
- annotations
- invocation contract

Server MCP exposure should continue to operate on tools only.

### 3. Probe Layer

`Probe` continues to represent runtime truth as semantic signals attached to executions. Probes remain distinct from tools:

- probes are observed
- tools are invoked

Capabilities may contribute probes, but a probe is never treated as an implicit tool. This preserves the original design intent that runtime truth lives in the graph and AI-facing actions stay high-level.

## Starter Design

Starter becomes the capability host.

### Registry

Introduce a `LensCapabilityRegistry` that aggregates every discovered capability and provides read models for:

- capability catalog
- tool catalog
- probe catalog

It also owns invocation of capability-contributed tools.

### First-party capabilities

The existing built-ins should be re-expressed as first-party capabilities:

- diagnosis capability
  - contributes `diagnose_request`
- annotation capability
  - adapts `@LensTool` methods into contributed tools
- generated capability
  - contributes tools inferred from controller mappings and related probes

The existing probe capture flow remains valid, but probe registration is published through the capability registry instead of an isolated project-tool registry.

### User-defined capability model

Applications should be able to register a plain Spring bean that implements the new capability SPI and have it discovered automatically by the starter. That is the key proof that capability is a starter first-class concept rather than a documentation alias.

## Server Design

The server should stop treating runtime-defined tools as a special secondary category.

### Unified routing

Server routing should operate on the same shared callable tool contract used by the starter. The existing `DiagnosticTool`-only server model should be folded into the unified tool contract, or `DiagnosticTool` should be renamed and generalized until no server-specific naming remains in the shared SPI.

### Runtime client

The runtime observation client should query the starter for:

- capability descriptors
- tool descriptors
- tool schemas
- probe descriptors
- probe values

Historical `ProjectTool*` transport DTOs should be removed.

### MCP surface

MCP continues to expose tools, not capabilities. The external generic tools should be renamed to match the new runtime vocabulary:

- `list_runtime_tools`
- `invoke_runtime_tool`
- `query_probe_values`

Capabilities are discoverable through runtime APIs and internal catalogs, but the AI-facing action surface remains tool-oriented.

## Runtime HTTP API

The starter-side runtime API should move to this vocabulary:

- `GET /internal/spring-lens/capabilities`
- `GET /internal/spring-lens/tools`
- `GET /internal/spring-lens/tools/schema`
- `POST /internal/spring-lens/tools/{toolName}:invoke`
- `GET /internal/spring-lens/probes`
- `GET /internal/spring-lens/probe-values`
- existing graph, slow SQL, and exception endpoints remain in place

The historical `/project-tools` endpoints should be removed rather than aliased.

## Naming Decisions

- Keep `probe` because it accurately describes a semantic observation signal.
- Keep `tool` because it accurately describes an AI-callable task surface.
- Promote `capability` to the plugin and discovery layer above both.
- Remove `project tool` and `generated skill` because they no longer express a unique architectural role.

## Migration and Deletion Policy

This refactor does not preserve historical naming for compatibility. The following categories are expected to disappear:

- `ProjectToolDescriptor`
- `ProjectToolSchemaDescriptor`
- `ProjectToolSourceType`
- `LensProjectToolRegistry`
- `ProjectToolInvocationRequest`
- `GeneratedSkillTool`
- `SkillGenerator`
- `SkillGenerationRequest`
- `/internal/spring-lens/project-tools*`
- `list_project_tools`
- `invoke_project_tool`

## Testing Strategy

- SPI tests prove the capability contract can describe contributed tools and probes.
- Starter tests prove the registry aggregates built-in and user-defined capabilities.
- Demo integration tests prove a user-defined capability bean is discovered and its tool is invokable through the runtime API.
- Server tests prove runtime tool listing and invocation route cleanly through the renamed generic MCP tools.
- Full-reactor verification remains `mvn test`.

## Expected Outcome

After the refactor:

- starter plugins are first-class capabilities
- runtime and server share one extension vocabulary
- AI-facing APIs stay high-level and tool-oriented
- probes stay graph-native semantic signals
- historical naming no longer shapes the architecture
