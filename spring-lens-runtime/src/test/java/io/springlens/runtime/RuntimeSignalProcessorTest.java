package io.springlens.runtime;

import io.springlens.model.core.ExecutionContext;
import io.springlens.spi.RuntimeSignal;
import io.springlens.spi.RuntimeSignalType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeSignalProcessorTest {

    @Test
    void capturesSlowSqlAndExceptionIntoCompletedGraph() {
        InMemoryExecutionGraphStore store = new InMemoryExecutionGraphStore(32);
        RuntimeSignalProcessor processor = new RuntimeSignalProcessor(
                store,
                List.of(new HttpRequestCollector(), new JdbcSlowSqlCollector(), new ExceptionCollector())
        );

        ExecutionContext context = new ExecutionContext(
                "orders-app",
                "orders-app-1",
                "request-1",
                Instant.parse("2026-03-19T10:15:30Z"),
                Map.of("path", "/orders/slow")
        );

        processor.start(context);
        processor.process(new RuntimeSignal(
                "request-1",
                RuntimeSignalType.HTTP_REQUEST_STARTED,
                Instant.parse("2026-03-19T10:15:30Z"),
                Map.of("method", "GET", "path", "/orders/slow")
        ));
        processor.process(new RuntimeSignal(
                "request-1",
                RuntimeSignalType.JDBC_EXECUTED,
                Instant.parse("2026-03-19T10:15:31Z"),
                Map.of("sql", "select * from orders", "durationMs", 240L, "slow", true)
        ));
        processor.process(new RuntimeSignal(
                "request-1",
                RuntimeSignalType.EXCEPTION_CAPTURED,
                Instant.parse("2026-03-19T10:15:31Z"),
                Map.of(
                        "exceptionClass",
                        "java.lang.IllegalStateException",
                        "message",
                        "boom",
                        "stackTrace",
                        List.of("IllegalStateException: boom")
                )
        ));
        processor.process(new RuntimeSignal(
                "request-1",
                RuntimeSignalType.HTTP_REQUEST_COMPLETED,
                Instant.parse("2026-03-19T10:15:31Z"),
                Map.of("status", 500, "durationMs", 1000L)
        ));

        assertThat(store.findSlowSql("orders-app", 10, 100))
                .singleElement()
                .satisfies(record -> {
                    assertThat(record.sql()).isEqualTo("select * from orders");
                    assertThat(record.durationMs()).isEqualTo(240L);
                    assertThat(record.requestPath()).isEqualTo("/orders/slow");
                });

        assertThat(store.findExceptionContexts("orders-app", 10, null))
                .singleElement()
                .satisfies(record -> {
                    assertThat(record.exceptionClass()).isEqualTo("java.lang.IllegalStateException");
                    assertThat(record.message()).isEqualTo("boom");
                    assertThat(record.requestPath()).isEqualTo("/orders/slow");
                });
    }
}
