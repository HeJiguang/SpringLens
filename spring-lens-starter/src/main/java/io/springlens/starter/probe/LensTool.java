package io.springlens.starter.probe;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Spring Lens AI 专属工具端点暴露注解。
 * 可以用来修饰微服务上的任意 Spring 容器内部实例的方法（不建议用在影响正向主业的方法上）。
 * 这些方法将被代理为 AI Agent 随时待命的一个子功能（比如：清空用户缓存工具，补偿一次订单等）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LensTool {

    /** 此工具的名字，给 AI 大模型调用用的标示 */
    String name();

    /** 工具的详细描述（Prompt 级别），越详细 AI 越知道什么时候用它，参数传什么好 */
    String description() default "";
}
