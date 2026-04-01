package io.springlens.server.controlplane.patch;

import io.springlens.agent.contract.AuditEventType;
import io.springlens.agent.contract.PatchDraftStatus;
import io.springlens.agent.contract.PatchProposalDraft;
import io.springlens.agent.contract.RegisteredPatchDraft;
import io.springlens.server.controlplane.audit.AuditTrailService;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PatchDraftRegistryService {

    private final Clock clock;
    private final AuditTrailService auditTrailService;
    private final ConcurrentHashMap<String, RegisteredPatchDraft> drafts = new ConcurrentHashMap<>();

    @Autowired
    public PatchDraftRegistryService(AuditTrailService auditTrailService) {
        this(Clock.systemUTC(), auditTrailService);
    }

    public PatchDraftRegistryService(Clock clock, AuditTrailService auditTrailService) {
        this.clock = clock;
        this.auditTrailService = auditTrailService;
    }

    public RegisteredPatchDraft submit(PatchProposalDraft draft, String actor) {
        Map<String, String> metadata = new LinkedHashMap<>(draft.metadata());
        metadata.put("actor", actor);
        RegisteredPatchDraft registered = new RegisteredPatchDraft(
                draft.draftId(),
                draft,
                PatchDraftStatus.PENDING,
                Instant.now(clock).toString(),
                null,
                Map.copyOf(metadata)
        );
        drafts.put(registered.draftId(), registered);
        auditTrailService.record(AuditEventType.of("PATCH_DRAFT_SUBMITTED"), actor, registered.draftId(), registered.metadata());
        return registered;
    }

    public List<RegisteredPatchDraft> list() {
        return drafts.values().stream()
                .sorted(Comparator.comparing(RegisteredPatchDraft::draftId))
                .toList();
    }
}
