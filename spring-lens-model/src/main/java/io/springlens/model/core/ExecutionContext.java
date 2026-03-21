package io.springlens.model.core;

import java.time.Instant;
import java.util.Map;

/**
 * 执行上下文 (Execution Context)。
 * 记录了当前执行图的全局身份信息。
 *
 * @param applicationId 产生该执行图的应用标识（如 spring-lens-demo）。
 * @param instanceId    产生该执行图的具体应用实例标识。
 * @param executionId   全局唯一的执行追踪ID（类似于 Trace ID）。
 * @param startedAt     本次执行过程开始的时间戳。
 * @param tags          附属的自定义标签字典。
 */
public record ExecutionContext(
        String applicationId,
        String instanceId,
        String executionId,
        Instant startedAt,
        Map<String, String> tags
) {

    public ExecutionContext {
        tags = tags == null ? Map.of() : Map.copyOf(tags);
    }
}
