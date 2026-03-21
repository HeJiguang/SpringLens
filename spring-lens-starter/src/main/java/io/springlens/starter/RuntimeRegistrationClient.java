package io.springlens.starter;

import io.springlens.model.AppRegistration;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 运行时启动注册机 (Registration Client)。
 * 当 Spring Boot 启动并完全就绪后（监听 ApplicationReadyEvent），主动给中央 Server 发送一封报到信。
 * 告诉 Server："我启动好了，你可以从我的 runtimeBaseUrl 拉取内存里的排查数据了"。
 */
public class RuntimeRegistrationClient {

    private static final Logger logger = LoggerFactory.getLogger(RuntimeRegistrationClient.class);

    private final RestClient restClient;
    private final LensRuntimeProperties properties;

    public RuntimeRegistrationClient(RestClient.Builder restClientBuilder, LensRuntimeProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    /**
     * Spring Boot 容器完全启动时触发的方法。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void register() {
        if (!properties.isRegistrationEnabled()) {
            return;
        }
        
        // 必须要配置自己暴露出去的 URL 才能完成拉取体系
        if (properties.getRuntimeBaseUrl() == null || properties.getRuntimeBaseUrl().isBlank()) {
            logger.info("Skipping Spring Lens registration because spring.lens.runtime-base-url is not configured.");
            return;
        }
        
        // 构造注册信息，对应上文我们在 model 包中看到的 AppRegistration 模型
        AppRegistration registration = new AppRegistration(
                properties.getApplicationId(),
                properties.getInstanceId(),
                URI.create(properties.getRuntimeBaseUrl()),
                Instant.now(),
                Map.of("signals", "http,exception,jdbc-slow-sql")
        );
        try {
            // 通过 HTTP POST 发往中央 Server
            restClient.post()
                    .uri(UriComponentsBuilder.fromUriString(properties.getServerUrl())
                            .path("/internal/apps/register")
                            .build(true)
                            .toUri())
                    .body(registration)
                    .retrieve()
                    .toBodilessEntity();
        }
        catch (Exception ex) {
            logger.warn("Spring Lens registration failed: {}", ex.getMessage());
        }
    }
}
