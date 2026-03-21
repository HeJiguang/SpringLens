package io.springlens.demo;

import io.springlens.model.diagnostic.ExceptionContextRecord;
import io.springlens.model.diagnostic.SlowSqlRecord;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = SpringLensDemoApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.lens.registration-enabled=false"
)
class SpringLensDemoApplicationTests {

    @LocalServerPort
    private int port;

    @Test
    void exposesSlowSqlAfterSlowEndpointRuns() {
        restClient().get()
                .uri("http://localhost:" + port + "/orders/slow")
                .retrieve()
                .toBodilessEntity();

        List<SlowSqlRecord> response = restClient().get()
                .uri("http://localhost:" + port + "/internal/spring-lens/slow-sql?limit=5&minDurationMs=1")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        assertThat(response).isNotEmpty();
        assertThat(response.getFirst().requestPath()).isEqualTo("/orders/slow");
    }

    @Test
    void exposesExceptionContextAfterFailingEndpointRuns() {
        restClient().get()
                .uri("http://localhost:" + port + "/orders/fail")
                .exchange((request, response) -> response.getStatusCode());

        List<ExceptionContextRecord> response = restClient().get()
                .uri("http://localhost:" + port + "/internal/spring-lens/exception-context?limit=5")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        assertThat(response).isNotEmpty();
        assertThat(response.getFirst().exceptionClass()).isEqualTo("java.lang.IllegalStateException");
    }

    private RestClient restClient() {
        return RestClient.create();
    }
}
