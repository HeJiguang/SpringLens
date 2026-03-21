package io.springlens.server.diagnostic;

import io.springlens.spi.DefaultDiagnosticEngine;
import io.springlens.spi.DiagnosticEngine;
import io.springlens.spi.DiagnosticEngineSelectionStrategy;
import io.springlens.spi.PriorityDiagnosticEngineSelectionStrategy;
import io.springlens.spi.RoutingDiagnosticEngine;
import io.springlens.spi.SelectableDiagnosticEngine;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DiagnosticEngineConfiguration {

    @Bean
    @ConditionalOnMissingBean(DefaultDiagnosticEngine.class)
    public SelectableDiagnosticEngine defaultRuleBasedDiagnosticEngine() {
        return new DefaultDiagnosticEngine();
    }

    @Bean
    @ConditionalOnMissingBean(DiagnosticEngineSelectionStrategy.class)
    public DiagnosticEngineSelectionStrategy diagnosticEngineSelectionStrategy() {
        return new PriorityDiagnosticEngineSelectionStrategy();
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(DiagnosticEngine.class)
    public DiagnosticEngine diagnosticEngine(
            List<SelectableDiagnosticEngine> diagnosticEngines,
            DiagnosticEngineSelectionStrategy selectionStrategy
    ) {
        return new RoutingDiagnosticEngine(diagnosticEngines, selectionStrategy);
    }
}
