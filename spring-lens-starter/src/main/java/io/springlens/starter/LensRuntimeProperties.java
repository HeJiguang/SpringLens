package io.springlens.starter;

import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.lens")
public class LensRuntimeProperties {

    private boolean enabled = true;
    private boolean observationNativeEnabled = true;
    private boolean compatibilityInstrumentationEnabled = true;
    private boolean registrationEnabled = true;
    private String applicationId;
    private String instanceId = UUID.randomUUID().toString();
    private String serverUrl = "http://localhost:8090";
    private String runtimeBaseUrl;
    private long slowSqlThresholdMs = 200L;
    private int maxCompletedGraphs = 200;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isObservationNativeEnabled() {
        return observationNativeEnabled;
    }

    public void setObservationNativeEnabled(boolean observationNativeEnabled) {
        this.observationNativeEnabled = observationNativeEnabled;
    }

    public boolean isCompatibilityInstrumentationEnabled() {
        return compatibilityInstrumentationEnabled;
    }

    public void setCompatibilityInstrumentationEnabled(boolean compatibilityInstrumentationEnabled) {
        this.compatibilityInstrumentationEnabled = compatibilityInstrumentationEnabled;
    }

    public boolean isRegistrationEnabled() {
        return registrationEnabled;
    }

    public void setRegistrationEnabled(boolean registrationEnabled) {
        this.registrationEnabled = registrationEnabled;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getRuntimeBaseUrl() {
        return runtimeBaseUrl;
    }

    public void setRuntimeBaseUrl(String runtimeBaseUrl) {
        this.runtimeBaseUrl = runtimeBaseUrl;
    }

    public long getSlowSqlThresholdMs() {
        return slowSqlThresholdMs;
    }

    public void setSlowSqlThresholdMs(long slowSqlThresholdMs) {
        this.slowSqlThresholdMs = slowSqlThresholdMs;
    }

    public int getMaxCompletedGraphs() {
        return maxCompletedGraphs;
    }

    public void setMaxCompletedGraphs(int maxCompletedGraphs) {
        this.maxCompletedGraphs = maxCompletedGraphs;
    }
}
