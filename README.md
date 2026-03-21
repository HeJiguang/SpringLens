# Spring Lens

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
  - Spring Boot auto-configuration, runtime query API, request filter, MVC exception interceptor, JDBC observation aspect
- `spring-lens-server`
  - external control plane with application registry, tool registry/router, playbooks, and MCP-exposed tools
- `spring-lens-demo-app`
  - runnable example application backed by H2

## Built-in MVP Tools

- `list_registered_apps`
- `get_slow_sql`
- `get_exception_context`
- `get_execution_graph`
- `get_diagnostic_playbook`
- `list_project_tools`
- `invoke_project_tool`
- `query_probe_values`

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

## Build

```bash
mvn test
```

## Run The Server

```bash
mvn -pl spring-lens-server spring-boot:run
```

The server starts on `http://localhost:8090`.

## Run The Demo App

```bash
mvn -pl spring-lens-demo-app spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081 --spring.lens.registration-enabled=true --spring.lens.server-url=http://localhost:8090 --spring.lens.runtime-base-url=http://localhost:8081"
```

The demo app exposes:

- `GET /orders/1`
- `GET /orders/slow`
- `GET /orders/fail`

The runtime query API exposed by the starter is:

- `GET /internal/spring-lens/slow-sql`
- `GET /internal/spring-lens/exception-context`
- `GET /internal/spring-lens/graphs/{executionId}`
- `GET /internal/spring-lens/probes`
- `GET /internal/spring-lens/probe-values?probeId=...`
- `GET /internal/spring-lens/project-tools`
- `POST /internal/spring-lens/project-tools/{toolName}:invoke`

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
   - `list_project_tools`
   - `invoke_project_tool`
   - `query_probe_values`
4. Correlate the returned probe values with `get_execution_graph`.

## Notes

- Runtime storage is currently in-memory and optimized for MVP clarity.
- Multi-application registration is supported by the external server.
- The workspace is not currently a Git repository, so there are no commits for the generated files.
