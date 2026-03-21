package io.springlens.starter;

import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring WebMvc 配置钩子。
 * 主要用途是将写好的 ExceptionInterceptor 注册到 Spring MVC 的执行链中去，
 * 以便在 Controller 抛出异常返回前能够被拦截并送入 Graph。
 */
public class RuntimeWebMvcConfiguration implements WebMvcConfigurer {

    private final LensExceptionInterceptor exceptionInterceptor;

    public RuntimeWebMvcConfiguration(LensExceptionInterceptor exceptionInterceptor) {
        this.exceptionInterceptor = exceptionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(exceptionInterceptor);
    }
}
