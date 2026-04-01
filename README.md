# Spring Lens

[![CI](https://github.com/HeJiguang/SpringLens/actions/workflows/ci.yml/badge.svg)](https://github.com/HeJiguang/SpringLens/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/HeJiguang/SpringLens/blob/main/LICENSE)

`Spring Lens` is an AI Runtime Operating System for Spring Boot applications.

It does not treat logs as the primary diagnostic surface. Instead, it captures runtime behavior as structured execution graphs and exposes question-oriented runtime tools for AI systems.

## Modules

- `spring-lens-model`
  - shared runtime graph model and registration DTOs
- `spring-lens-spi`
  - plugin contracts for collectors, tools, and playbooks
- `spring-lens-runtime`
  - in-memory graph assembly and built-in collectors for HTTP, exception, and JDBC slow SQL
- `spring-lens-starter`
  - default low-risk Spring Boot starter with observation-native capture foundations, runtime query API, and compatibility instrumentation switches
- `spring-lens-agent-contract`
  - shared agent-side contracts for overlay specs, patch proposals, and instrumentation modes
- `spring-lens-agent-starter`
  - explicit high-trust extension for agent-origin instrumentation and future overlay/patch control
- `spring-lens-server`
  - external control plane with application registry, tool registry/router, playbooks, and MCP-exposed tools
- `spring-lens-demo-app`
  - runnable example application backed by H2

## Adoption Path

- Add `spring-lens-starter` when you want runtime truth, execution graphs, and AI-facing runtime tools with the lowest adoption risk.
- Add `spring-lens-agent-starter` only when you want to opt into future high-trust agent instrumentation flows such as overlays or governed source patch proposals.

Current Phase 1 defaults:

- `spring.lens.observation-native-enabled=true`
- `spring.lens.compatibility-instrumentation-enabled=true`

This means the observation-native path is present and testable, while legacy filter/interceptor/aspect capture remains enabled as the safe compatibility path during the transition.

Current Phase 2 skeleton:

- `spring-lens-server` now exposes an in-memory agent control plane for:
  - policy snapshot reads
  - overlay registration, approval, and disable flows
  - audit trail listing
- these control-plane flows are intentionally skeletal in this phase:
  - no runtime overlay delivery yet
  - no distributed lease management yet
  - approval state remains in-memory only

Current Phase 3 skeleton:

- `spring-lens-server` now exposes an internal overlay delivery snapshot endpoint per application:
  - `GET /internal/apps/{applicationId}/agent-overlays`
  - optional query parameter: `instanceId`
- `spring-lens-agent-starter` can now:
  - pull the current control-plane snapshot from the server at startup
  - store filtered active overlays in the local `AgentOverlayEngine`
- this phase still does not execute runtime weaving:
  - it synchronizes overlay state only
  - actual interceptor/aspect/filter attachment remains a later phase

## Built-in MVP Tools

Runtime and diagnosis:

- `list_registered_apps`
- `get_slow_sql`
- `get_exception_context`
- `get_execution_graph`
- `diagnose_execution_graph`
- `get_diagnostic_playbook`
- `list_runtime_tools`
- `invoke_runtime_tool`
- `query_probe_values`
- `inspect_runtime_safety`
- `draft_runtime_safety_remediation`

Control-plane skeleton:

- `get_policy_snapshot`
- `list_active_overlays`
- `list_patch_drafts`
- `apply_overlay_instrumentation`
- `approve_overlay_instrumentation`
- `disable_overlay_instrumentation`
- `list_audit_events`
- `promote_runtime_safety_remediation`

## Agent Overlay Delivery

`spring-lens-agent-starter` now includes a startup overlay sync path intended for coding-agent environments:

- it resolves the server endpoint from `spring.lens.server-url`
- it pulls `GET /internal/apps/{applicationId}/agent-overlays?instanceId=...`
- it filters overlays again against the local `applicationId` and `instanceId`
- it stores the accepted overlay specs inside the local `AgentOverlayEngine`

Current delivery properties:

- `spring.lens.agent.instrumentation.startup-sync-enabled=true`
- `spring.lens.agent.instrumentation.periodic-refresh-enabled=true`
- `spring.lens.agent.instrumentation.refresh-interval-millis=30000`
- `spring.lens.agent.instrumentation.overlay-pull-path=/internal/apps/{applicationId}/agent-overlays`

This delivery path now enforces a controlled subset of runtime safety:

- only `APPROVED` overlays are delivered into live runtime activation
- overlays expire locally when their TTL has elapsed
- periodic refresh can revoke overlays after server-side approval or disable changes

Current Phase 4 activation slice:

- synced overlays can now emit agent-origin probe captures through two low-risk selector types:
  - `spring-bean-method`
  - `http-route`
- overlay captures now land in the execution graph as `AGENT_OVERLAY` watch nodes
- current activation remains intentionally narrow:
  - no bytecode weaving
  - no arbitrary expression execution
  - no patch auto-apply

## Programmable Runtime Probes

Spring Lens now supports project-defined runtime observability primitives aimed at AI agents:

- `@LensWatch`
  - captures method input, return, or exception values into the execution graph
- `Lens.look(...)`
  - captures local or temporary values inside an active request execution
- `@LensTool`
  - defines a project-specific callable runtime function discoverable through Spring Lens

Example:

```java
@LensWatch(id = "order.lookup.result", description = "Observe repository lookup result")
public OrderDto loadOrder(long id) {
    OrderDto order = repository.find(id);
    Lens.look("order.local.status", order.status(), "Status after repository lookup");
    return order;
}

@LensTool(name = "count_orders_by_status", description = "Count orders grouped by status.")
public Map<String, Object> countOrdersByStatus(@LensToolParam("status") String status) {
    return Map.of("status", status, "count", repository.countByStatus(status));
}
```

## Agent Safety Inspection

`spring-lens-starter` now includes a built-in runtime safety capability that exposes:

- `inspect_runtime_safety`
- `plan_runtime_safety_remediation`

This tool is intended for coding agents that need high-level safety signals instead of raw bean dumps. The first slice checks singleton bean patterns that commonly lead to thread-safety or memory-retention bugs, including:

- `ThreadLocal` state kept on singleton beans
- `@Async` methods combined with singleton `ThreadLocal` state
- manual `ExecutorService` ownership without explicit shutdown lifecycle
- unbounded singleton queues that can retain memory under sustained load
- shared mutable collections backed by non-thread-safe implementations
- non-atomic counter-like fields shared across singleton beans
- thread-unsafe formatter instances such as `SimpleDateFormat`

`plan_runtime_safety_remediation` builds on those findings and returns:

- overlay suggestions for targeted runtime verification
- reviewable patch suggestions aligned with likely code fixes

`spring-lens-server` now adds a control-plane planning bridge on top of that runtime capability:

- `draft_runtime_safety_remediation`
- `promote_runtime_safety_remediation`
- `list_patch_drafts`

This server-side tool invokes the runtime remediation planner on a registered application and maps the result into:

- control-plane-ready `OverlaySpec` drafts
- honest `PatchProposalDraft` records that do not pretend to contain final diffs yet
- registered overlay submissions and governed patch-draft registrations once promoted into the control plane

## Build

```bash
mvn test
```

## Run The Server

```bash
mvn -f spring-lens-server/pom.xml spring-boot:run
```

The server starts on `http://localhost:8090`.

## Run The Demo App

```bash
mvn -pl spring-lens-demo-app -am clean install -DskipTests "-Dspring-boot.repackage.skip=true"
mvn -f spring-lens-demo-app/pom.xml spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081 --spring.lens.registration-enabled=true --spring.lens.server-url=http://localhost:8090 --spring.lens.runtime-base-url=http://localhost:8081"
```

The demo app exposes:

- `GET /orders/1`
- `GET /orders/slow`
- `GET /orders/fail`

The runtime query API exposed by the starter is:

- `GET /internal/spring-lens/slow-sql`
- `GET /internal/spring-lens/exception-context`
- `GET /internal/spring-lens/graphs/{executionId}`
- `GET /internal/spring-lens/capabilities`
- `GET /internal/spring-lens/tools`
- `GET /internal/spring-lens/tools/schema`
- `GET /internal/spring-lens/probes`
- `GET /internal/spring-lens/probe-values?probeId=...`
- `POST /internal/spring-lens/tools/{toolName}:invoke`

## Demo Flow

1. Start `spring-lens-server`.
2. Start `spring-lens-demo-app` with registration enabled.
3. Hit `http://localhost:8081/orders/slow`.
4. Connect an MCP client to the server and call `get_slow_sql` for application `spring-lens-demo`.
5. Hit `http://localhost:8081/orders/fail`.
6. Call `get_exception_context` or `get_diagnostic_playbook`.

For project-defined runtime capabilities:

1. Add `@LensWatch`, `Lens.look(...)`, or `@LensTool` to application code.
2. Start the application with the starter on the classpath.
3. Use:
   - `list_runtime_tools`
   - `invoke_runtime_tool`
   - `query_probe_values`
4. Correlate the returned probe values with `get_execution_graph`.

## Notes

- Runtime storage is currently in-memory and optimized for MVP clarity.
- Multi-application registration is supported by the external server.
- `spring-lens-starter` is the default dependency; `spring-lens-agent-starter` is the explicit higher-trust extension.

## Community

- Contribution guide: [CONTRIBUTING.md](./CONTRIBUTING.md)
- Code of conduct: [CODE_OF_CONDUCT.md](./CODE_OF_CONDUCT.md)
- Security policy: [SECURITY.md](./SECURITY.md)
- Support: [SUPPORT.md](./SUPPORT.md)
