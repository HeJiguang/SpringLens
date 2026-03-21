# Capability Architecture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor Spring Lens so capability is the single plugin/discovery concept, tools remain the AI-facing callable surface, probes remain runtime semantic signals, and all legacy project-tool terminology is removed.

**Architecture:** Introduce a shared capability SPI, make the starter aggregate built-in and user-defined capabilities through one registry, replace runtime transport DTOs and HTTP endpoints with capability/tool vocabulary, and unify the server around the same tool contract before updating the demo app and docs. This is an internal reset, so historical project-tool APIs are removed instead of preserved.

**Tech Stack:** Java 21, Spring Boot 4.0.3, Spring MVC, Spring AOP, Spring AI MCP server starter, Jackson, JUnit 5, AssertJ, Maven 3.9.11

---

### Task 1: Introduce the shared capability SPI

**Files:**
- Create: `spring-lens-spi/src/main/java/io/springlens/spi/LensCapability.java`
- Create: `spring-lens-spi/src/main/java/io/springlens/spi/CapabilityDescriptor.java`
- Create: `spring-lens-spi/src/main/java/io/springlens/spi/CapabilityContribution.java`
- Create: `spring-lens-spi/src/main/java/io/springlens/spi/CapabilityKind.java`
- Create: `spring-lens-spi/src/main/java/io/springlens/spi/CapabilitySource.java`
- Create: `spring-lens-spi/src/main/java/io/springlens/spi/LensCallableTool.java`
- Create: `spring-lens-spi/src/main/java/io/springlens/spi/CapabilityToolGenerator.java`
- Create: `spring-lens-spi/src/test/java/io/springlens/spi/LensCapabilityContractTest.java`
- Delete: `spring-lens-spi/src/main/java/io/springlens/spi/GeneratedSkillTool.java`
- Delete: `spring-lens-spi/src/main/java/io/springlens/spi/SkillGenerator.java`
- Delete: `spring-lens-spi/src/main/java/io/springlens/spi/SkillGenerationRequest.java`
- Modify: `spring-lens-spi/src/main/java/io/springlens/spi/DiagnosticTool.java`
- Modify: `spring-lens-spi/src/main/java/io/springlens/spi/ToolDescriptor.java`
- Modify: `spring-lens-spi/src/main/java/io/springlens/spi/ToolMetadata.java`
- Modify: `spring-lens-spi/src/main/java/io/springlens/spi/ToolRequest.java`
- Modify: `spring-lens-spi/src/main/java/io/springlens/spi/ToolSchema.java`

- [ ] **Step 1: Write the failing SPI contract test**

```java
@Test
void capabilityExposesDescriptorToolsAndProbesThroughOneContract() {
    LensCallableTool tool = new SampleTool();
    LensCapability capability = new SampleCapability(tool);

    CapabilityContribution contribution = capability.contribute();

    assertThat(contribution.descriptor().id()).isEqualTo("demo.orders");
    assertThat(contribution.tools()).extracting(toolItem -> toolItem.metadata().name())
            .contains("count_orders");
    assertThat(contribution.probes()).extracting(ProbeDescriptor::probeId)
            .contains("order.status");
}
```

- [ ] **Step 2: Run the SPI test to verify it fails**

Run: `mvn -pl spring-lens-spi -am test -Dtest=LensCapabilityContractTest "-Dsurefire.failIfNoSpecifiedTests=false"`

Expected: FAIL because the capability types do not exist.

- [ ] **Step 3: Write the minimal SPI implementation**

```java
public interface LensCapability {

    CapabilityContribution contribute();
}
```

```java
public record CapabilityContribution(
        CapabilityDescriptor descriptor,
        List<ProbeDescriptor> probes,
        List<LensCallableTool> tools
) {
}
```

- [ ] **Step 4: Run the SPI test to verify it passes**

Run: `mvn -pl spring-lens-spi -am test -Dtest=LensCapabilityContractTest "-Dsurefire.failIfNoSpecifiedTests=false"`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add spring-lens-spi
git commit -m "refactor: introduce capability spi"
```

### Task 2: Replace runtime-facing project-tool DTOs with capability/tool DTOs

**Files:**
- Create: `spring-lens-model/src/main/java/io/springlens/model/RuntimeCapabilityDescriptor.java`
- Create: `spring-lens-model/src/main/java/io/springlens/model/RuntimeToolDescriptor.java`
- Create: `spring-lens-model/src/main/java/io/springlens/model/RuntimeToolSchemaDescriptor.java`
- Create: `spring-lens-model/src/test/java/io/springlens/model/RuntimeToolSchemaDescriptorTest.java`
- Delete: `spring-lens-model/src/main/java/io/springlens/model/ProjectToolDescriptor.java`
- Delete: `spring-lens-model/src/main/java/io/springlens/model/ProjectToolSchemaDescriptor.java`
- Delete: `spring-lens-model/src/main/java/io/springlens/model/ProjectToolSourceType.java`

- [ ] **Step 1: Write the failing model test**

```java
@Test
void runtimeToolSchemaDescriptorDefensivelyCopiesNestedSchemaMaps() {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", "object");

    RuntimeToolSchemaDescriptor descriptor = new RuntimeToolSchemaDescriptor(
            "count_orders",
            "Count orders.",
            schema,
            schema,
            "demo.orders"
    );

    assertThatThrownBy(() -> descriptor.inputSchema().put("x", "y"))
            .isInstanceOf(UnsupportedOperationException.class);
}
```

- [ ] **Step 2: Run the model test to verify it fails**

Run: `mvn -pl spring-lens-model -am test -Dtest=RuntimeToolSchemaDescriptorTest "-Dsurefire.failIfNoSpecifiedTests=false"`

Expected: FAIL because the runtime DTOs do not exist.

- [ ] **Step 3: Write the minimal runtime DTO implementation**

```java
public record RuntimeToolDescriptor(
        String name,
        String description,
        String capabilityId
) {
}
```

- [ ] **Step 4: Run the model test to verify it passes**

Run: `mvn -pl spring-lens-model -am test -Dtest=RuntimeToolSchemaDescriptorTest "-Dsurefire.failIfNoSpecifiedTests=false"`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add spring-lens-model
git commit -m "refactor: replace project tool dto vocabulary"
```

### Task 3: Build a starter-side capability registry and first-party capabilities

**Files:**
- Create: `spring-lens-starter/src/main/java/io/springlens/starter/capability/LensCapabilityRegistry.java`
- Create: `spring-lens-starter/src/main/java/io/springlens/starter/capability/AnnotationToolCapability.java`
- Create: `spring-lens-starter/src/main/java/io/springlens/starter/capability/GeneratedToolCapability.java`
- Create: `spring-lens-starter/src/main/java/io/springlens/starter/capability/RequestDiagnosisCapability.java`
- Create: `spring-lens-starter/src/main/java/io/springlens/starter/capability/DefaultCapabilityToolGenerator.java`
- Create: `spring-lens-starter/src/test/java/io/springlens/starter/capability/LensCapabilityRegistryTest.java`
- Delete: `spring-lens-starter/src/main/java/io/springlens/starter/probe/LensProjectToolRegistry.java`
- Delete: `spring-lens-starter/src/main/java/io/springlens/starter/probe/LensDiagnosticTool.java`
- Delete: `spring-lens-starter/src/main/java/io/springlens/starter/probe/DefaultSkillGenerator.java`
- Modify: `spring-lens-starter/src/main/java/io/springlens/starter/LensRuntimeAutoConfiguration.java`
- Modify: `spring-lens-starter/src/main/java/io/springlens/starter/probe/ProjectToolJsonSchemas.java`
- Modify: `spring-lens-starter/src/main/java/io/springlens/starter/probe/ProjectToolSchemaFactory.java`

- [ ] **Step 1: Write the failing starter registry test**

```java
@Test
void registryAggregatesBuiltInAndUserDefinedCapabilities() {
    LensCapabilityRegistry registry = new LensCapabilityRegistry(List.of(
            new RequestDiagnosisCapability(...),
            new StubUserCapability()
    ));

    assertThat(registry.capabilities()).extracting(item -> item.id())
            .contains("spring-lens.diagnosis", "demo.orders");
    assertThat(registry.tools()).extracting(RuntimeToolDescriptor::name)
            .contains("diagnose_request", "count_orders");
}
```

- [ ] **Step 2: Run the starter test to verify it fails**

Run: `mvn -pl spring-lens-starter -am test -Dtest=LensCapabilityRegistryTest "-Dsurefire.failIfNoSpecifiedTests=false"`

Expected: FAIL because the registry and capability classes do not exist.

- [ ] **Step 3: Write the minimal starter capability implementation**

```java
public final class LensCapabilityRegistry {

    private final List<LensCapability> capabilities;

    public LensCapabilityRegistry(List<LensCapability> capabilities) {
        this.capabilities = List.copyOf(capabilities);
    }
}
```

- [ ] **Step 4: Run the starter test to verify it passes**

Run: `mvn -pl spring-lens-starter -am test -Dtest=LensCapabilityRegistryTest "-Dsurefire.failIfNoSpecifiedTests=false"`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add spring-lens-starter
git commit -m "refactor: build starter capability registry"
```

### Task 4: Replace starter runtime endpoints with capability/tool vocabulary

**Files:**
- Create: `spring-lens-starter/src/main/java/io/springlens/starter/ToolInvocationRequest.java`
- Modify: `spring-lens-starter/src/main/java/io/springlens/starter/RuntimeQueryController.java`
- Modify: `spring-lens-starter/src/test/java/io/springlens/starter/probe/ProjectToolSchemaFactoryTest.java`
- Delete: `spring-lens-starter/src/main/java/io/springlens/starter/ProjectToolInvocationRequest.java`

- [ ] **Step 1: Write the failing runtime API integration test**

```java
@Test
void exposesCapabilityAndToolCatalogsThroughRuntimeApi() {
    List<RuntimeCapabilityDescriptor> capabilities = restClient().get()
            .uri(baseUrl + "/internal/spring-lens/capabilities")
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

    List<RuntimeToolDescriptor> tools = restClient().get()
            .uri(baseUrl + "/internal/spring-lens/tools")
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

    assertThat(capabilities).extracting(RuntimeCapabilityDescriptor::id)
            .contains("demo.orders");
    assertThat(tools).extracting(RuntimeToolDescriptor::name)
            .contains("count_orders");
}
```

- [ ] **Step 2: Run the runtime API test to verify it fails**

Run: `mvn -pl spring-lens-demo-app -am test -Dtest=SpringLensCapabilityIntegrationTests "-Dsurefire.failIfNoSpecifiedTests=false"`

Expected: FAIL with 404 or missing payload types because the new endpoints do not exist.

- [ ] **Step 3: Write the minimal runtime API implementation**

```java
@GetMapping("/capabilities")
public List<RuntimeCapabilityDescriptor> getCapabilities() {
    return capabilityRegistry.capabilities();
}
```

- [ ] **Step 4: Run the runtime API test to verify it passes**

Run: `mvn -pl spring-lens-demo-app -am test -Dtest=SpringLensCapabilityIntegrationTests "-Dsurefire.failIfNoSpecifiedTests=false"`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add spring-lens-starter spring-lens-demo-app
git commit -m "refactor: expose capability runtime api"
```

### Task 5: Refactor the server around runtime tool vocabulary

**Files:**
- Create: `spring-lens-server/src/main/java/io/springlens/server/tool/ListRuntimeToolsTool.java`
- Create: `spring-lens-server/src/main/java/io/springlens/server/tool/InvokeRuntimeToolTool.java`
- Create: `spring-lens-server/src/test/java/io/springlens/server/tool/RuntimeToolGatewayTest.java`
- Delete: `spring-lens-server/src/main/java/io/springlens/server/tool/ListProjectToolsTool.java`
- Delete: `spring-lens-server/src/main/java/io/springlens/server/tool/InvokeProjectToolTool.java`
- Modify: `spring-lens-server/src/main/java/io/springlens/server/runtime/RuntimeObservationClient.java`
- Modify: `spring-lens-server/src/main/java/io/springlens/server/runtime/HttpRuntimeObservationClient.java`
- Modify: `spring-lens-server/src/main/java/io/springlens/server/tool/ToolRegistry.java`
- Modify: `spring-lens-server/src/main/java/io/springlens/server/tool/QueryProbeValuesTool.java`
- Modify: `spring-lens-server/src/main/java/io/springlens/server/mcp/LensMcpTools.java`
- Modify: `spring-lens-server/src/main/java/io/springlens/server/mcp/http/McpToolController.java`
- Modify: `spring-lens-server/src/test/java/io/springlens/server/tool/ToolRegistryExposureTest.java`
- Delete: `spring-lens-server/src/test/java/io/springlens/server/tool/ProjectToolGatewayTest.java`

- [ ] **Step 1: Write the failing server gateway test**

```java
@Test
void listRuntimeToolsDelegatesToRuntimeClientUsingNewVocabulary() {
    FakeRuntimeObservationClient client = new FakeRuntimeObservationClient();
    ListRuntimeToolsTool tool = new ListRuntimeToolsTool(registryService, client);

    Object result = tool.execute(new ToolRequest("orders-app", null, Map.of()));

    assertThat(result).asList().extracting("name")
            .contains("count_orders");
    assertThat(client.listRuntimeToolsCalled()).isTrue();
}
```

- [ ] **Step 2: Run the server test to verify it fails**

Run: `mvn -pl spring-lens-server -am test -Dtest=RuntimeToolGatewayTest "-Dsurefire.failIfNoSpecifiedTests=false"`

Expected: FAIL because the new gateway tools and client methods do not exist.

- [ ] **Step 3: Write the minimal server refactor**

```java
public interface RuntimeObservationClient {

    List<RuntimeToolDescriptor> listRuntimeTools(AppRegistration registration);
}
```

- [ ] **Step 4: Run the server test to verify it passes**

Run: `mvn -pl spring-lens-server -am test -Dtest=RuntimeToolGatewayTest "-Dsurefire.failIfNoSpecifiedTests=false"`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add spring-lens-server
git commit -m "refactor: rename server runtime tool gateway"
```

### Task 6: Add a demo user-defined capability and end-to-end tests

**Files:**
- Create: `spring-lens-demo-app/src/main/java/io/springlens/demo/OrderCapability.java`
- Create: `spring-lens-demo-app/src/test/java/io/springlens/demo/SpringLensCapabilityIntegrationTests.java`
- Modify: `spring-lens-demo-app/src/main/java/io/springlens/demo/OrderProbeService.java`
- Modify: `spring-lens-demo-app/src/main/java/io/springlens/demo/OrderController.java`
- Delete: `spring-lens-demo-app/src/test/java/io/springlens/demo/SpringLensProbeIntegrationTests.java`

- [ ] **Step 1: Write the failing demo integration test**

```java
@Test
void discoversUserDefinedCapabilityAndInvokesItsTool() {
    List<RuntimeCapabilityDescriptor> capabilities = restClient().get()
            .uri(baseUrl + "/internal/spring-lens/capabilities")
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

    Map<String, Object> result = restClient().post()
            .uri(baseUrl + "/internal/spring-lens/tools/count_orders_by_status:invoke")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("arguments", Map.of("status", "PAID")))
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

    assertThat(capabilities).extracting(RuntimeCapabilityDescriptor::id)
            .contains("demo.orders");
    assertThat(result).containsEntry("count", 1);
}
```

- [ ] **Step 2: Run the demo test to verify it fails**

Run: `mvn -pl spring-lens-demo-app -am test -Dtest=SpringLensCapabilityIntegrationTests "-Dsurefire.failIfNoSpecifiedTests=false"`

Expected: FAIL because the demo app does not yet define a capability bean or use the new runtime endpoints.

- [ ] **Step 3: Write the minimal demo capability implementation**

```java
@Component
public final class OrderCapability implements LensCapability {

    @Override
    public CapabilityContribution contribute() {
        return new CapabilityContribution(
                new CapabilityDescriptor("demo.orders", "Order Capability", "Order runtime insights", ...),
                List.of(...),
                List.of(...)
        );
    }
}
```

- [ ] **Step 4: Run the demo test to verify it passes**

Run: `mvn -pl spring-lens-demo-app -am test -Dtest=SpringLensCapabilityIntegrationTests "-Dsurefire.failIfNoSpecifiedTests=false"`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add spring-lens-demo-app
git commit -m "refactor: demonstrate user-defined capability"
```

### Task 7: Delete legacy naming, refresh docs, and run the full reactor

**Files:**
- Modify: `README.md`
- Modify: `docs/plans/2026-03-21-capability-architecture-design.md`
- Modify: `docs/plans/2026-03-21-capability-architecture-implementation.md`
- Delete: every remaining source file or test that still uses `project tool` or `generated skill` terminology after the refactor

- [ ] **Step 1: Write the failing final verification target**

Run: `mvn test`

Expected: FAIL until all legacy references are removed and all modules compile against the new vocabulary.

- [ ] **Step 2: Remove the last legacy terminology and update docs**

```markdown
- replace `project tool` with `runtime tool`
- replace `generated skill` with `generated capability`
- document `/internal/spring-lens/capabilities`
- document `list_runtime_tools`
```

- [ ] **Step 3: Run the full reactor to verify it passes**

Run: `mvn test`

Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add README.md docs/plans spring-lens-model spring-lens-spi spring-lens-starter spring-lens-server spring-lens-demo-app
git commit -m "refactor: unify spring lens around capabilities"
```
