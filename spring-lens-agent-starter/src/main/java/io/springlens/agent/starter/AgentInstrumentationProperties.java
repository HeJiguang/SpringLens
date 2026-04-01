package io.springlens.agent.starter;

import io.springlens.agent.contract.AgentInstrumentationMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.lens.agent.instrumentation")
public class AgentInstrumentationProperties {

    private boolean enabled = true;
    private AgentInstrumentationMode mode = AgentInstrumentationMode.HYBRID_APPROVAL;
    private boolean sourceEditEnabled = true;
    private boolean sourceEditAutoApply = false;
    private boolean startupSyncEnabled = true;
    private boolean periodicRefreshEnabled = true;
    private long refreshIntervalMillis = 30_000L;
    private String overlayPullPath = "/internal/apps/{applicationId}/agent-overlays";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public AgentInstrumentationMode getMode() {
        return mode;
    }

    public void setMode(AgentInstrumentationMode mode) {
        this.mode = mode;
    }

    public boolean isSourceEditEnabled() {
        return sourceEditEnabled;
    }

    public void setSourceEditEnabled(boolean sourceEditEnabled) {
        this.sourceEditEnabled = sourceEditEnabled;
    }

    public boolean isSourceEditAutoApply() {
        return sourceEditAutoApply;
    }

    public void setSourceEditAutoApply(boolean sourceEditAutoApply) {
        this.sourceEditAutoApply = sourceEditAutoApply;
    }

    public boolean isStartupSyncEnabled() {
        return startupSyncEnabled;
    }

    public void setStartupSyncEnabled(boolean startupSyncEnabled) {
        this.startupSyncEnabled = startupSyncEnabled;
    }

    public boolean isPeriodicRefreshEnabled() {
        return periodicRefreshEnabled;
    }

    public void setPeriodicRefreshEnabled(boolean periodicRefreshEnabled) {
        this.periodicRefreshEnabled = periodicRefreshEnabled;
    }

    public long getRefreshIntervalMillis() {
        return refreshIntervalMillis;
    }

    public void setRefreshIntervalMillis(long refreshIntervalMillis) {
        this.refreshIntervalMillis = refreshIntervalMillis;
    }

    public String getOverlayPullPath() {
        return overlayPullPath;
    }

    public void setOverlayPullPath(String overlayPullPath) {
        this.overlayPullPath = overlayPullPath;
    }
}
