# Programmable Runtime Probes Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add programmable runtime probes and project-defined callable tools to Spring Lens while keeping the external MCP surface generic and stable.

**Architecture:** Application-side starter code scans and captures `@LensWatch`, `Lens.look(...)`, and `@LensTool`, then exposes probe and project-tool APIs through the existing internal runtime HTTP surface. The external server adds generic MCP tools that discover and invoke these runtime-defined capabilities against registered applications.

**Tech Stack:** Java 21, Spring Boot 4.0.3, Spring AOP, Spring MVC, Jackson, Spring AI MCP server starter, JUnit 5

---

### Task 1: Extend the shared model for probes and project tools

**Files:**
- Modify: `spring-lens-model/src/main/java/io/springlens/model/NodeType.java`
- Create: `spring-lens-model/src/main/java/io/springlens/model/ProbeDescriptor.java`
- Create: `spring-lens-model/src/main/java/io/springlens/model/ProbeValueRecord.java`
- Create: `spring-lens-model/src/main/java/io/springlens/model/ProjectToolDescriptor.java`
- Create: `spring-lens-model/src/main/java/io/springlens/model/ProbeCapturePhase.java`
- Modify: `spring-lens-spi/src/main/java/io/springlens/spi/RuntimeSignalType.java`

**Step 1: Write the failing test**

Reference `WATCH_VALUE`, `ProbeValueRecord`, and `PROBE_VALUE_CAPTURED` from new starter or demo tests.

**Step 2: Run test to verify it fails**

Run: `mvn -pl spring-lens-demo-app -am test -Dtest=SpringLensProbeIntegrationTests "-Dsurefire.failIfNoSpecifiedTests=false"`
Expected: FAIL with missing model or signal types.

### Task 2: Add starter-side probe annotations and runtime registries

**Files:**
- Create: `spring-lens-starter/src/main/java/io/springlens/starter/probe/LensWatch.java`
- Create: `spring-lens-starter/src/main/java/io/springlens/starter/probe/LensTool.java`
- Create: `spring-lens-starter/src/main/java/io/springlens/starter/probe/LensToolParam.java`
- Create: `spring-lens-starter/src/main/java/io/springlens/starter/probe/Lens.java`
- Create: `spring-lens-starter/src/main/java/io/springlens/starter/probe/LensProbeRegistry.java`
- Create: `spring-lens-starter/src/main/java/io/springlens/starter/probe/LensProjectToolRegistry.java`
- Create: `spring-lens-starter/src/main/java/io/springlens/starter/probe/LensValueSanitizer.java`

**Step 1: Write the failing test**

Add a demo integration test that expects probe and project-tool descriptors to be queryable through the internal API.

**Step 2: Run test to verify it fails**

Run: `mvn -pl spring-lens-demo-app -am test -Dtest=SpringLensProbeIntegrationTests "-Dsurefire.failIfNoSpecifiedTests=false"`
Expected: FAIL because the new endpoints and registries do not exist.

### Task 3: Capture watchpoints and manual `Lens.look(...)` values into execution graphs

**Files:**
- Create: `spring-lens-starter/src/main/java/io/springlens/starter/probe/LensProbeAspect.java`
- Create: `spring-lens-starter/src/main/java/io/springlens/starter/probe/LensProbeCaptureService.java`
- Create: `spring-lens-runtime/src/main/java/io/springlens/runtime/ProbeValueCollector.java`
- Modify: `spring-lens-runtime/src/main/java/io/springlens/runtime/InMemoryExecutionGraphStore.java`
- Modify: `spring-lens-starter/src/main/java/io/springlens/starter/LensRuntimeAutoConfiguration.java`

**Step 1: Write the failing test**

Add a demo integration test that hits an endpoint using `@LensWatch` and `Lens.look(...)`, then asserts `/internal/spring-lens/probe-values` returns both values.

**Step 2: Run test to verify it fails**

Run: `mvn -pl spring-lens-demo-app -am test -Dtest=SpringLensProbeIntegrationTests "-Dsurefire.failIfNoSpecifiedTests=false"`
Expected: FAIL because probe capture is not implemented.

### Task 4: Expose starter-side probe and project-tool runtime APIs

**Files:**
- Modify: `spring-lens-starter/src/main/java/io/springlens/starter/RuntimeQueryController.java`
- Create: `spring-lens-starter/src/main/java/io/springlens/starter/ProjectToolInvocationRequest.java`

**Step 1: Write the failing test**

Extend the demo integration test to call:

- `GET /internal/spring-lens/probes`
- `GET /internal/spring-lens/probe-values`
- `GET /internal/spring-lens/project-tools`
- `POST /internal/spring-lens/project-tools/{toolName}:invoke`

**Step 2: Run test to verify it fails**

Run: `mvn -pl spring-lens-demo-app -am test -Dtest=SpringLensProbeIntegrationTests "-Dsurefire.failIfNoSpecifiedTests=false"`
Expected: FAIL with 404 or incorrect payloads.

### Task 5: Add server-side generic MCP tools for project-defined capabilities

**Files:**
- Modify: `spring-lens-server/src/main/java/io/springlens/server/runtime/RuntimeObservationClient.java`
- Modify: `spring-lens-server/src/main/java/io/springlens/server/runtime/HttpRuntimeObservationClient.java`
- Create: `spring-lens-server/src/main/java/io/springlens/server/tool/ListProjectToolsTool.java`
- Create: `spring-lens-server/src/main/java/io/springlens/server/tool/InvokeProjectToolTool.java`
- Create: `spring-lens-server/src/main/java/io/springlens/server/tool/QueryProbeValuesTool.java`
- Modify: `spring-lens-server/src/main/java/io/springlens/server/mcp/LensMcpTools.java`
- Create: `spring-lens-server/src/test/java/io/springlens/server/tool/ProjectToolGatewayTest.java`

**Step 1: Write the failing test**

Write a server-side unit test that invokes the new generic tools against a fake runtime client.

**Step 2: Run test to verify it fails**

Run: `mvn -pl spring-lens-server -am test -Dtest=ProjectToolGatewayTest "-Dsurefire.failIfNoSpecifiedTests=false"`
Expected: FAIL because the new tools and runtime client methods are missing.

### Task 6: Add demo application code that exercises the new programming model

**Files:**
- Modify: `spring-lens-demo-app/src/main/java/io/springlens/demo/OrderController.java`
- Create: `spring-lens-demo-app/src/main/java/io/springlens/demo/OrderProbeService.java`
- Create: `spring-lens-demo-app/src/test/java/io/springlens/demo/SpringLensProbeIntegrationTests.java`

**Step 1: Write the failing test**

Create tests that prove:

- `@LensWatch` captures method return values
- `Lens.look(...)` captures local values
- `@LensTool` is discoverable and invokable

**Step 2: Run test to verify it fails**

Run: `mvn -pl spring-lens-demo-app -am test -Dtest=SpringLensProbeIntegrationTests "-Dsurefire.failIfNoSpecifiedTests=false"`
Expected: FAIL until the programming model is wired end-to-end.

### Task 7: Verify the full reactor and update documentation

**Files:**
- Modify: `README.md`

**Step 1: Write the failing test**

Use the full reactor test command as the final gate.

**Step 2: Run test to verify it fails**

Run: `mvn test`
Expected: FAIL until all second-stage wiring is complete.

**Step 3: Write minimal implementation**

Document:

- `@LensWatch`
- `Lens.look(...)`
- `@LensTool`
- internal runtime endpoints
- MCP generic tools

**Step 4: Run test to verify it passes**

Run: `mvn test`
Expected: PASS
