package io.springlens.server.playbook;

import io.springlens.spi.DiagnosticPlaybook;
import io.springlens.spi.PlaybookStep;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ExceptionPlaybook implements DiagnosticPlaybook {

    @Override
    public String id() {
        return "exception";
    }

    @Override
    public String problem() {
        return "Investigate runtime exceptions with surrounding request and SQL context.";
    }

    @Override
    public List<PlaybookStep> steps() {
        return List.of(
                new PlaybookStep(1, "Find recent exceptions", "get_exception_context", "Retrieve recent exception snapshots for the target application."),
                new PlaybookStep(2, "Open execution graph", "get_execution_graph", "Inspect the full request graph that led to the exception."),
                new PlaybookStep(3, "Check slow SQL correlation", "get_slow_sql", "Validate whether query latency contributed to the exception path.")
        );
    }
}
