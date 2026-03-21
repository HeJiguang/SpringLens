package io.springlens.server;

import io.springlens.spi.DiagnosticEngine;
import io.springlens.spi.RoutingDiagnosticEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = SpringLensServerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class SpringLensServerApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DiagnosticEngine diagnosticEngine;

    @Test
    void startsServerContext() {
        assertThat(port).isPositive();
        assertThat(applicationContext.containsBean("httpRuntimeObservationClient")).isTrue();
        assertThat(diagnosticEngine).isInstanceOf(RoutingDiagnosticEngine.class);
    }
}
