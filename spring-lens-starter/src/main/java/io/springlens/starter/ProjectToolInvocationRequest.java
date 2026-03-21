package io.springlens.starter;

import java.util.Map;

/**
 * AI 工具调用入参反序列化载体。
 * 当外部分析 Server 或大模型发来 POST 请求要激活微服务上的某个 @LensTool 时，
 * 它放在 HTTP Body 里的 Json 对象会被反序列化为此类。
 * @param arguments 这些参数接着会被 LensProjectToolRegistry 进行二次反射注入真实方法的参数里。
 */
public record ProjectToolInvocationRequest(Map<String, Object> arguments) {

    public ProjectToolInvocationRequest {
        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
    }
}
