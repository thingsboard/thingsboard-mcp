package org.thingsboard.ai.mcp.server.tools.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.annotation.PeOnly;
import org.thingsboard.ai.mcp.server.data.ThingsBoardEdition;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.McpTools;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.UUID;

import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ALARM_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.CUSTOMER_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_GROUP_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PE_ONLY_AVAILABLE;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.RBAC_GROUP_READ_CHECK;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.TENANT_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.USER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.util.ToolUtils.createPageLink;

@Service
@RequiredArgsConstructor
public class UserTools implements McpTools {

    private final RestClientService clientService;

    @Tool(description = "Fetch the User object based on the provided User Id. " +
            "If the user has the authority of 'SYS_ADMIN', the server does not perform additional checks. " +
            "If the user has the authority of 'TENANT_ADMIN', the server checks that the requested user is owned by the same tenant. " +
            "If the user has the authority of 'CUSTOMER_USER', the server checks that the requested user is owned by the same customer. ")
    public String getUserById(@ToolParam(description = USER_ID_PARAM_DESCRIPTION) String userId) {
        return JacksonUtil.toString(clientService.getClient().getUserById(new UserId(UUID.fromString(userId))));
    }

    @Tool(description = "Returns a page of users owned by tenant or customer. The scope depends on authority of the user that performs the request. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    public String getUsers(
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive int pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero int page,
            @ToolParam(required = false, description = CUSTOMER_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'firstName', 'lastName', 'email'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return JacksonUtil.toString(clientService.getClient().getUsers(pageLink));
    }

    @Tool(description = "Returns a page of tenant administrator users assigned to the specified tenant. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    public String getTenantAdmins(
            @ToolParam(description = TENANT_ID_PARAM_DESCRIPTION) @NotBlank String tenantId,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive int pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero int page,
            @ToolParam(required = false, description = CUSTOMER_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'firstName', 'lastName', 'email'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return JacksonUtil.toString(clientService.getClient().getTenantAdmins(TenantId.fromUUID(UUID.fromString(tenantId)), pageLink));
    }

    @Tool(description = "Returns a page of users assigned to the specified customer. " +
            PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getCustomerUsers(
            @ToolParam(description = CUSTOMER_ID_PARAM_DESCRIPTION) @NotBlank String customerId,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive int pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero int page,
            @ToolParam(required = false, description = CUSTOMER_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'firstName', 'lastName', 'email'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return JacksonUtil.toString(clientService.getClient().getCustomerUsers(new CustomerId(UUID.fromString(customerId)), pageLink));
    }

    @PeOnly
    @Tool(description = "Returns a page of users for the current tenant with authority 'CUSTOMER_USER'. " + PE_ONLY_AVAILABLE +
            PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    public String getAllCustomerUsers(
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive int pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero int page,
            @ToolParam(required = false, description = CUSTOMER_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'firstName', 'lastName', 'email'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        if (ThingsBoardEdition.CE == clientService.getEdition()) {
            return PE_ONLY_AVAILABLE;
        }
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return JacksonUtil.toString(clientService.getClient().getAllCustomerUsers(pageLink));
    }

    @Tool(description = "Returns page of user data objects that can be assigned to provided alarmId. Search is been executed by email, firstName and lastName fields. " +
            PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getUsersForAssign(
            @ToolParam(description = ALARM_ID_PARAM_DESCRIPTION) @NotBlank String alarmId,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive int pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero int page,
            @ToolParam(required = false, description = CUSTOMER_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'firstName', 'lastName', 'email'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return JacksonUtil.toString(clientService.getClient().getUsersForAssign(new AlarmId(UUID.fromString(alarmId)), pageLink));
    }

    @PeOnly
    @Tool(description = "Returns a page of user objects that belongs to specified Entity Group Id. " + PE_ONLY_AVAILABLE + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_GROUP_READ_CHECK)
    public String getUsersByEntityGroupId(
            @ToolParam(description = ENTITY_GROUP_ID_PARAM_DESCRIPTION) @NotBlank String entityGroupId,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive int pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero int page,
            @ToolParam(required = false, description = CUSTOMER_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'firstName', 'lastName', 'email'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        if (ThingsBoardEdition.CE == clientService.getEdition()) {
            return PE_ONLY_AVAILABLE;
        }
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return JacksonUtil.toString(clientService.getClient().getUsersByEntityGroupId(new EntityGroupId(UUID.fromString(entityGroupId)), pageLink));
    }

}
