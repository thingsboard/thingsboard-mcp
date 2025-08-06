package org.thingsboard.ai.mcp.server.tools;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.UUID;

import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.ALARM_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.CUSTOMER_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.TENANT_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.USER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ToolUtils.createPageLink;

@Service
@RequiredArgsConstructor
public class UserTools {

    private final RestClientService clientService;

    @Tool(description = "Fetch the User object based on the provided User Id. " +
            "If the user has the authority of 'SYS_ADMIN', the server does not perform additional checks. " +
            "If the user has the authority of 'TENANT_ADMIN', the server checks that the requested user is owned by the same tenant. " +
            "If the user has the authority of 'CUSTOMER_USER', the server checks that the requested user is owned by the same customer. ")
    public String getUserById(@ToolParam(description = USER_ID_PARAM_DESCRIPTION) String userId) {
        return JacksonUtil.toString(clientService.getClient().getUserById(new UserId(UUID.fromString(userId))));
    }

    @Tool(description = "Checks that the system is configured to allow administrators to impersonate themself as other users. " +
            "If the user has the authority of 'SYS_ADMIN', the server does not perform additional checks. " +
            "If the user has the authority of 'TENANT_ADMIN', the server checks that the requested user is owned by the same tenant. " +
            "If the user has the authority of 'CUSTOMER_USER', the server checks that the requested user is owned by the same customer. ")
    public Boolean isUserTokenAccessEnabled() {
        return clientService.getClient().isUserTokenAccessEnabled();
    }

    @Tool(description = "Returns the token of the User based on the provided User Id. " +
            "If the user has the authority of 'SYS_ADMIN', the server does not perform additional checks. " +
            "If the user has the authority of 'TENANT_ADMIN', the server checks that the requested user is owned by the same tenant. " +
            "If the user has the authority of 'CUSTOMER_USER', the server checks that the requested user is owned by the same customer. ")
    public String getUserToken(@ToolParam(description = USER_ID_PARAM_DESCRIPTION) String userId) {
        return JacksonUtil.toString(clientService.getClient().getUserToken(new UserId(UUID.fromString(userId))));
    }

    @Tool(description = "Get the activation link for the user. The base url for activation link is configurable in the general settings of system administrator." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    public String getActivationLink(@ToolParam(description = USER_ID_PARAM_DESCRIPTION) String userId) {
        return JacksonUtil.toString(clientService.getClient().getActivationLink(new UserId(UUID.fromString(userId))));
    }

    @Tool(description = "Returns a page of users owned by tenant or customer. The scope depends on authority of the user that performs the request. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    public String getUsers(
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) int pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) int page,
            @ToolParam(required = false, description = CUSTOMER_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'firstName', 'lastName', 'email'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return JacksonUtil.toString(clientService.getClient().getUsers(pageLink).getData());
    }

    @Tool(description = "Returns a page of tenant administrator users assigned to the specified tenant. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    public String getTenantAdmins(
            @ToolParam(description = TENANT_ID_PARAM_DESCRIPTION) String tenantId,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) int pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) int page,
            @ToolParam(required = false, description = CUSTOMER_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'firstName', 'lastName', 'email'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return JacksonUtil.toString(clientService.getClient().getTenantAdmins(TenantId.fromUUID(UUID.fromString(tenantId)), pageLink).getData());
    }

    @Tool(description = "Returns a page of users assigned to the specified customer. " +
            PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getCustomerUsers(
            @ToolParam(description = CUSTOMER_ID_PARAM_DESCRIPTION) String customerId,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) int pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) int page,
            @ToolParam(required = false, description = CUSTOMER_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'firstName', 'lastName', 'email'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return JacksonUtil.toString(clientService.getClient().getCustomerUsers(new CustomerId(UUID.fromString(customerId)), pageLink).getData());
    }

    @Tool(description = "Returns page of user data objects that can be assigned to provided alarmId. Search is been executed by email, firstName and lastName fields. " +
            PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getUsersForAssign(
            @ToolParam(description = ALARM_ID_PARAM_DESCRIPTION) String alarmId,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) int pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) int page,
            @ToolParam(required = false, description = CUSTOMER_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'firstName', 'lastName', 'email'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return JacksonUtil.toString(clientService.getClient().getUsersForAssign(new AlarmId(UUID.fromString(alarmId)), pageLink).getData());
    }

}
