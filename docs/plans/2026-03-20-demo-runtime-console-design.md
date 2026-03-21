# Demo Runtime Console Design

## Goal

Add a lightweight frontend inside `spring-lens-demo-app` so users can understand what Spring Lens already does, trigger the implemented runtime features, and inspect the resulting structured runtime data without relying on IDEA HTTP tooling.

## Product Boundary

- The page is hosted by `spring-lens-demo-app` and only calls the demo application's business and internal runtime APIs.
- The page explains, but does not directly invoke, the external MCP server on port `8090`.
- The page remains pure `HTML`, `CSS`, and `JavaScript`; no frontend build system, framework, or npm dependencies are introduced.
- The first version is intentionally read-heavy and action-driven rather than a full observability UI or DAG renderer.

## Recommended Experience

### 1. Product introduction and capability overview

The top section should immediately explain that Spring Lens is an AI runtime operating system, not a log viewer. It should list the implemented capabilities in concrete terms:

- structured slow SQL capture
- structured exception capture
- execution graph retrieval
- programmable runtime probes
- project-defined callable tools

The same section should also explain the AI-facing surface that these features enable:

- `get_slow_sql`
- `get_exception_context`
- `get_execution_graph`
- `query_probe_values`
- `invoke_project_tool`

### 2. Runtime operation console

The center of the page should act like an experiment console. Users trigger one action at a time and immediately see the runtime evidence that Spring Lens captured.

The first version should provide these actions:

- `Load Order`
- `Trigger Slow SQL`
- `Trigger Exception`
- `Trigger Probe`
- `Invoke Project Tool`

Each action should automatically perform the secondary follow-up API calls needed to surface the relevant runtime evidence.

### 3. Structured runtime result panels

The lower half of the page should show distinct result cards for:

- business response
- slow SQL
- exception context
- probe descriptors and probe values
- project tool result
- execution graph

Execution graph output should be shown as structured sections and formatted JSON, not as a complex force-directed graph.

## Architecture

### Static hosting

The page lives under:

- `spring-lens-demo-app/src/main/resources/static/lens/index.html`
- `spring-lens-demo-app/src/main/resources/static/lens/lens.css`
- `spring-lens-demo-app/src/main/resources/static/lens/lens.js`

To support the friendlier `/lens/` entry point, the demo app should add a minimal MVC forward that maps `/lens` and `/lens/` to `/lens/index.html`.

### Frontend data flow

The page uses a small in-browser state object with the latest:

- action name
- graph id
- business payload
- slow SQL records
- exception records
- probe descriptors
- probe value records
- project tool descriptors
- project tool invocation result
- execution graph
- error message

Action handlers update only the relevant state slices and then re-render the affected panels.

### Backend interaction model

The page only calls existing demo-app endpoints:

- `GET /orders/1`
- `GET /orders/slow`
- `GET /orders/fail`
- `GET /orders/probe/1`
- `GET /internal/spring-lens/slow-sql`
- `GET /internal/spring-lens/exception-context`
- `GET /internal/spring-lens/graphs/{executionId}`
- `GET /internal/spring-lens/probes`
- `GET /internal/spring-lens/probe-values`
- `GET /internal/spring-lens/project-tools`
- `POST /internal/spring-lens/project-tools/{toolName}:invoke`

The page does not add any new runtime APIs.

## Visual Direction

The page should feel like a runtime lab rather than a Swagger page or a monitoring dashboard.

- Use a warm light background instead of a dark default.
- Use a restrained blue and amber palette with strong contrast for result states.
- Keep large, product-like headings for explanation sections.
- Keep monospace data blocks for runtime payloads and graphs.
- Use card layouts with clear borders, elevated panels, and intentional spacing.

## Non-goals

- No new charting library
- No client-side routing
- No persistence of runs
- No direct MCP protocol client in the browser
- No live polling or streaming updates
- No graphical DAG rendering library

## Testing Strategy

- Add a demo-app integration test that requests `/lens/` and verifies the HTML contains the key product description and action labels.
- Keep the page logic dependency-free so existing backend integration tests remain the primary functional guardrail.
- Use `mvn clean test` as the verification command to avoid stale `target` output from earlier IDE-driven compilation failures.
