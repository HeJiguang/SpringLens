package io.springlens.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
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
        assertEquals("HANDWRITTEN", ProjectToolSourceType.HANDWRITTEN.value());
        assertEquals(ProbeCapturePhase.AFTER_RETURN, ProbeCapturePhase.of(ProbeCapturePhases.AFTER_RETURN));
        assertEquals(NodeType.of("MQ_MESSAGE"), objectMapper.readValue("\"MQ_MESSAGE\"", NodeType.class));
        assertEquals(ProjectToolSourceType.of("GENERATED_AGENT"), objectMapper.readValue("\"GENERATED_AGENT\"", ProjectToolSourceType.class));
    }

    @Test
    void serializesModelTypesAsPlainStrings() throws Exception {
        assertEquals("\"HTTP_REQUEST\"", objectMapper.writeValueAsString(NodeType.HTTP_REQUEST));
        assertEquals("\"FAILURE\"", objectMapper.writeValueAsString(NodeStatus.FAILURE));
        assertEquals("\"MANUAL\"", objectMapper.writeValueAsString(ProbeCapturePhase.MANUAL));
        assertEquals("\"GENERATED_PROBE\"", objectMapper.writeValueAsString(ProjectToolSourceType.GENERATED_PROBE));
    }
}
