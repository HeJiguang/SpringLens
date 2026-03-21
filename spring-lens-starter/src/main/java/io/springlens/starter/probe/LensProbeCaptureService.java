package io.springlens.starter.probe;

import io.springlens.model.diagnostic.ProbeCapturePhase;
import io.springlens.runtime.RuntimeSignalProcessor;
import io.springlens.spi.RuntimeSignal;
import io.springlens.spi.RuntimeSignalType;
import io.springlens.starter.RuntimeExecutionContextHolder;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 探针信号发射器 (Probe Capture Service)。
 * 充当了一个中转站，将 AOP 切面送来的拦截值，或者是 Lens.look 送来的硬编码值，
 * 进行脱敏 (Sanitize) 和组装后，伪装成一个 RuntimeSignal 最终投递给大脑。
 */
public class LensProbeCaptureService implements Lens.LensOperations {

    private final RuntimeSignalProcessor signalProcessor;
    private final RuntimeExecutionContextHolder contextHolder;
    private final LensProbeRegistry probeRegistry;
    private final LensValueSanitizer valueSanitizer;

    public LensProbeCaptureService(
            RuntimeSignalProcessor signalProcessor,
            RuntimeExecutionContextHolder contextHolder,
            LensProbeRegistry probeRegistry,
            LensValueSanitizer valueSanitizer
    ) {
        this.signalProcessor = signalProcessor;
        this.contextHolder = contextHolder;
        this.probeRegistry = probeRegistry;
        this.valueSanitizer = valueSanitizer;
        // 把自身注入到静态类 Lens 身上，点通了“硬编码探针 -> 服务端”的通路
        Lens.bind(this);
    }

    public void captureAnnotation(LensWatch watch, ProbeCapturePhase phase, Object value) {
        probeRegistry.registerAnnotation(watch);
        emit(watch.id(), watch.description(), phase, "annotation", value);
    }

    @Override
    public void look(String id, Object value, String description) {
        probeRegistry.registerManual(id, description);
        emit(id, description, ProbeCapturePhase.MANUAL, "manual", value);
    }

    /**
     * 核心生产逻辑：将内存快照值打包装进信号内！
     */
    private void emit(String probeId, String description, ProbeCapturePhase phase, String captureSource, Object value) {
        // 只有当前绑定了一个活跃的能够画出图的 Http 请求上下文中，探针才有效发出去
        contextHolder.currentExecutionId().ifPresent(executionId -> {
            
            // AI 虽然强大，但不能把 G级内存数据拉爆！必须序列化清理，抹除敏感且巨大的无用引用（脱敏）
            LensValueSanitizer.SanitizedValue sanitized = valueSanitizer.sanitize(value);
            
            Map<String, Object> attributes = new LinkedHashMap<>();
            attributes.put("probeId", probeId);
            attributes.put("description", description);
            attributes.put("captureSource", captureSource);
            attributes.put("capturePhase", phase.value());
            
            // 这个是专门留给 AI 读的值
            attributes.put("value", sanitized.value());
            attributes.put("valueType", sanitized.valueType());
            
            signalProcessor.process(new RuntimeSignal(
                    executionId,
                    RuntimeSignalType.PROBE_VALUE_CAPTURED,
                    Instant.now(),
                    attributes
            ));
        });
    }
}
