package org.thingsboard.ai.mcp.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "thingsboard.tools")
public class ToolGroupsProperties {

    /**
     * Map of tool group names to their enabled status.
     * If a group is not specified, it defaults to enabled (true).
     */
    private Map<String, Boolean> groups = new HashMap<>();

    /**
     * Check if a tool group is enabled.
     * Groups not explicitly configured default to enabled.
     */
    public boolean isGroupEnabled(String groupName) {
        return groups.getOrDefault(groupName, true);
    }

}
