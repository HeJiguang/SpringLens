package io.springlens.spi;

import io.springlens.model.core.ExecutionGraph;
import io.springlens.model.diagnostic.DiagnosticResult;

/**
 * Contract for components that analyze an execution graph and produce a diagnosis.
 */
public interface DiagnosticEngine {

    DiagnosticResult analyze(ExecutionGraph graph);
}
