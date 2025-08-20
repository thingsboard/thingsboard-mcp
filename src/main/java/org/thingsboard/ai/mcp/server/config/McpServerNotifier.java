package org.thingsboard.ai.mcp.server.config;

import io.modelcontextprotocol.server.McpSyncServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.thingsboard.ai.mcp.server.data.RemoveToolsEvent;

@Slf4j
@Component
public class McpServerNotifier {

    private final McpSyncServer mcpServer;

    private volatile boolean removed = false;

    public McpServerNotifier(McpSyncServer mcpServer) {
        this.mcpServer = mcpServer;
    }

    @EventListener
    public void handleEvent(RemoveToolsEvent event) {
        if (!removed) {
            event.tools().forEach(mcpServer::removeTool);
            removed = true;
        }
    }

}
