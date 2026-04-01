package io.springlens.agent.starter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.springlens.agent.contract.AgentInstrumentationMode;
import io.springlens.starter.LensRuntimeProperties;
import io.springlens.starter.probe.LensProbeCaptureService;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

class AgentInstrumentationAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AgentInstrumentationAutoConfiguration.class))
            .withBean(LensRuntimeProperties.class, () -> {
                LensRuntimeProperties properties = new LensRuntimeProperties();
                properties.setApplicationId("orders-app");
                properties.setInstanceId("orders-1");
                properties.setServerUrl("http://localhost:8090");
                return properties;
            })
            .withBean(RestClient.Builder.class, RestClient::builder);

    @Test
    void registersOverlayBeansWhenEnabled() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AgentInstrumentationProperties.class);
            assertThat(context).hasSingleBean(AgentOverlayEngine.class);
            assertThat(context).hasSingleBean(AgentOverlayControlClient.class);
            assertThat(context).hasSingleBean(AgentOverlaySyncService.class);
            assertThat(context).hasSingleBean(ScheduledExecutorService.class);
            assertThat(context.getBean(AgentOverlayEngine.class).mode()).isEqualTo(AgentInstrumentationMode.HYBRID_APPROVAL);
            assertThat(context.getBean(AgentOverlayEngine.class).isEnabled()).isTrue();
        });
    }

    @Test
    void skipsOverlayEngineWhenDisabled() {
        contextRunner.withPropertyValues("spring.lens.agent.instrumentation.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(AgentInstrumentationProperties.class);
                    assertThat(context).doesNotHaveBean(AgentOverlayEngine.class);
                    assertThat(context).doesNotHaveBean(AgentOverlayControlClient.class);
                    assertThat(context).doesNotHaveBean(AgentOverlaySyncService.class);
                });
    }

    @Test
    void agentLensScopeCarriesOverlayIdentity() {
        AgentLens.AgentProbe probe = AgentLens.agent("ovl-order-status")
                .look("order.status", "PAID", "Status after submit");

        assertThat(probe.overlayId()).isEqualTo("ovl-order-status");
        assertThat(probe.probeId()).isEqualTo("order.status");
        assertThat(probe.description()).isEqualTo("Status after submit");
    }

    @Test
    void keepsSyncServiceButSkipsDedicatedRefreshExecutorWhenPeriodicRefreshIsDisabled() {
        contextRunner.withPropertyValues("spring.lens.agent.instrumentation.periodic-refresh-enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(AgentOverlayEngine.class);
                    assertThat(context).hasSingleBean(AgentOverlayControlClient.class);
                    assertThat(context).hasSingleBean(AgentOverlaySyncService.class);
                    assertThat(context).doesNotHaveBean(ScheduledExecutorService.class);
                });
    }

    @Test
    void keepsSyncServiceWhenStartupSyncIsDisabled() {
        contextRunner.withPropertyValues("spring.lens.agent.instrumentation.startup-sync-enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(AgentOverlayEngine.class);
                    assertThat(context).hasSingleBean(AgentOverlayControlClient.class);
                    assertThat(context).hasSingleBean(AgentOverlaySyncService.class);
                });
    }

    @Test
    void registersOverlayActivationBeansWhenProbeCaptureServiceExists() {
        contextRunner.withBean(LensProbeCaptureService.class, () -> mock(LensProbeCaptureService.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(AgentOverlayValueResolver.class);
                    assertThat(context).hasSingleBean(AgentOverlayMethodInterceptor.class);
                    assertThat(context).hasSingleBean(AgentOverlayHttpInterceptor.class);
                    assertThat(context).hasBean("agentOverlayMethodAdvisor");
                    assertThat(context).hasBean("agentOverlayWebMvcConfigurer");
                });
    }
}
