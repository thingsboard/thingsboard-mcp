package org.thingsboard.ai.mcp.server.tools.user;

import jakarta.validation.Valid;
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
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ALARM_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.CUSTOMER_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_GROUP_IDS_CREATE_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_GROUP_ID_CREATE_PARAM_DESCRIPTION;
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

    private static final String USER_JSON_EXAMPLE =
            """
                    ```json
                    {
                      "id": { "entityType": "USER", "id": "784f394c-42b6-435a-983c-b7beff2784f9" },
                      "email": "user@example.com",
                      "authority": "TENANT_ADMIN",
                      "firstName": "John",
                      "lastName": "Doe",
                      "phone": "38012345123",
                      "customMenuId": { "id": "784f394c-42b6-435a-983c-b7beff2784f9" },
                      "additionalInfo": {}
                    }
                    ```""";

    private final RestClientService clientService;

    @Tool(description =
            "Create or update a User. Remove 'id', 'tenantId' and 'customerId' from the request body to create a new User. " +
                    "If 'id' is provided, the existing User is updated. \n" +

                    "### Required fields:\n" +
                    "- **email** (unique per tenant)\n" +
                    "- **authority**: one of SYS_ADMIN, TENANT_ADMIN, CUSTOMER_USER\n\n" +

                    "### Platform Edition:\n" +
                    "- In ThingsBoard PE, you can also attach the User to an entity group using the `entityGroupId` parameter or to multiple groups using `entityGroupIds`.\n" +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String saveUser(
            @ToolParam(description = "A JSON string representing the User entity. Omit 'id' to create a new User; include 'id' to update an existing one. " + USER_JSON_EXAMPLE)
            @NotBlank @Valid String userJson,
            @ToolParam(required = false, description = "Send activation email (or use activation link)")
            Boolean sendActivationEmail,
            @ToolParam(required = false, description = "(PE only) " + ENTITY_GROUP_ID_CREATE_PARAM_DESCRIPTION)
            @NotBlank String entityGroupId,
            @ToolParam(required = false, description = "(PE only) " + ENTITY_GROUP_IDS_CREATE_PARAM_DESCRIPTION)
            @NotBlank String entityGroupIds) {
        sendActivationEmail = sendActivationEmail == null || sendActivationEmail;
        User user = JacksonUtil.fromString(userJson, User.class);
        if (StringUtils.isNotBlank(entityGroupId)) {
            return JacksonUtil.toString(clientService.getClient().saveUser(user, sendActivationEmail, new EntityGroupId(UUID.fromString(entityGroupId)), null));
        } else if (StringUtils.isNotBlank(entityGroupIds)) {
            return JacksonUtil.toString(clientService.getClient().saveUser(user, sendActivationEmail, null, entityGroupIds));
        } else {
            return JacksonUtil.toString(clientService.getClient().saveUser(user, sendActivationEmail));
        }
    }

    @Tool(description = "Delete the user. Referencing non-existing User Id will cause an error. " + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String deleteUser(@ToolParam(description = USER_ID_PARAM_DESCRIPTION) @NotBlank @Valid String userIdStr) {
        try {
            UserId userId = new UserId(UUID.fromString(userIdStr));
            clientService.getClient().deleteUser(userId);
            return "{\"status\":\"OK\",\"id\":\"" + userId + "\"}";
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("id", userIdStr);
            err.put("message", e.getMessage());
            return JacksonUtil.toString(err);
        }
    }

    @Tool(description = "Fetch the User object based on the provided User Id. " +
            "If the user has the authority of 'SYS_ADMIN', the server does not perform additional checks. " +
            "If the user has the authority of 'TENANT_ADMIN', the server checks that the requested user is owned by the same tenant. " +
            "If the user has the authority of 'CUSTOMER_USER', the server checks that the requested user is owned by the same customer. ")
    public String getUserById(@ToolParam(description = USER_ID_PARAM_DESCRIPTION) String userId) {
        return JacksonUtil.toString(clientService.getClient().getUserById(new UserId(UUID.fromString(userId))));
    }

    @Tool(description = "Returns a page of users owned by tenant or customer. The scope depends on authority of the user that performs the request. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    public String getUsers(
            @ToolParam(required = false, description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(required = false, description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = CUSTOMER_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'firstName', 'lastName', 'email'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return JacksonUtil.toString(clientService.getClient().getUsers(pageLink));
    }

    @Tool(description = "Returns a page of tenant administrator users assigned to the specified tenant. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    public String getTenantAdmins(
            @ToolParam(description = TENANT_ID_PARAM_DESCRIPTION) @NotBlank String tenantId,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
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
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
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
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
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
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
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
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
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
