package io.springlens.server.playbook;

import io.springlens.spi.DiagnosticPlaybook;
import io.springlens.spi.PlaybookStep;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SlowSqlPlaybook implements DiagnosticPlaybook {

    @Override
    public String id() {
        return "slow-sql";
    }

    @Override
    public String problem() {
        return "Investigate slow SQL inside recent HTTP request executions.";
    }

    @Override
    public List<PlaybookStep> steps() {
        return List.of(
                new PlaybookStep(1, "Locate recent slow SQL", "get_slow_sql", "Query recent slow SQL grouped by application."),
                new PlaybookStep(2, "Open execution graph", "get_execution_graph", "Inspect the request graph containing the slow query."),
                new PlaybookStep(3, "Correlate exception signals", "get_exception_context", "Check whether the same request also surfaced failures.")
        );
    }
}
