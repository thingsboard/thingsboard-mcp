package org.thingsboard.ai.mcp.server.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;

import java.util.Optional;

@Service
public class AdminTools {

    @Autowired
    private RestClientService clientService;

    /**
     * Retrieves admin settings by key.
     *
     * @param key the settings key
     * @return JSON string representation of the settings or "Not found"
     */
    @Tool(description = "Get admin settings by key")
    public String getAdminSettings(String key) {
        Optional<AdminSettings> settings = clientService.getClient().getAdminSettings(key);
        return settings.map(JacksonUtil::toString).orElse("Not found");
    }


    /**
     * Checks if there are available updates in ThingsBoard.
     *
     * @return JSON string with update info or "No updates available"
     */
    @Tool(description = "Check for ThingsBoard updates")
    public String checkUpdates() {
        return clientService.getClient().checkUpdates().map(JacksonUtil::toString).orElse("No updates available");
    }

    /**
     * Retrieves security settings.
     *
     * @return JSON string of the settings or "Not found"
     */
    @Tool(description = "Get security settings")
    public String getSecuritySettings() {
        return clientService.getClient().getSecuritySettings().map(JacksonUtil::toString).orElse("Not found");
    }

    /**
     * Retrieves JWT settings.
     *
     * @return JSON string or "Not found"
     */
    @Tool(description = "Get JWT settings")
    public String getJwtSettings() {
        return clientService.getClient().getJwtSettings().map(JacksonUtil::toString).orElse("Not found");
    }

    /**
     * Retrieves system information.
     *
     * @return JSON string with system info
     */
    @Tool(description = "Get system info")
    public String getSystemInfo() {
        return JacksonUtil.toString(clientService.getClient().getSystemInfo());
    }


    /**
     * Retrieves current usage info for the platform.
     *
     * @return JSON string of usage data
     */
    @Tool(description = "Get usage info")
    public String getUsageInfo() {
        return JacksonUtil.toString(clientService.getClient().getUsageInfo());
    }
}
