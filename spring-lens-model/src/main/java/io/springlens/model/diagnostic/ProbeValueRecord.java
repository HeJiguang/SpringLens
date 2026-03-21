package io.springlens.model.diagnostic;

import java.time.Instant;

/**
 * 探针值记录 (Probe Value Record)。
 * 当程序代码中设定的探针（如 @LensWatch 或是 Lens.look()）被触发时，捕获到的业务数据快照。
 *
 * @param graphId       包含该探针触发事件的执行图 ID。
 * @param nodeId        响应该探针事件的底层执行节点 ID。
 * @param probeId       探针的唯一标识符（即代码中声明的 ID）。
 * @param description   对该探针的人类可读的详细描述。
 * @param value         探针实际捕获到的变量值（通常是序列化或脱敏后的对象快照）。
 * @param valueType     捕获值的具体 Java 数据类型（类名）。
 * @param requestPath   触发当前探针所在的外部请求路径。
 * @param occurredAt    探针命中并捕获数据的具体时间点。
 * @param captureSource 触发数据的来源代码位置或方法名。
 * @param phase         捕获数据的生命周期阶段（如执行前、返回后）。
 */
public record ProbeValueRecord(
        String graphId,
        String nodeId,
        String probeId,
        String description,
        Object value,
        String valueType,
        String requestPath,
        Instant occurredAt,
        String captureSource,
        ProbeCapturePhase phase
) {
}
