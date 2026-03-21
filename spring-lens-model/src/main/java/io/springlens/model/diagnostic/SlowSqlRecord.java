package io.springlens.model.diagnostic;

import java.time.Instant;

/**
 * 慢 SQL 记录 (Slow SQL Record)。
 * 当数据库 JDBC 执行时间超过设定阈值时生成的审计记录。
 *
 * @param graphId     包含该慢 SQL 的执行图 ID。
 * @param sqlNodeId   具体执行该慢 SQL 的节点 ID。
 * @param sql         被执行的原生 SQL 语句。
 * @param durationMs  该 SQL 语句的实际执行耗时（毫秒）。
 * @param requestPath 触发该 SQL 的外部请求路径（如果有）。
 * @param occurredAt  慢 SQL 执行完毕的时间戳。
 */
public record SlowSqlRecord(
        String graphId,
        String sqlNodeId,
        String sql,
        long durationMs,
        String requestPath,
        Instant occurredAt
) {
}
