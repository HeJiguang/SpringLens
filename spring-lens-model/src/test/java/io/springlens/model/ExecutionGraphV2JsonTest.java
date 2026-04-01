package io.springlens.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.springlens.model.core.ExecutionContext;
import io.springlens.model.core.ExecutionEdge;
import io.springlens.model.core.ExecutionEntrypointKind;
import io.springlens.model.core.ExecutionNode;
import io.springlens.model.core.ExecutionOriginKind;
import io.springlens.model.core.ExecutionTransportKind;
import io.springlens.model.core.NodeStatus;
import io.springlens.model.core.NodeType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExecutionGraphV2JsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesExecutionContextWithDistributedFields() throws Exception {
        ExecutionContext context = new ExecutionContext(
                "orders-app",
                "orders-app-1",
                "execution-1",
                "trace-1",
                "span-1",
                "parent-span-1",
                ExecutionEntrypointKind.HTTP_SERVER,
                ExecutionTransportKind.HTTP,
                "order-service",
                "local",
                null,
                Map.of("path", "/orders/1"),
                List.of("ovl-order-status"),
                List.of("patch-order-status"),
                "observation-native"
        );

        String json = objectMapper.writeValueAsString(context);

        assertTrue(json.contains("\"traceId\":\"trace-1\""));
        assertTrue(json.contains("\"spanId\":\"span-1\""));
        assertTrue(json.contains("\"entrypointKind\":\"HTTP_SERVER\""));
        assertTrue(json.contains("\"transportKind\":\"HTTP\""));
        assertTrue(json.contains("\"activeOverlayIds\":[\"ovl-order-status\"]"));
        assertTrue(json.contains("\"activePatchIds\":[\"patch-order-status\"]"));
        assertTrue(json.contains("\"captureMode\":\"observation-native\""));
    }

    @Test
    void serializesExecutionNodeAndEdgeWithOriginMetadata() throws Exception {
        ExecutionNode node = new ExecutionNode(
                "node-1",
                NodeType.HTTP_REQUEST,
                "/orders/1",
                ExecutionOriginKind.OBSERVATION,
                "OrderController#getOrder",
                NodeStatus.SUCCESS,
                null,
                null,
                Map.of("status", 200)
        );
        ExecutionEdge edge = new ExecutionEdge("node-1", "node-2", "calls_http", ExecutionOriginKind.AGENT_OVERLAY);

        String nodeJson = objectMapper.writeValueAsString(node);
        String edgeJson = objectMapper.writeValueAsString(edge);

        assertTrue(nodeJson.contains("\"originKind\":\"OBSERVATION\""));
        assertTrue(nodeJson.contains("\"sourceRef\":\"OrderController#getOrder\""));
        assertTrue(edgeJson.contains("\"relation\":\"calls_http\""));
        assertTrue(edgeJson.contains("\"originKind\":\"AGENT_OVERLAY\""));
    }
}
