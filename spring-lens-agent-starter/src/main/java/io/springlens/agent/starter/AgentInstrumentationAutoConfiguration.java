package io.springlens.agent.starter;

import io.springlens.starter.LensRuntimeProperties;
import io.springlens.starter.probe.LensProbeCaptureService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.aop.Advisor;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
@EnableConfigurationProperties(AgentInstrumentationProperties.class)
public class AgentInstrumentationAutoConfiguration {

    @Bean
    @ConditionalOnBean(LensRuntimeProperties.class)
    @ConditionalOnProperty(prefix = "spring.lens.agent.instrumentation", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AgentOverlayEngine agentOverlayEngine(
            AgentInstrumentationProperties properties,
            LensRuntimeProperties runtimeProperties
    ) {
        return new AgentOverlayEngine(properties, runtimeProperties);
    }

    @Bean
    @ConditionalOnBean({LensRuntimeProperties.class, RestClient.Builder.class, AgentOverlayEngine.class})
    @ConditionalOnProperty(prefix = "spring.lens.agent.instrumentation", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AgentOverlayControlClient agentOverlayControlClient(
            RestClient.Builder restClientBuilder,
            LensRuntimeProperties runtimeProperties,
            AgentInstrumentationProperties properties
    ) {
        return new AgentOverlayControlClient(restClientBuilder, runtimeProperties, properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.lens.agent.instrumentation", name = "periodic-refresh-enabled", havingValue = "true", matchIfMissing = true)
    public ScheduledExecutorService agentOverlayRefreshExecutor() {
        return Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "spring-lens-agent-overlay-refresh");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Bean
    @ConditionalOnBean({AgentOverlayControlClient.class, AgentOverlayEngine.class})
    public AgentOverlaySyncService agentOverlaySyncService(
            AgentOverlayControlClient controlClient,
            AgentOverlayEngine overlayEngine,
            AgentInstrumentationProperties properties,
            @org.springframework.beans.factory.annotation.Autowired(required = false) ScheduledExecutorService refreshExecutor
    ) {
        return new AgentOverlaySyncService(controlClient, overlayEngine, properties, refreshExecutor);
    }

    @Bean
    @ConditionalOnBean({AgentOverlayEngine.class, LensProbeCaptureService.class})
    public AgentOverlayValueResolver agentOverlayValueResolver() {
        return new AgentOverlayValueResolver();
    }

    @Bean
    @ConditionalOnBean({AgentOverlayEngine.class, LensProbeCaptureService.class, AgentOverlayValueResolver.class})
    public AgentOverlayMethodInterceptor agentOverlayMethodInterceptor(
            AgentOverlayEngine overlayEngine,
            LensProbeCaptureService captureService,
            AgentOverlayValueResolver valueResolver
    ) {
        return new AgentOverlayMethodInterceptor(overlayEngine, captureService, valueResolver);
    }

    @Bean
    @ConditionalOnBean(AgentOverlayMethodInterceptor.class)
    public Advisor agentOverlayMethodAdvisor(AgentOverlayMethodInterceptor interceptor) {
        Pointcut pointcut = new Pointcut() {
            @Override
            public ClassFilter getClassFilter() {
                return AgentInstrumentationAutoConfiguration::isCandidateApplicationBean;
            }

            @Override
            public MethodMatcher getMethodMatcher() {
                return MethodMatcher.TRUE;
            }
        };
        return new DefaultPointcutAdvisor(pointcut, interceptor);
    }

    @Bean
    @ConditionalOnBean({AgentOverlayEngine.class, LensProbeCaptureService.class, AgentOverlayValueResolver.class})
    public AgentOverlayHttpInterceptor agentOverlayHttpInterceptor(
            AgentOverlayEngine overlayEngine,
            LensProbeCaptureService captureService,
            AgentOverlayValueResolver valueResolver
    ) {
        return new AgentOverlayHttpInterceptor(overlayEngine, captureService, valueResolver);
    }

    @Bean
    @ConditionalOnBean(AgentOverlayHttpInterceptor.class)
    public WebMvcConfigurer agentOverlayWebMvcConfigurer(AgentOverlayHttpInterceptor interceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor);
            }
        };
    }

    private static boolean isCandidateApplicationBean(Class<?> targetClass) {
        if (targetClass == null) {
            return false;
        }
        String className = targetClass.getName();
        if (className.startsWith("java.")
                || className.startsWith("jakarta.")
                || className.startsWith("org.springframework.")
                || className.startsWith("io.springlens.")) {
            return false;
        }
        return AnnotatedElementUtils.hasAnnotation(targetClass, Component.class)
                || AnnotatedElementUtils.hasAnnotation(targetClass, Service.class)
                || AnnotatedElementUtils.hasAnnotation(targetClass, Repository.class)
                || AnnotatedElementUtils.hasAnnotation(targetClass, Controller.class)
                || AnnotatedElementUtils.hasAnnotation(targetClass, RestController.class)
                || AnnotatedElementUtils.hasAnnotation(targetClass, ControllerAdvice.class);
    }
}
