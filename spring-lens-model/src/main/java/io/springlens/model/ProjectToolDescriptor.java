package io.springlens.model;

/**
 * 项目工具描述符 (Project Tool Descriptor)。
 * 用于声明应用程序通过 @LensTool 向外（例如 AI大模型 或 管理UI）暴露的可用排查工具方法。
 *
 * @param name        向外暴露的工具调用名称。
 * @param description 工具的功能和作用详尽描述。AI 将借此理解何时该调用此工具。
 */
public record ProjectToolDescriptor(
        String name,
        String description
) {
}
