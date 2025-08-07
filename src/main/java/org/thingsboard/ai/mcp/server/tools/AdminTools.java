package org.thingsboard.ai.mcp.server.tools;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.common.util.JacksonUtil;

import static org.thingsboard.ai.mcp.server.util.ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH;
import static org.thingsboard.ai.mcp.server.util.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;

@Service
@RequiredArgsConstructor
public class AdminTools {

    private final RestClientService clientService;

    @Tool(description = "Get the Administration Settings object using specified string key. Referencing non-existing key will cause an error. " + SYSTEM_AUTHORITY_PARAGRAPH)
    public String getAdminSettings(@ToolParam(description = "A string value of the key (e.g. 'general', 'mail', etc") String key) {
        return JacksonUtil.toString(clientService.getClient().getAdminSettings(key));
    }

    @Tool(description = "Get the Security settings object that contains password policy, lockout limits, notification email, " +
            "mobile secret key length, and TTL values for activation & password-reset tokens (1–24 hours). " + SYSTEM_AUTHORITY_PARAGRAPH)
    public String getSecuritySettings() {
        return JacksonUtil.toString(clientService.getClient().getSecuritySettings());
    }

    @Tool(description = "Get JWT settings object that contains JWT token policy, etc. " + SYSTEM_AUTHORITY_PARAGRAPH)
    public String getJwtSettings() {
        return JacksonUtil.toString(clientService.getClient().getJwtSettings());
    }

    @Tool(description = "Get the repository settings object for version control. " + TENANT_AUTHORITY_PARAGRAPH)
    public String getRepositorySettings() {
        return JacksonUtil.toString(clientService.getClient().getRepositorySettings());
    }

    @Tool(description = "Check whether the repository settings exists for version control. " + TENANT_AUTHORITY_PARAGRAPH)
    public Boolean repositorySettingsExists() {
        return clientService.getClient().repositorySettingsExists();
    }

    @Tool(description = "Get the auto commit settings object for version control. " + TENANT_AUTHORITY_PARAGRAPH)
    public String getAutoCommitSettings() {
        return JacksonUtil.toString(clientService.getClient().getAutoCommitSettings());
    }

    @Tool(description = "Check whether the auto commit settings exists for version control. " + TENANT_AUTHORITY_PARAGRAPH)
    public Boolean autoCommitSettingsExists() {
        return clientService.getClient().autoCommitSettingsExists();
    }

    @Tool(description = "Check for new ThingsBoard platform releases. " + SYSTEM_AUTHORITY_PARAGRAPH)
    public String checkUpdates() {
        return JacksonUtil.toString(clientService.getClient().checkUpdates());
    }

    @Tool(description = "Get main information about system. " + SYSTEM_AUTHORITY_PARAGRAPH)
    public String getSystemInfo() {
        return JacksonUtil.toString(clientService.getClient().getSystemInfo());
    }

    @Tool(description = "Retrieves usage statistics for the current tenant, including number of devices, assets, customers, users, dashboards, edges, " +
            "transportMessages, jsExecutions, tbelExecutions, emails, sms, alarms. " + TENANT_AUTHORITY_PARAGRAPH)
    public String getUsageInfo() {
        return JacksonUtil.toString(clientService.getClient().getUsageInfo());
    }

}
