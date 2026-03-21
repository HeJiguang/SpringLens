package io.springlens.demo;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = SpringLensDemoApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.lens.registration-enabled=false"
)
class SpringLensConsolePageIntegrationTests {

    @LocalServerPort
    private int port;

    @Test
    void servesDemoRuntimeConsolePage() {
        String html = RestClient.create()
                .get()
                .uri("http://localhost:" + port + "/lens/")
                .retrieve()
                .body(String.class);

        assertThat(html).contains("Spring Lens");
        assertThat(html).contains("lens.js");
        assertThat(html).contains("Refresh Health");
        assertThat(html).contains("Runtime Base");
    }

    @Test
    void servesLensScriptWithUtf8ChineseTranslations() {
        byte[] script = RestClient.create()
                .get()
                .uri("http://localhost:" + port + "/lens/lens.js")
                .retrieve()
                .body(byte[].class);

        assertThat(new String(script, StandardCharsets.UTF_8))
                .contains("Spring Lens \u8fd0\u884c\u65f6\u63a7\u5236\u53f0")
                .contains("\u5f53\u524d\u6f14\u793a\u4e0a\u4e0b\u6587")
                .contains("\u6267\u884c\u56fe");
    }

    @Test
    void servesLensScriptWithCorrectManualProbeEndpoint() {
        String script = RestClient.create()
                .get()
                .uri("http://localhost:" + port + "/lens/lens.js")
                .retrieve()
                .body(String.class);

        assertThat(script)
                .contains("probeId=order.status&limit=5")
                .doesNotContain("probeId=order.local.status&limit=5");
    }
}
