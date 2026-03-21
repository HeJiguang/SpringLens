package io.springlens.starter;

import java.util.Optional;

/**
 * 线程上下文寄存器 (Context Holder)。
 * 它是保证同一次 HTTP 请求无论穿插到哪个 Controller、Service 层和 DAO 层，
 * 我们都能通过 ThreadLocal 把同一个 executionId 衔接起来的核心载体。
 */
public final class RuntimeExecutionContextHolder {

    private final ThreadLocal<String> currentExecutionId = new ThreadLocal<>();

    public void set(String executionId) {
        currentExecutionId.set(executionId);
    }

    public Optional<String> currentExecutionId() {
        return Optional.ofNullable(currentExecutionId.get());
    }

    public void clear() {
        currentExecutionId.remove();
    }
}
