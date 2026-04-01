package io.springlens.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = SpringLensDemoApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.lens.registration-enabled=true",
                "spring.lens.server-url=http://localhost:8090",
                "spring.lens.runtime-base-url=http://localhost:8081"
        }
)
class SpringLensRegistrationIntegrationTests {

    @LocalServerPort
    private int port;

    @Test
    void startsWhenRuntimeRegistrationIsEnabled() {
        String response = RestClient.create()
                .get()
                .uri("http://localhost:" + port + "/internal/spring-lens/tools")
                .retrieve()
                .body(String.class);

        assertThat(response).contains("count_orders_by_status");
    }
}
