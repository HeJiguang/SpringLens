package io.springlens.spi;

import java.util.Map;

/**
 * 工具调用请求 (Tool Request)。
 * 封装了在执行某个诊断工具时传入的上下文参数和用户输入。
 *
 * @param applicationId 目标应用的分组 ID（如果工具的目标是一个具体应用的话）。
 * @param instanceId    目标应用实例的具体 ID（如果工具的目标是一台具体机器）。
 * @param arguments     AI 大模型为执行此工具自动推断、填充的具体业务参数键值对。
 */
public record ToolRequest(
        String applicationId,
        String instanceId,
        Map<String, Object> arguments
) {

    public ToolRequest {
        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
    }
}
