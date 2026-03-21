package io.springlens.server.tool;

import io.springlens.server.playbook.PlaybookRegistry;
import io.springlens.spi.DiagnosticPlaybook;
import io.springlens.spi.DiagnosticTool;
import io.springlens.spi.ToolDescriptor;
import io.springlens.spi.ToolMetadata;
import io.springlens.spi.ToolRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GetDiagnosticPlaybookTool implements DiagnosticTool {

    private final PlaybookRegistry playbookRegistry;

    public GetDiagnosticPlaybookTool(PlaybookRegistry playbookRegistry) {
        this.playbookRegistry = playbookRegistry;
    }

    @Override
    public ToolDescriptor descriptor() {
        return new ToolDescriptor("get_diagnostic_playbook", "Fetch a diagnostic playbook by problem id.");
    }

    @Override
    public ToolMetadata metadata() {
        ToolDescriptor descriptor = descriptor();
        return ToolMetadata.publiclyExposed(descriptor.name(), descriptor.description());
    }

    @Override
    public Map<String, Object> inputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("playbookId", ToolJsonSchemas.stringProperty("Optional playbook id."));
        return ToolJsonSchemas.objectSchema(properties, List.of());
    }

    @Override
    public Object execute(ToolRequest request) {
        Object playbookId = request.arguments().get("playbookId");
        if (playbookId == null) {
            return playbookRegistry.list().stream().map(this::toView).toList();
        }
        return toView(playbookRegistry.get(String.valueOf(playbookId)));
    }

    private Map<String, Object> toView(DiagnosticPlaybook playbook) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", playbook.id());
        view.put("problem", playbook.problem());
        view.put("steps", playbook.steps());
        return view;
    }
}
