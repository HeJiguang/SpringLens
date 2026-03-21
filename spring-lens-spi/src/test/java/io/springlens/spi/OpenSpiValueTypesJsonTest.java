package io.springlens.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class OpenSpiValueTypesJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void supportsCustomSpiTypesAndKeepsStringJsonShape() throws Exception {
        assertEquals("PROBE_VALUE_CAPTURED", RuntimeSignalType.PROBE_VALUE_CAPTURED.value());
        assertEquals("PUBLIC", ToolExposureLevel.PUBLIC.value());
        assertEquals(RuntimeSignalType.of("AGENT_STEP"), objectMapper.readValue("\"AGENT_STEP\"", RuntimeSignalType.class));
        assertEquals(ToolExposureLevel.of("PARTNER"), objectMapper.readValue("\"PARTNER\"", ToolExposureLevel.class));
        assertEquals("\"JDBC_EXECUTED\"", objectMapper.writeValueAsString(RuntimeSignalType.JDBC_EXECUTED));
        assertEquals("\"INTERNAL\"", objectMapper.writeValueAsString(ToolExposureLevel.INTERNAL));
    }
}
