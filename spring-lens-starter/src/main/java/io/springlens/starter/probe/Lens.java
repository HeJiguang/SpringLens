package io.springlens.starter.probe;

/**
 * 编程式手动埋点入口点 (Programmatic Probe API)。
 * 当仅仅使用 {@code @LensWatch} 切面无法满足极其精准的代码块内部变量捕获时，
 * 开发者可以在代码深处直接调用 `Lens.look("变量标识", 变量本身, "描述")` 来捕获运行期内存值。
 */
public final class Lens {

    // 默认空实现，以防在 Spring 容器未启动时调用报错
    private static volatile LensOperations operations = (id, value, description) -> {
    };

    private Lens() {
    }

    /**
     * 框架通过后门绑定的真实业务实现 (通常在 LensProbeCaptureService 初始化时调用注入真实能力)
     */
    static void bind(LensOperations operations) {
        Lens.operations = operations;
    }

    /**
     * 向 Spring Lens 上报一个活体变量观测值。
     * @param id 全局唯一的观测点 ID
     * @param value 具体想要观察的内存对象实例
     * @param description 让 AI 知道这个值代表什么含义
     */
    public static void look(String id, Object value, String description) {
        operations.look(id, value, description);
    }

    @FunctionalInterface
    interface LensOperations {
        void look(String id, Object value, String description);
    }
}
