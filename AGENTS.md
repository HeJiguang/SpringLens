# Spring Lens Agent Guide

## Project Goal
Spring Lens is an AI Runtime System for Spring Boot that exposes runtime behavior as AI-callable tools.

## Core Principles
- Everything is plugin-based (SPI first)
- Core must stay minimal
- Tools are task-oriented, not data-oriented
- AI-facing APIs must be high-level

## Architecture Rules
- Do NOT couple runtime with server
- Do NOT introduce business logic into core
- Prefer composition over inheritance

## Coding Style
- Java 17+
- Prefer immutability
- Clear naming over comments

## Important Concepts
- ExecutionGraph = runtime truth
- Probe = semantic signal
- LensTool = AI interface

## Anti-patterns
- Do NOT hardcode logic in controllers
- Do NOT bypass SPI layer
- Do NOT expose low-level APIs to AI

## Testing
- Always include unit tests for new features