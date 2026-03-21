# Spring Lens MVP Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a runnable multi-module Spring project that captures HTTP requests, exceptions, and JDBC slow SQL into execution graphs, then exposes AI-facing MCP tools through an external server.

**Architecture:** The runtime starter assembles structured execution graphs inside each application instance and exposes a narrow runtime query API. The external server handles application registration, tool routing, MCP exposure, and diagnostic playbook discovery.

**Tech Stack:** Java 21, Spring Boot 4.0.3, Spring AI 1.1.3 MCP server starter, Spring MVC, Spring JDBC, H2, JUnit 5

---

### Task 1: Scaffold the multi-module Maven build

**Files:**
- Create: `pom.xml`
- Create: `.gitignore`
- Create: `spring-lens-model/pom.xml`
- Create: `spring-lens-spi/pom.xml`
- Create: `spring-lens-runtime/pom.xml`
- Create: `spring-lens-starter/pom.xml`
- Create: `spring-lens-server/pom.xml`
- Create: `spring-lens-demo-app/pom.xml`

**Step 1: Write the failing test**

Create module-level tests that reference planned runtime and server classes before those classes exist.

**Step 2: Run test to verify it fails**

Run: `mvn test`
Expected: FAIL with compilation errors for missing runtime and server classes.

**Step 3: Write minimal implementation**

Add the parent POM and child module POMs with shared dependency management.

### Task 2: Define the structured runtime model and SPI

**Files:**
- Create: `spring-lens-model/src/main/java/io/springlens/model/...`
- Create: `spring-lens-spi/src/main/java/io/springlens/spi/...`

**Step 1: Write the failing test**

Reference `RuntimeSignalType`, `RuntimeCollector`, and `ExecutionGraph` from runtime tests.

**Step 2: Run test to verify it fails**

Run: `mvn -pl spring-lens-runtime test`
Expected: FAIL with missing model and SPI symbols.

### Task 3: Implement in-memory runtime graph assembly

**Files:**
- Create: `spring-lens-runtime/src/main/java/io/springlens/runtime/...`
- Create: `spring-lens-runtime/src/test/java/io/springlens/runtime/RuntimeSignalProcessorTest.java`

**Step 1: Write the failing test**

Create a test that emits request, SQL, exception, and completion signals and asserts the completed graph queries.

### Task 4: Expose runtime capture in a Spring Boot starter

**Files:**
- Create: `spring-lens-starter/src/main/java/io/springlens/starter/...`
- Create: `spring-lens-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

### Task 5: Implement the MCP server and diagnostic playbooks

**Files:**
- Create: `spring-lens-server/src/main/java/io/springlens/server/...`
- Create: `spring-lens-server/src/test/java/io/springlens/server/tool/ToolRouterTest.java`

### Task 6: Add the runnable demo app and seed data

**Files:**
- Create: `spring-lens-demo-app/src/main/java/io/springlens/demo/...`
- Create: `spring-lens-demo-app/src/test/java/io/springlens/demo/SpringLensDemoApplicationTests.java`

### Task 7: Verify the whole reactor and document how to run it

**Files:**
- Create: `README.md`

The workspace is not currently a Git repository, so the commit commands in the original workflow cannot be executed unless the repository is initialized first.
