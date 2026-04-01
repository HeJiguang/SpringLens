package io.springlens.server.tool;

import io.springlens.server.controlplane.audit.AuditTrailService;
import io.springlens.spi.DiagnosticTool;
import io.springlens.spi.ToolDescriptor;
import io.springlens.spi.ToolMetadata;
import io.springlens.spi.ToolRequest;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ListAuditEventsTool implements DiagnosticTool {

    private final AuditTrailService auditTrailService;

    public ListAuditEventsTool(AuditTrailService auditTrailService) {
        this.auditTrailService = auditTrailService;
    }

    @Override
    public ToolDescriptor descriptor() {
        return new ToolDescriptor("list_audit_events", "List recorded agent control-plane audit events.");
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
        return auditTrailService.list();
    }
}
