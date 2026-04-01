package io.springlens.server.tool;

import io.springlens.server.controlplane.patch.PatchDraftRegistryService;
import io.springlens.spi.DiagnosticTool;
import io.springlens.spi.ToolDescriptor;
import io.springlens.spi.ToolMetadata;
import io.springlens.spi.ToolRequest;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ListPatchDraftsTool implements DiagnosticTool {

    private final PatchDraftRegistryService patchDraftRegistryService;

    public ListPatchDraftsTool(PatchDraftRegistryService patchDraftRegistryService) {
        this.patchDraftRegistryService = patchDraftRegistryService;
    }

    @Override
    public ToolDescriptor descriptor() {
        return new ToolDescriptor("list_patch_drafts", "List patch drafts currently registered in the control plane.");
    }

    @Override
    public ToolMetadata metadata() {
        ToolDescriptor descriptor = descriptor();
        return ToolMetadata.publiclyExposed(descriptor.name(), descriptor.description());
    }

    @Override
    public Map<String, Object> inputSchema() {
        return ToolJsonSchemas.objectSchema(Map.of(), List.of());
    }

    @Override
    public Object execute(ToolRequest request) {
        return patchDraftRegistryService.list();
    }
}
