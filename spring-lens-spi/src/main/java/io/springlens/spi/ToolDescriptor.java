package io.springlens.spi;

/**
 * 工具说明书 (Tool Descriptor)。
 * 面向 AI 的功能“推销说明”，详细解释本工具有什么用，应该怎么用。
 * （注：这个与 spring-lens-model 里的 ProjectToolDescriptor 类似，但那个针对的是客户端应用上方的微服务级工具，而这个面向的是在 Server 端本身的全局系统诊断工具，如获取全部应用列表等）。
 *
 * @param name        工具的可被调用的名字标识（如 "get_slow_sql"）。
 * @param description 描述工具的作用环境以及对排查有何帮助的详细文本。
 */
public record ToolDescriptor(
        String name,
        String description
) {
}
