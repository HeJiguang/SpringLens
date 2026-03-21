package io.springlens.server.registry;

import io.springlens.model.AppRegistration;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/apps")
public class RegistrationController {

    private final ApplicationRegistryService registryService;

    public RegistrationController(ApplicationRegistryService registryService) {
        this.registryService = registryService;
    }

    @PostMapping("/register")
    public AppRegistration register(@RequestBody AppRegistration registration) {
        return registryService.register(registration);
    }

    @PostMapping("/heartbeat")
    public AppRegistration heartbeat(@RequestBody AppRegistration registration) {
        return registryService.register(registration);
    }
}
