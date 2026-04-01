package io.springlens.agent.starter;

import io.springlens.agent.contract.OverlayDeliverySnapshot;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

public class AgentOverlaySyncService implements InitializingBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(AgentOverlaySyncService.class);

    private final AgentOverlayControlClient controlClient;
    private final AgentOverlayEngine overlayEngine;
    private final AgentInstrumentationProperties properties;
    private final ScheduledExecutorService refreshExecutor;
    private volatile ScheduledFuture<?> refreshTask;

    public AgentOverlaySyncService(
            AgentOverlayControlClient controlClient,
            AgentOverlayEngine overlayEngine,
            AgentInstrumentationProperties properties,
            ScheduledExecutorService refreshExecutor
    ) {
        this.controlClient = controlClient;
        this.overlayEngine = overlayEngine;
        this.properties = properties;
        this.refreshExecutor = refreshExecutor;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void refreshOnStartup() {
        if (!properties.isStartupSyncEnabled()) {
            return;
        }
        refresh();
    }

    @Override
    public void afterPropertiesSet() {
        if (!properties.isPeriodicRefreshEnabled() || refreshExecutor == null) {
            return;
        }
        long intervalMillis = properties.getRefreshIntervalMillis();
        if (intervalMillis <= 0) {
            logger.warn("Skipping agent overlay periodic refresh because refresh interval is not positive: {}", intervalMillis);
            return;
        }
        refreshTask = refreshExecutor.scheduleWithFixedDelay(this::safeRefresh, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }

    public void refresh() {
        OverlayDeliverySnapshot snapshot = controlClient.fetchSnapshot();
        if (snapshot != null) {
            overlayEngine.applySnapshot(snapshot);
        }
    }

    @Override
    public void destroy() {
        if (refreshTask != null) {
            refreshTask.cancel(true);
            refreshTask = null;
        }
    }

    private void safeRefresh() {
        try {
            refresh();
        }
        catch (RuntimeException ex) {
            logger.warn("Agent overlay periodic refresh failed: {}", ex.getMessage());
        }
    }
}
