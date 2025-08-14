package org.thingsboard.ai.mcp.server.tools.health;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.tools.McpTools;
import org.thingsboard.server.common.data.StringUtils;

@Service
public class HealthTools implements McpTools {

    @Tool(description = "Simple ping")
    public String ping(@ToolParam(required = false, description = "Optional text") String text) {
        return StringUtils.isBlank(text) ? "pong" : text;
    }

}
