# Spring Lens Design

## Goal

Build `Spring Lens` as an AI Runtime Operating System for Spring Boot applications. The system does not optimize for human log reading. It optimizes for AI-driven runtime understanding through structured runtime snapshots, tool-oriented access, and diagnostic playbooks.

## Product Boundaries

- Topology: hybrid
  - runtime capture inside each Spring Boot application
  - MCP server runs as an external process
- Scope: multiple applications can register with the control plane
- MVP signals:
  - HTTP request lifecycle
  - exception context
  - JDBC slow SQL
- Non-goals:
  - dashboards and time-series monitoring
  - debugger-like execution control
  - heavy UI

## Recommended Architecture

### 1. Runtime Layer

The application side is delivered as a Spring Boot starter. It instruments request handling and JDBC operations, assembles an `ExecutionGraph`, and exposes a compact runtime query API. The unit of capture is a single execution, usually one HTTP request.

### 2. Control Plane

The external `spring-lens-server` maintains the runtime registry, routes tool calls to the right application instance, and exposes MCP tools. The server owns AI-facing concerns such as tool naming, routing, and playbook discovery.

### 3. Structured Runtime Model

Runtime data is modeled as a graph:

- `ExecutionContext`: application, instance, request, start time, tags
- `ExecutionNode`: typed unit such as HTTP request, JDBC SQL, exception
- `ExecutionEdge`: relation between nodes
- `ExecutionGraph`: immutable snapshot returned to tools

### 4. Plugin SPI

The plugin boundary is intentionally small:

- `RuntimeCollector`: consumes structured runtime signals and mutates the graph
- `DiagnosticTool`: serves AI questions such as `get_slow_sql`
- `DiagnosticPlaybook`: codifies step-by-step diagnosis paths

## MVP Runtime Flow

1. HTTP request enters application.
2. Runtime starter creates an execution session and emits `HTTP_REQUEST_STARTED`.
3. JDBC calls emit `JDBC_EXECUTED` signals with SQL text and duration.
4. MVC completion emits exception information if present.
5. Request completion emits `HTTP_REQUEST_COMPLETED` and finalizes the graph.
6. `spring-lens-server` routes MCP tool calls to the target application runtime API.

## Known Constraint

The workspace is not a Git repository on `2026-03-19`, so this design document cannot be committed from the current environment.
