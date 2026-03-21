package io.springlens.starter;

import io.springlens.runtime.ExceptionCollector;
import io.springlens.runtime.HttpRequestCollector;
import io.springlens.runtime.InMemoryExecutionGraphStore;
import io.springlens.runtime.JdbcSlowSqlCollector;
import io.springlens.runtime.ProbeValueCollector;
import io.springlens.runtime.RuntimeSignalProcessor;
import io.springlens.spi.DefaultDiagnosticEngine;
import io.springlens.spi.DiagnosticEngine;
import io.springlens.spi.DiagnosticEngineSelectionStrategy;
import io.springlens.spi.PriorityDiagnosticEngineSelectionStrategy;
import io.springlens.spi.RuntimeCollector;
import io.springlens.spi.RoutingDiagnosticEngine;
import io.springlens.spi.SelectableDiagnosticEngine;
import io.springlens.spi.SkillGenerator;
import io.springlens.starter.probe.LensDiagnosticTool;
import io.springlens.starter.probe.DefaultSkillGenerator;
import io.springlens.starter.probe.LensProbeAspect;
import io.springlens.starter.probe.LensProbeCaptureService;
import io.springlens.starter.probe.LensProbeRegistry;
import io.springlens.starter.probe.LensProjectToolRegistry;
import io.springlens.starter.probe.LensValueSanitizer;
import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

/**
 * Spring Lens 运行时核心自动装配 (Auto Configuration)。
 * 当 Spring Boot 启动时，它会自动发现并组装前面所有的发动机 (Processor, Collectors) 和数据存储 (Store)。
 * 可以通过 spring.lens.enabled=false 来一键关闭所有功能。
 */
@AutoConfiguration
@ConditionalOnClass(RuntimeSignalProcessor.class)
@ConditionalOnProperty(prefix = "spring.lens", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(LensRuntimeProperties.class)
public class LensRuntimeAutoConfiguration {

    @Bean
    public InMemoryExecutionGraphStore executionGraphStore(LensRuntimeProperties properties) {
        return new InMemoryExecutionGraphStore(properties.getMaxCompletedGraphs());
    }

    // ==========================================
    // 注册内置收集器 (Built-in Collectors)
    // ==========================================

    @Bean
    public HttpRequestCollector httpRequestCollector() {
        return new HttpRequestCollector();
    }

    @Bean
    public JdbcSlowSqlCollector jdbcSlowSqlCollector() {
        return new JdbcSlowSqlCollector();
    }

    @Bean
    public ExceptionCollector exceptionCollector() {
        return new ExceptionCollector();
    }

    @Bean
    public ProbeValueCollector probeValueCollector() {
        return new ProbeValueCollector();
    }

    /**
     * 将所有的 RuntimeCollector (包括内置的和用户自定义的) 收集起来，塞进中央处理器。
     */
    @Bean
    public RuntimeSignalProcessor runtimeSignalProcessor(
            InMemoryExecutionGraphStore graphStore,
            List<RuntimeCollector> runtimeCollectors
    ) {
        return new RuntimeSignalProcessor(graphStore, runtimeCollectors);
    }

    // ==========================================
    // 线程上下文与拦截器切面
    // ==========================================

    @Bean
    public RuntimeExecutionContextHolder runtimeExecutionContextHolder() {
        return new RuntimeExecutionContextHolder();
    }

    @Bean
    public LensRequestFilter lensRequestFilter(
            RuntimeSignalProcessor signalProcessor,
            RuntimeExecutionContextHolder contextHolder,
            LensRuntimeProperties properties,
            Environment environment
    ) {
        return new LensRequestFilter(signalProcessor, contextHolder, properties, environment);
    }

    @Bean
    public LensExceptionInterceptor lensExceptionInterceptor(
            RuntimeSignalProcessor signalProcessor,
            RuntimeExecutionContextHolder contextHolder
    ) {
        return new LensExceptionInterceptor(signalProcessor, contextHolder);
    }

    @Bean
    public RuntimeWebMvcConfiguration runtimeWebMvcConfiguration(LensExceptionInterceptor exceptionInterceptor) {
        return new RuntimeWebMvcConfiguration(exceptionInterceptor);
    }

    @Bean
    public JdbcObservationAspect jdbcObservationAspect(
            RuntimeSignalProcessor signalProcessor,
            RuntimeExecutionContextHolder contextHolder,
            LensRuntimeProperties properties
    ) {
        return new JdbcObservationAspect(signalProcessor, contextHolder, properties);
    }

    // ==========================================
    // 探针(Probe) 与 工具(Tool) 机制
    // ==========================================

    @Bean
    public LensProbeRegistry lensProbeRegistry(ApplicationContext applicationContext) {
        return new LensProbeRegistry(applicationContext);
    }

    @Bean
    public LensValueSanitizer lensValueSanitizer(ObjectMapper objectMapper) {
        return new LensValueSanitizer(objectMapper);
    }

    @Bean
    public LensProbeCaptureService lensProbeCaptureService(
            RuntimeSignalProcessor signalProcessor,
            RuntimeExecutionContextHolder contextHolder,
            LensProbeRegistry probeRegistry,
            LensValueSanitizer valueSanitizer
    ) {
        return new LensProbeCaptureService(signalProcessor, contextHolder, probeRegistry, valueSanitizer);
    }

    @Bean
    public LensProbeAspect lensProbeAspect(LensProbeCaptureService captureService) {
        return new LensProbeAspect(captureService);
    }

    @Bean
    @ConditionalOnMissingBean(SkillGenerator.class)
    public SkillGenerator skillGenerator(
            InMemoryExecutionGraphStore graphStore,
            LensRuntimeProperties properties
    ) {
        return new DefaultSkillGenerator(graphStore, properties);
    }

    @Bean
    public LensProjectToolRegistry lensProjectToolRegistry(
            ApplicationContext applicationContext,
            ObjectMapper objectMapper,
            LensProbeRegistry probeRegistry,
            ObjectProvider<RequestMappingHandlerMapping> requestMappingHandlerMapping,
            ObjectProvider<SkillGenerator> skillGenerators
    ) {
        return new LensProjectToolRegistry(
                applicationContext,
                objectMapper,
                probeRegistry,
                requestMappingHandlerMapping.getIfAvailable(),
                skillGenerators.orderedStream().toList()
        );
    }

    @Bean
    @ConditionalOnMissingBean(DefaultDiagnosticEngine.class)
    public SelectableDiagnosticEngine defaultRuleBasedDiagnosticEngine() {
        return new DefaultDiagnosticEngine();
    }

    @Bean
    @ConditionalOnMissingBean(DiagnosticEngineSelectionStrategy.class)
    public DiagnosticEngineSelectionStrategy diagnosticEngineSelectionStrategy() {
        return new PriorityDiagnosticEngineSelectionStrategy();
    }

    @Bean
    @org.springframework.context.annotation.Primary
    @ConditionalOnMissingBean(DiagnosticEngine.class)
    public DiagnosticEngine diagnosticEngine(
            List<SelectableDiagnosticEngine> diagnosticEngines,
            DiagnosticEngineSelectionStrategy selectionStrategy
    ) {
        return new RoutingDiagnosticEngine(diagnosticEngines, selectionStrategy);
    }

    @Bean
    public LensDiagnosticTool lensDiagnosticTool(
            InMemoryExecutionGraphStore graphStore,
            DiagnosticEngine diagnosticEngine
    ) {
        return new LensDiagnosticTool(graphStore, diagnosticEngine);
    }

    // ==========================================
    // 端点暴露与服务端通信
    // ==========================================

    /**
     * 对内嵌的图形数据库和工具调用，包装成 HTTP API 暴露给 Server 或者大模型。
     */
    @Bean
    public RuntimeQueryController runtimeQueryController(
            InMemoryExecutionGraphStore graphStore,
            LensRuntimeProperties properties,
            LensProbeRegistry probeRegistry,
            LensProjectToolRegistry projectToolRegistry,
            Environment environment
    ) {
        if (properties.getApplicationId() == null || properties.getApplicationId().isBlank()) {
            properties.setApplicationId(environment.getProperty("spring.application.name", "spring-lens-app"));
        }
        return new RuntimeQueryController(graphStore, properties, probeRegistry, projectToolRegistry);
    }

    @Bean
    @ConditionalOnMissingBean(RestClient.Builder.class)
    public RestClient.Builder lensRestClientBuilder() {
        return RestClient.builder();
    }

    /**
     * 自动注册客户端，启动后向 Server 报到。
     */
    @Bean
    @ConditionalOnProperty(prefix = "spring.lens", name = "registration-enabled", havingValue = "true", matchIfMissing = true)
    public RuntimeRegistrationClient runtimeRegistrationClient(
            org.springframework.web.client.RestClient.Builder restClientBuilder,
            LensRuntimeProperties properties,
            Environment environment
    ) {
        if (properties.getApplicationId() == null || properties.getApplicationId().isBlank()) {
            properties.setApplicationId(environment.getProperty("spring.application.name", "spring-lens-app"));
        }
        return new RuntimeRegistrationClient(restClientBuilder, properties);
    }
}
