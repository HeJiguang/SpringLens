package io.springlens.server.registry;

import io.springlens.model.AppRegistration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class ApplicationRegistryService {

    private final Map<String, Map<String, AppRegistration>> registrations = new ConcurrentHashMap<>();

    public AppRegistration register(AppRegistration registration) {
        AppRegistration normalized = new AppRegistration(
                registration.applicationId(),
                registration.instanceId(),
                registration.runtimeBaseUrl(),
                registration.registeredAt() == null ? Instant.now() : registration.registeredAt(),
                registration.capabilities()
        );
        registrations
                .computeIfAbsent(normalized.applicationId(), ignored -> new ConcurrentHashMap<>())
                .put(normalized.instanceId(), normalized);
        return normalized;
    }

    public List<AppRegistration> list() {
        return registrations.values().stream()
                .flatMap(instances -> instances.values().stream())
                .sorted(Comparator.comparing(AppRegistration::applicationId).thenComparing(AppRegistration::instanceId))
                .toList();
    }

    public AppRegistration resolve(String applicationId, String instanceId) {
        Map<String, AppRegistration> instances = registrations.get(applicationId);
        if (instances == null || instances.isEmpty()) {
            throw new IllegalArgumentException("No application registered for " + applicationId);
        }
        if (instanceId != null && !instanceId.isBlank()) {
            AppRegistration registration = instances.get(instanceId);
            if (registration == null) {
                throw new IllegalArgumentException("No instance " + instanceId + " registered for " + applicationId);
            }
            return registration;
        }
        return instances.values().stream()
                .max(Comparator.comparing(AppRegistration::registeredAt))
                .orElseThrow(() -> new IllegalArgumentException("No active instances for " + applicationId));
    }
}
