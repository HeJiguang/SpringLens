# Contributing to Spring Lens

Spring Lens is an agent-first runtime tooling project for Spring Boot. Contributions are welcome, but they should preserve the core architecture:

- keep runtime, server, and control-plane concerns separated
- keep AI-facing APIs high-level and task-oriented
- prefer SPI boundaries over hardcoded branching
- add tests for every behavior change

## Before opening a PR

1. Open an issue for non-trivial changes.
2. Keep the scope focused. Mixed refactors and feature work are hard to review.
3. Update docs when public behavior, runtime tools, or control-plane APIs change.

## Local development

Requirements:

- Java 21+
- Maven 3.9+

Run the full suite:

```bash
mvn test
```

If you want an isolated repository-local Maven cache:

```bash
mvn -q "-Dmaven.repo.local=.m2-repo" test
```

## Pull request guidelines

- explain the problem first, then the implementation
- include tests for new behavior
- call out breaking changes explicitly
- avoid bypassing SPI layers or introducing business-specific assumptions into core modules

## Good first contribution areas

- runtime truth capture improvements
- agent-safe capabilities and MCP tools
- control-plane governance and audit improvements
- docs, demos, and onboarding polish

## Reporting bugs

Please include:

- module name
- Java version
- Maven version
- exact command or endpoint used
- stack trace or failing test
