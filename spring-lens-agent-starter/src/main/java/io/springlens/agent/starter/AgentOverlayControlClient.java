package io.springlens.agent.starter;

import io.springlens.agent.contract.OverlayDeliverySnapshot;
import io.springlens.starter.LensRuntimeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

public class AgentOverlayControlClient {

    private static final Logger logger = LoggerFactory.getLogger(AgentOverlayControlClient.class);

    private final RestClient restClient;
    private final LensRuntimeProperties runtimeProperties;
    private final AgentInstrumentationProperties agentInstrumentationProperties;

    public AgentOverlayControlClient(
            RestClient.Builder restClientBuilder,
            LensRuntimeProperties runtimeProperties,
            AgentInstrumentationProperties agentInstrumentationProperties
    ) {
        this.restClient = restClientBuilder.build();
        this.runtimeProperties = runtimeProperties;
        this.agentInstrumentationProperties = agentInstrumentationProperties;
    }

    public OverlayDeliverySnapshot fetchSnapshot() {
        String applicationId = normalize(runtimeProperties.getApplicationId());
        if (applicationId == null) {
            logger.info("Skipping overlay snapshot pull because spring.lens.application-id is not configured.");
            return null;
        }
        String overlayPath = agentInstrumentationProperties.getOverlayPullPath()
                .replace("{applicationId}", applicationId);
        try {
            return restClient.get()
                    .uri(UriComponentsBuilder.fromUriString(runtimeProperties.getServerUrl())
                            .path(overlayPath)
                            .queryParamIfPresent("instanceId", java.util.Optional.ofNullable(normalize(runtimeProperties.getInstanceId())))
                            .build(true)
                            .toUri())
                    .retrieve()
                    .body(OverlayDeliverySnapshot.class);
        }
        catch (Exception ex) {
            logger.warn("Agent overlay snapshot pull failed: {}", ex.getMessage());
            return null;
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
