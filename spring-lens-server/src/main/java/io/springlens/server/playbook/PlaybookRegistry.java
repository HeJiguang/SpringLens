package io.springlens.server.playbook;

import io.springlens.spi.DiagnosticPlaybook;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class PlaybookRegistry {

    private final Map<String, DiagnosticPlaybook> playbooks = new ConcurrentHashMap<>();

    public PlaybookRegistry(List<DiagnosticPlaybook> playbooks) {
        playbooks.forEach(playbook -> this.playbooks.put(playbook.id(), playbook));
    }

    public List<DiagnosticPlaybook> list() {
        return playbooks.values().stream().sorted((left, right) -> left.id().compareTo(right.id())).toList();
    }

    public DiagnosticPlaybook get(String id) {
        DiagnosticPlaybook playbook = playbooks.get(id);
        if (playbook == null) {
            throw new IllegalArgumentException("No playbook found for " + id);
        }
        return playbook;
    }
}
