package io.springlens.server.controlplane.audit;

import io.springlens.agent.contract.AuditEvent;
import io.springlens.agent.contract.AuditEventType;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class AuditTrailService {

    private final Clock clock;
    private final AtomicLong nextId = new AtomicLong(1);
    private final CopyOnWriteArrayList<AuditEvent> events = new CopyOnWriteArrayList<>();

    public AuditTrailService() {
        this(Clock.systemUTC());
    }

    public AuditTrailService(Clock clock) {
        this.clock = clock;
    }

    public AuditEvent record(AuditEventType eventType, String actor, String targetId, Map<String, String> metadata) {
        AuditEvent event = new AuditEvent(
                "audit-" + nextId.getAndIncrement(),
                eventType,
                Instant.now(clock).toString(),
                actor,
                targetId,
                metadata
        );
        events.add(event);
        return event;
    }

    public List<AuditEvent> list() {
        return List.copyOf(events);
    }
}
