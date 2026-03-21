package io.springlens.starter;

import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring Lens 配置文件属性。
 * 对应于 application.yml 中的 `spring.lens.*` 配置项。
 */
@ConfigurationProperties(prefix = "spring.lens")
public class LensRuntimeProperties {

    /** 是否开启 Spring Lens 运行时的所有功能，默认 true */
    private boolean enabled = true;
    
    /** 是否开启向中央 Server 注册报到的功能，默认 true */
    private boolean registrationEnabled = true;

    /** 当前应用的全局唯一定义名称组别（如 "order-service"），不填默认读 spring.application.name */
    private String applicationId;
    
    /** 当前微服务实例的唯一 ID（随机生成），区分横向扩展的不同 Pod */
    private String instanceId = UUID.randomUUID().toString();
    
    /** 中央 Server 的地址（例如本地部署的 http://localhost:8090） */
    private String serverUrl = "http://localhost:8090";
    
    /** 
     * 本地应用直接暴露的外部可访问基地址（如 http://10.0.0.1:8080）。
     * Server 端将利用这个地址发起请求来主动拉取数据（Pull 模型）。 
     */
    private String runtimeBaseUrl;
    
    /** 慢 SQL 执行时间的判断阈值，单位毫秒，默认 200ms */
    private long slowSqlThresholdMs = 200L;
    
    /** 内存环形队列中保存的最新完成的图快照数量，默认 200 个 */
    private int maxCompletedGraphs = 200;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
