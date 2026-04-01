package io.springlens.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.springlens.model.core.ExecutionEntrypointKind;
import io.springlens.model.core.ExecutionOriginKind;
import io.springlens.model.core.ExecutionTransportKind;
import io.springlens.model.core.NodeStatus;
import io.springlens.model.core.NodeType;
import io.springlens.model.diagnostic.ProbeCapturePhase;
import io.springlens.model.diagnostic.ProbeCapturePhases;
import org.junit.jupiter.api.Test;

class OpenValueTypesJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void preservesStandardValuesAndSupportsCustomModelTypes() throws Exception {
        assertEquals("HTTP_REQUEST", NodeType.HTTP_REQUEST.value());
        assertEquals("RUNNING", NodeStatus.RUNNING.value());
        assertEquals("AFTER_RETURN", ProbeCapturePhase.AFTER_RETURN.value());
        assertEquals("HTTP_SERVER", ExecutionEntrypointKind.HTTP_SERVER.value());
        assertEquals("OBSERVATION", ExecutionOriginKind.OBSERVATION.value());
        assertEquals("HTTP", ExecutionTransportKind.HTTP.value());
        assertEquals(ProbeCapturePhase.AFTER_RETURN, ProbeCapturePhase.of(ProbeCapturePhases.AFTER_RETURN));
        assertEquals(NodeType.of("MQ_MESSAGE"), objectMapper.readValue("\"MQ_MESSAGE\"", NodeType.class));
        assertEquals(NodeStatus.of("QUEUED"), objectMapper.readValue("\"QUEUED\"", NodeStatus.class));
        assertEquals(ProbeCapturePhase.of("BEFORE_RETRY"), objectMapper.readValue("\"BEFORE_RETRY\"", ProbeCapturePhase.class));
        assertEquals(ExecutionEntrypointKind.of("KAFKA_CONSUMER"), objectMapper.readValue("\"KAFKA_CONSUMER\"", ExecutionEntrypointKind.class));
        assertEquals(ExecutionOriginKind.of("AGENT_PATCH"), objectMapper.readValue("\"AGENT_PATCH\"", ExecutionOriginKind.class));
        assertEquals(ExecutionTransportKind.of("MESSAGING"), objectMapper.readValue("\"MESSAGING\"", ExecutionTransportKind.class));
    }

    @Test
    void serializesModelTypesAsPlainStrings() throws Exception {
        assertEquals("\"HTTP_REQUEST\"", objectMapper.writeValueAsString(NodeType.HTTP_REQUEST));
        assertEquals("\"FAILURE\"", objectMapper.writeValueAsString(NodeStatus.FAILURE));
        assertEquals("\"MANUAL\"", objectMapper.writeValueAsString(ProbeCapturePhase.MANUAL));
        assertEquals("\"HTTP_SERVER\"", objectMapper.writeValueAsString(ExecutionEntrypointKind.HTTP_SERVER));
        assertEquals("\"OBSERVATION\"", objectMapper.writeValueAsString(ExecutionOriginKind.OBSERVATION));
        assertEquals("\"HTTP\"", objectMapper.writeValueAsString(ExecutionTransportKind.HTTP));
    }
}
