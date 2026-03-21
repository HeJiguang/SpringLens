package io.springlens.spi;

import java.time.Instant;
import java.util.Map;

/**
 * 运行时信号 (Runtime Signal)。
 * 代表底层框架（如 Spring AOP、Filter 等）捕获到的一个极其原始的动作或事件。
 * 它是驱动 Spring Lens 收集器 (Collectors) 工作的源动力。
 *
 * @param executionId 产生该信号的全局唯一执行 ID（请求链 ID）。
 * @param type        信号的具体类型（如 HTTP 请求开始，或者发生异常）。
 * @param occurredAt  信号引发的具体时刻。
 * @param attributes  该信号附带的原始原生数据（如 Method 对象、Exception 堆栈、Request URL 等）。
 */
public record RuntimeSignal(
        String executionId,
        RuntimeSignalType type,
        Instant occurredAt,
        Map<String, Object> attributes
) {

    public RuntimeSignal {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
