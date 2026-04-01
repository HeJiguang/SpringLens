package io.springlens.agent.starter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.springlens.agent.contract.AgentActionRiskLevel;
import io.springlens.agent.contract.AgentInstrumentationMode;
import io.springlens.agent.contract.ApprovalState;
import io.springlens.agent.contract.OverlayDeliverySnapshot;
import io.springlens.agent.contract.OverlaySpec;
import io.springlens.agent.contract.PolicySnapshot;
import io.springlens.agent.contract.RegisteredOverlay;
import io.springlens.starter.LensRuntimeProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.Test;

class AgentOverlaySyncServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-03-31T16:46:00Z"), java.time.ZoneOffset.UTC);

    @Test
    void refreshLoadsOnlyDeliverableOverlaysIntoEngine() {
        AgentOverlayControlClient client = mock(AgentOverlayControlClient.class);
        when(client.fetchSnapshot()).thenReturn(new OverlayDeliverySnapshot(
                "orders-app",
                "orders-1",
                new PolicySnapshot(
                        AgentInstrumentationMode.HYBRID_APPROVAL,
                        true,
                        false,
                        true,
                        Map.of("plane", "agent-control")
                ),
                List.of(
                        registeredOverlay("ovl-orders-approved", ApprovalState.APPROVED, "PT4H", "2026-03-31T16:45:00Z"),
                        registeredOverlay("ovl-orders-pending", ApprovalState.PENDING, "PT4H", "2026-03-31T16:45:00Z")
                ),
                "2026-03-31T16:45:05Z"
        ));
        AgentOverlayEngine engine = overlayEngine();

        AgentOverlaySyncService syncService = new AgentOverlaySyncService(client, engine, agentProperties(), null);
        syncService.refresh();

        assertThat(engine.activeOverlays()).extracting(OverlaySpec::overlayId)
                .containsExactly("ovl-orders-approved");
        assertThat(engine.lastDeliveredAt()).isEqualTo("2026-03-31T16:45:05Z");
    }

    @Test
    void refreshRemovesOverlayWhenServerStopsDeliveringIt() {
        AgentOverlayControlClient client = mock(AgentOverlayControlClient.class);
        when(client.fetchSnapshot())
                .thenReturn(new OverlayDeliverySnapshot(
                        "orders-app",
                        "orders-1",
                        policySnapshot(),
                        List.of(registeredOverlay("ovl-orders-approved", ApprovalState.APPROVED, "PT4H", "2026-03-31T16:45:00Z")),
                        "2026-03-31T16:45:05Z"
                ))
                .thenReturn(new OverlayDeliverySnapshot(
                        "orders-app",
                        "orders-1",
                        policySnapshot(),
                        List.of(),
                        "2026-03-31T16:46:05Z"
                ));
        AgentOverlayEngine engine = overlayEngine();

        AgentOverlaySyncService syncService = new AgentOverlaySyncService(client, engine, agentProperties(), null);
        syncService.refresh();
        syncService.refresh();

        assertThat(engine.activeOverlays()).isEmpty();
        assertThat(engine.lastDeliveredAt()).isEqualTo("2026-03-31T16:46:05Z");
    }

    @Test
    void refreshOnStartupHonorsStartupSyncFlag() {
        AgentOverlayControlClient client = mock(AgentOverlayControlClient.class);
        AgentOverlayEngine engine = overlayEngine();
        AgentInstrumentationProperties properties = agentProperties();
        properties.setStartupSyncEnabled(false);

        AgentOverlaySyncService syncService = new AgentOverlaySyncService(client, engine, properties, null);
        syncService.refreshOnStartup();

        verify(client, never()).fetchSnapshot();
    }

    @Test
    void schedulesPeriodicRefreshWhenEnabled() throws Exception {
        AgentOverlayControlClient client = mock(AgentOverlayControlClient.class);
        ScheduledExecutorService executorService = mock(ScheduledExecutorService.class);
        ScheduledFuture<?> future = mock(ScheduledFuture.class);
        doReturn(future).when(executorService).scheduleWithFixedDelay(
                org.mockito.ArgumentMatchers.any(Runnable.class),
                org.mockito.ArgumentMatchers.eq(30_000L),
                org.mockito.ArgumentMatchers.eq(30_000L),
                org.mockito.ArgumentMatchers.eq(java.util.concurrent.TimeUnit.MILLISECONDS)
        );
        AgentOverlayEngine engine = overlayEngine();

        AgentOverlaySyncService syncService = new AgentOverlaySyncService(client, engine, agentProperties(), executorService);
        syncService.afterPropertiesSet();
        syncService.destroy();

        verify(executorService).scheduleWithFixedDelay(
                org.mockito.ArgumentMatchers.any(Runnable.class),
                org.mockito.ArgumentMatchers.eq(30_000L),
                org.mockito.ArgumentMatchers.eq(30_000L),
                org.mockito.ArgumentMatchers.eq(java.util.concurrent.TimeUnit.MILLISECONDS)
        );
        verify(future).cancel(true);
    }

    private AgentInstrumentationProperties agentProperties() {
        AgentInstrumentationProperties properties = new AgentInstrumentationProperties();
        properties.setEnabled(true);
        properties.setMode(AgentInstrumentationMode.HYBRID_APPROVAL);
        return properties;
    }

    private LensRuntimeProperties runtimeProperties() {
        LensRuntimeProperties properties = new LensRuntimeProperties();
        properties.setApplicationId("orders-app");
        properties.setInstanceId("orders-1");
        return properties;
    }

    private AgentOverlayEngine overlayEngine() {
        return new AgentOverlayEngine(agentProperties(), runtimeProperties(), FIXED_CLOCK);
    }

    private PolicySnapshot policySnapshot() {
        return new PolicySnapshot(
                AgentInstrumentationMode.HYBRID_APPROVAL,
                true,
                false,
                true,
                Map.of("plane", "agent-control")
        );
    }

    private RegisteredOverlay registeredOverlay(String overlayId, ApprovalState approvalState, String ttl, String createdAt) {
        return new RegisteredOverlay(
                overlayId,
                new OverlaySpec(
                        overlayId,
                        AgentInstrumentationMode.HYBRID_APPROVAL,
                        AgentActionRiskLevel.MEDIUM,
                        true,
                        ttl,
                        "spring-bean-method",
                        "com.example.order.OrderService",
                        "submitOrder",
                        "AFTER_RETURN",
                        "order.submit.status",
                        "result.status",
                        "Capture status",
                        List.of("orders"),
                        Map.of("applicationId", "orders-app")
                ),
                approvalState,
                createdAt,
                null,
                Map.of()
        );
    }
}
