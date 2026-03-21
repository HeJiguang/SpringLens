package io.springlens.spi;

import java.util.Set;

/**
 * 运行时信号收集器 (Runtime Collector)。
 * 这是一个核心扩展点（SPI）。它负责订阅感兴趣的原始 {@link RuntimeSignal}，并将它们解析并绘制到执行图中。
 * 可以通过实现该接口来添加对新类型中间件或新事件的收集能力。
 */
public interface RuntimeCollector {

    /**
     * @return 收集器的唯一标识符（如 "http-collector"）。
     */
    String id();

    /**
     * @return 当前收集器声明自己能够处理和关心的信号类型集合。
     */
    Set<RuntimeSignalType> supportedTypes();

    /**
     * 处理到来的信号，并对执行图进行结构性修改。
     *
     * @param signal 引发收集动作的原始运行信号。
     * @param graph  当前请求上下文中的执行图修改器，收集器通过它向图中添加节点和连线。
     */
    void collect(RuntimeSignal signal, GraphMutation graph);

    /**
     * 判断是否支持该类型的信号。
     * @param signalType 信号类型
     * @return true 如果支持
     */
    default boolean supports(RuntimeSignalType signalType) {
        return supportedTypes().contains(signalType);
    }
}
