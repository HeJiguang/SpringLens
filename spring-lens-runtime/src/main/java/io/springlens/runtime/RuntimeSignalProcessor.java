package io.springlens.runtime;

import io.springlens.model.core.ExecutionContext;
import io.springlens.spi.RuntimeCollector;
import io.springlens.spi.RuntimeSignal;
import io.springlens.spi.RuntimeSignalType;
import java.util.List;

/**
 * 运行时信号处理器 (Runtime Signal Processor)。
 * 它是 Spring Lens 运行时的核心“中央事件总线”与“调度引擎”。
 * 负责接收来自外界的原始信号，并把信号分发给所有感兴趣的 {@link RuntimeCollector} 收集器来处理。
 */
public final class RuntimeSignalProcessor {

    private final InMemoryExecutionGraphStore store;
    private final List<RuntimeCollector> collectors;

    /**
     * @param store      用于在内存中存储执行图的 Store 实例。
     * @param collectors 系统中注册的所有收集器（通过 Spring 的 DI 自动注入所有的 SPI 实现）。
     */
    public RuntimeSignalProcessor(InMemoryExecutionGraphStore store, List<RuntimeCollector> collectors) {
        this.store = store;
        this.collectors = List.copyOf(collectors);
    }

    /**
     * 当一个新的请求（或业务流）刚开始时被调用，用于初始化一个全新的空白执行图。
     */
    public void start(ExecutionContext context) {
        store.start(context);
    }

    /**
     * 引擎的核心处理循环：处理一个流入的原始信号。
     * 
     * @param signal 发生的异常、SQL 等底层事件信号
     */
    public void process(RuntimeSignal signal) {
        // 尝试获取当前信号所对应的请求正在描绘的执行图
        store.activeGraph(signal.executionId()).ifPresent(graph -> {
            
            // 将信号分发给所有声明了能够处理这种类型的收集器
            collectors.stream()
                    .filter(collector -> collector.supports(signal.type()))
                    .forEach(collector -> collector.collect(signal, graph));
            
            // 如果收到 HTTP 请求已经完成的信号，意味着这张图画完了，调用 store.complete() 把它封板归档
        if (RuntimeSignalType.HTTP_REQUEST_COMPLETED.equals(signal.type())) {
                store.complete(signal.executionId());
            }
        });
    }
}
