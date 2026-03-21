package io.springlens.model.diagnostic;

import java.time.Instant;
import java.util.List;

/**
 * 异常上下文记录 (Exception Context Record)。
 * 当应用程序中抛出异常并被系统捕获时，会生成此记录。
 *
 * @param graphId         产生该异常的执行图 ID。
 * @param exceptionNodeId 抛出该异常的具体执行节点 ID。
 * @param exceptionClass  异常的具体类名（如 java.lang.NullPointerException）。
 * @param message         异常的错误信息（Exception.getMessage()）。
 * @param requestPath     触发该异常的请求路径（如果是 HTTP 请求的话）。
 * @param occurredAt      异常发生的具体时间戳。
 * @param stackTrace      完整的异常堆栈轨迹信息列表。
 */
public record ExceptionContextRecord(
        String graphId,
        String exceptionNodeId,
        String exceptionClass,
        String message,
        String requestPath,
        Instant occurredAt,
        List<String> stackTrace
) {

    public ExceptionContextRecord {
        stackTrace = stackTrace == null ? List.of() : List.copyOf(stackTrace);
    }
}
