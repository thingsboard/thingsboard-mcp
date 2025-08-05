package org.thingsboard.ai.mcp.server.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.UUID;

import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

@Service
public class UserTools {

    @Autowired
    private RestClientService clientService;

    @Tool(description = "Returns a page of users assigned to the specified customer. " +
            PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getCustomerUsers(String customerId, int pageSize, int page, String textSearch) {
        var result = clientService.getClient().getCustomerUsers(new CustomerId(UUID.fromString(customerId)), new PageLink(pageSize, page, textSearch));
        return JacksonUtil.toString(result.getData());
    }

    @Tool(description = "Returns a page of tenant administrator users assigned to the specified tenant. " +
            PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    public String getTenantAdmins(String tenantId, int pageSize, int page, String textSearch) {
        var result = clientService.getClient().getTenantAdmins(new TenantId(UUID.fromString(tenantId)), new PageLink(pageSize, page, textSearch));
        return JacksonUtil.toString(result.getData());
    }

    @Tool(description = "Fetch the User object based on the provided User Id. " + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getUserById(String userId) {
        return JacksonUtil.toString(clientService.getClient().getUserById(new UserId(UUID.fromString(userId))));
    }

    @Tool(description = "Returns a page of users owned by the current tenant. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    public String getUsers(int pageSize, int page, String textSearch) {
        var result = clientService.getClient().getUsers(new PageLink(pageSize, page, textSearch));
        return JacksonUtil.toString(result.getData());
    }
}
