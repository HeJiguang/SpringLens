package io.springlens.demo;

import static org.assertj.core.api.Assertions.assertThat;

import io.springlens.starter.LensRequestFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = SpringLensDemoApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.lens.registration-enabled=false",
                "spring.lens.observation-native-enabled=true",
                "spring.lens.compatibility-instrumentation-enabled=false"
        }
)
class SpringLensObservationNativeIntegrationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void registersObservationBridgeWhenCompatInstrumentationIsDisabled() {
        assertThat(applicationContext.containsBean("lensObservationExecutionBridge")).isTrue();
        assertThat(applicationContext.containsBean("lensObservationHandler")).isTrue();
        assertThat(applicationContext.getBeansOfType(LensRequestFilter.class)).isEmpty();
    }
}
