# Demo Runtime Console Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a static demo runtime console at `/lens/` inside `spring-lens-demo-app` so users can trigger implemented features and inspect the structured runtime evidence they produce.

**Architecture:** The demo app serves a static HTML/CSS/JS page from `src/main/resources/static/lens` and adds a tiny MVC forward so `/lens/` resolves to the static entry point. The page calls only existing demo-app business and runtime APIs, then renders explanatory capability cards plus structured result panels for SQL, exceptions, probes, tools, and execution graphs.

**Tech Stack:** Java 21, Spring Boot 4.0.3, Spring MVC, static resources, vanilla JavaScript, JUnit 5, RestClient

---

### Task 1: Add the failing entry-point test for the frontend

**Files:**
- Create: `spring-lens-demo-app/src/test/java/io/springlens/demo/SpringLensConsolePageIntegrationTests.java`

**Step 1: Write the failing test**

Write a Spring Boot integration test that requests `/lens/` and asserts the HTML contains:

- `Spring Lens`
- `AI Runtime Operating System`
- `Trigger Slow SQL`
- `Invoke Project Tool`

**Step 2: Run test to verify it fails**

Run: `mvn -pl spring-lens-demo-app -am -Dtest=SpringLensConsolePageIntegrationTests "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: FAIL with `404`, redirect mismatch, or missing HTML content.

### Task 2: Add the frontend entry-point forwarding

**Files:**
- Create: `spring-lens-demo-app/src/main/java/io/springlens/demo/LensConsoleController.java`

**Step 1: Write minimal implementation**

Add a controller that forwards:

- `GET /lens`
- `GET /lens/`

to:

- `forward:/lens/index.html`

**Step 2: Run the test again**

Run: `mvn -pl spring-lens-demo-app -am -Dtest=SpringLensConsolePageIntegrationTests "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: FAIL because the static page files still do not exist or do not contain the required content.

### Task 3: Build the static HTML shell

**Files:**
- Create: `spring-lens-demo-app/src/main/resources/static/lens/index.html`

**Step 1: Write minimal implementation**

Create the page structure with:

- hero section
- implemented-capabilities section
- AI-facing-capabilities section
- action console section
- result panel grid
- execution graph section
- explanatory footer section

Include placeholders for:

- system status
- last action
- last graph id
- business result
- slow SQL
- exception context
- probes
- project tools
- execution graph

Include script and stylesheet links for:

- `/lens/lens.css`
- `/lens/lens.js`

**Step 2: Run the test again**

Run: `mvn -pl spring-lens-demo-app -am -Dtest=SpringLensConsolePageIntegrationTests "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: FAIL until the required text strings are present.

### Task 4: Add the page styling

**Files:**
- Create: `spring-lens-demo-app/src/main/resources/static/lens/lens.css`

**Step 1: Write minimal implementation**

Add the visual system:

- CSS variables for background, panels, accents, success, warning, and danger
- hero styling
- responsive grid cards
- monospace result blocks
- action button styles
- badges and state pills

**Step 2: Manual visual goal**

Ensure the page reads like a runtime lab rather than a Swagger sheet or a monitoring dashboard.

### Task 5: Add the frontend interaction logic

**Files:**
- Create: `spring-lens-demo-app/src/main/resources/static/lens/lens.js`

**Step 1: Write the state and rendering helpers**

Add a small state object for:

- `health`
- `lastAction`
- `lastGraphId`
- `businessResponse`
- `slowSql`
- `exceptionContext`
- `probes`
- `probeValues`
- `projectTools`
- `toolResult`
- `executionGraph`
- `errors`

Add rendering helpers for:

- status badges
- formatted JSON blocks
- empty states
- graph node and edge summaries

**Step 2: Implement action handlers**

Add button handlers for:

- `Load Order`
- `Trigger Slow SQL`
- `Trigger Exception`
- `Trigger Probe`
- `Invoke Project Tool`

Each handler should call the relevant follow-up runtime endpoints and update the state.

**Step 3: Run the entry-point test**

Run: `mvn -pl spring-lens-demo-app -am -Dtest=SpringLensConsolePageIntegrationTests "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: PASS

### Task 6: Verify the demo application still passes its backend tests

**Files:**
- No new files

**Step 1: Run demo-app focused tests**

Run: `mvn -pl spring-lens-demo-app -am test`
Expected: PASS

**Step 2: Manually exercise the page**

Open:

- `http://localhost:8081/lens/`

Then click:

- `Trigger Slow SQL`
- `Trigger Exception`
- `Trigger Probe`
- `Invoke Project Tool`

Confirm the panels update and the execution graph section reflects the latest `graphId`.

### Task 7: Verify the full reactor cleanly

**Files:**
- Optionally modify: `README.md` only if the frontend route needs documentation

**Step 1: Run the full clean verification**

Run: `mvn clean test`
Expected: PASS

**Step 2: Document only if needed**

If the route is user-facing enough to deserve README coverage, add a short note about:

- `/lens/`
- its purpose as a demo runtime console
- its dependence on the demo app only
