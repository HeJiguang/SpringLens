package io.springlens.agent.starter;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.springlens.starter.LensRuntimeProperties;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class AgentOverlayControlClientTest {

    private HttpServer httpServer;

    @AfterEach
    void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @Test
    void fetchesOverlayDeliverySnapshotFromServer() throws Exception {
        AtomicReference<String> recordedQuery = new AtomicReference<>();
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/internal/apps/orders-app/agent-overlays", exchange -> {
            recordedQuery.set(exchange.getRequestURI().getQuery());
            writeJson(exchange, """
                    {
                      "applicationId": "orders-app",
                      "instanceId": "orders-1",
                      "policy": {
                        "mode": "HYBRID_APPROVAL",
                        "sourceEditEnabled": true,
                        "sourceEditAutoApply": false,
                        "approvalRequired": true,
                        "metadata": {
                          "plane": "agent-control"
                        }
                      },
                      "activeOverlays": [
                        {
                          "overlayId": "ovl-orders-default",
                          "spec": {
                            "overlayId": "ovl-orders-default",
                            "mode": "HYBRID_APPROVAL",
                            "riskLevel": "MEDIUM",
                            "enabled": true,
                            "ttl": "PT4H",
                            "selectorType": "spring-bean-method",
                            "targetClassName": "com.example.order.OrderService",
                            "targetMethodName": "submitOrder",
                            "capturePhase": "AFTER_RETURN",
                            "probeId": "order.submit.status",
                            "expression": "result.status",
                            "description": "Capture status",
                            "tags": ["orders"],
                            "metadata": {
                              "applicationId": "orders-app"
                            }
                          },
                          "approvalState": "PENDING",
                          "createdAt": "2026-03-31T16:40:00Z",
                          "disabledAt": null,
                          "metadata": {}
                        }
                      ],
                      "deliveredAt": "2026-03-31T16:40:05Z"
                    }
                    """);
        });
        httpServer.start();

        AgentOverlayControlClient client = new AgentOverlayControlClient(
                RestClient.builder(),
                runtimeProperties(httpServer.getAddress().getPort()),
                agentProperties()
        );

        var snapshot = client.fetchSnapshot();

        assertThat(recordedQuery.get()).isEqualTo("instanceId=orders-1");
        assertThat(snapshot.applicationId()).isEqualTo("orders-app");
        assertThat(snapshot.activeOverlays()).hasSize(1);
        assertThat(snapshot.activeOverlays().get(0).overlayId()).isEqualTo("ovl-orders-default");
    }

    private LensRuntimeProperties runtimeProperties(int port) {
        LensRuntimeProperties properties = new LensRuntimeProperties();
        properties.setApplicationId("orders-app");
        properties.setInstanceId("orders-1");
        properties.setServerUrl("http://localhost:" + port);
        return properties;
    }

    private AgentInstrumentationProperties agentProperties() {
        AgentInstrumentationProperties properties = new AgentInstrumentationProperties();
        properties.setOverlayPullPath("/internal/apps/{applicationId}/agent-overlays");
        return properties;
    }

    private void writeJson(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}
