package io.springlens.model;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

/**
 * 应用注册登记信息 (App Registration)。
 * 当集成 Spring Lens 的微服务实例启动时，向中央服务端 (Server) 注册的自身信息。
 *
 * @param applicationId  注册应用的分组标识名称（如 spring-lens-demo）。
 * @param instanceId     应用实例的全局唯一标识。
 * @param runtimeBaseUrl 当前应用实例暴露内部探测 API 的基础 URL。Server 将使用此 URL 主动拉取数据。
 * @param registeredAt   发起注册的时间戳。
 * @param capabilities   标明该实例支持能力的扩展属性字典。
 */
public record AppRegistration(
        String applicationId,
        String instanceId,
        URI runtimeBaseUrl,
        Instant registeredAt,
        Map<String, String> capabilities
) {

    public AppRegistration {
        capabilities = capabilities == null ? Map.of() : Map.copyOf(capabilities);
    }
}
