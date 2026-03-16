package org.thingsboard.ai.mcp.server.tools.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.annotation.PeOnly;
import org.thingsboard.ai.mcp.server.annotation.ToolGroup;
import org.thingsboard.ai.mcp.server.data.ThingsBoardEdition;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.McpTools;
import org.thingsboard.ai.mcp.server.util.JsonUtils;
import org.thingsboard.client.model.User;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ALARM_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.CUSTOMER_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_GROUP_IDS_CREATE_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_GROUP_ID_CREATE_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_GROUP_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PE_ONLY_AVAILABLE;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.TENANT_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.USER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.util.ToolUtils.parseIntOrDefault;
import static org.thingsboard.ai.mcp.server.util.ToolUtils.sanitizeStringParam;

@Service
@RequiredArgsConstructor
@ToolGroup("user")
public class UserTools implements McpTools {

    private static final String USER_JSON_EXAMPLE = """
            {
              "email": "user@example.com",
              "authority": "TENANT_ADMIN",
              "firstName": "John",
              "lastName": "Doe"
            }""";

    private final RestClientService clientService;

    @Tool(description = "Use this to create or update a user. Omit 'id' to create; include 'id' to update. Required: 'email' (unique per tenant), 'authority' (SYS_ADMIN|TENANT_ADMIN|CUSTOMER_USER). PE: use 'entityGroupId'/'entityGroupIds' for groups.")
    public String saveUser(
            @ToolParam(description = "JSON user object. Omit 'id' to create; include 'id' to update. " + USER_JSON_EXAMPLE)
            @NotBlank @Valid String userJson,
            @ToolParam(required = false, description = "Send activation email (or use activation link)")
            Boolean sendActivationEmail,
            @ToolParam(required = false, description = "(PE only) " + ENTITY_GROUP_ID_CREATE_PARAM_DESCRIPTION)
            @NotBlank String entityGroupId,
            @ToolParam(required = false, description = "(PE only) " + ENTITY_GROUP_IDS_CREATE_PARAM_DESCRIPTION)
            @NotBlank String entityGroupIds) {
        sendActivationEmail = sendActivationEmail == null || sendActivationEmail;
        User user = JsonUtils.fromString(userJson, User.class);
        String sendActivationMailStr = String.valueOf(sendActivationEmail);
        if (StringUtils.isNotBlank(entityGroupId)) {
            return JsonUtils.toString(clientService.getClient().saveUser(user, sendActivationMailStr, entityGroupId, null));
        } else if (StringUtils.isNotBlank(entityGroupIds)) {
            List<String> groupIdsList = Arrays.asList(entityGroupIds.split(","));
            return JsonUtils.toString(clientService.getClient().saveUser(user, sendActivationMailStr, null, groupIdsList));
        } else {
            return JsonUtils.toString(clientService.getClient().saveUser(user, sendActivationMailStr, null, null));
        }
    }

    @Tool(description = "Use this to permanently delete a user by id.")
    public String deleteUser(@ToolParam(description = USER_ID_PARAM_DESCRIPTION) @NotBlank @Valid String userIdStr) {
        try {
            clientService.getClient().deleteUser(userIdStr);
            return "{\"status\":\"OK\",\"id\":\"" + userIdStr + "\"}";
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("id", userIdStr);
            err.put("message", e.getMessage());
            return JsonUtils.toString(err);
        }
    }

    @Tool(description = "Use this to get a user by id.")
    public String getUserById(@ToolParam(description = USER_ID_PARAM_DESCRIPTION) String userId) {
        return JsonUtils.toString(clientService.getClient().getUserById(userId));
    }

    @Tool(description = "Use this to get a paginated list of users. Scope depends on caller's authority.")
    public String getUsers(
            @ToolParam(required = false, description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(required = false, description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = CUSTOMER_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'firstName', 'lastName', 'email'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        return JsonUtils.toString(clientService.getClient().getAllCustomerUsers(
                parseIntOrDefault(pageSize, 10),
                parseIntOrDefault(page, 0),
                sanitizeStringParam(textSearch),
                sanitizeStringParam(sortProperty),
                sanitizeStringParam(sortOrder)));
    }

    @Tool(description = "Use this to get a paginated list of tenant administrator users for a specified tenant.")
    public String getTenantAdmins(
            @ToolParam(description = TENANT_ID_PARAM_DESCRIPTION) @NotBlank String tenantId,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = CUSTOMER_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'firstName', 'lastName', 'email'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        return JsonUtils.toString(clientService.getClient().getTenantAdmins(
                tenantId,
                parseIntOrDefault(pageSize, 10),
                parseIntOrDefault(page, 0),
                sanitizeStringParam(textSearch),
                sanitizeStringParam(sortProperty),
                sanitizeStringParam(sortOrder)));
    }

    @Tool(description = "Use this to get a paginated list of users assigned to a specific customer.")
    public String getCustomerUsers(
            @ToolParam(description = CUSTOMER_ID_PARAM_DESCRIPTION) @NotBlank String customerId,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = CUSTOMER_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'firstName', 'lastName', 'email'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        return JsonUtils.toString(clientService.getClient().getCustomerUsers(
                customerId,
                parseIntOrDefault(pageSize, 10),
                parseIntOrDefault(page, 0),
                sanitizeStringParam(textSearch),
                sanitizeStringParam(sortProperty),
                sanitizeStringParam(sortOrder)));
    }

    @PeOnly
    @Tool(description = "Use this to get a paginated list of all customer users for the current tenant. PE only.")
    public String getAllCustomerUsers(
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = CUSTOMER_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'firstName', 'lastName', 'email'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        if (ThingsBoardEdition.CE == clientService.getEdition()) {
            return PE_ONLY_AVAILABLE;
        }
        return JsonUtils.toString(clientService.getClient().getAllCustomerUsers(
                parseIntOrDefault(pageSize, 10),
                parseIntOrDefault(page, 0),
                sanitizeStringParam(textSearch),
                sanitizeStringParam(sortProperty),
                sanitizeStringParam(sortOrder)));
    }

    @Tool(description = "Use this to get users that can be assigned to a specific alarm. Searches by email, firstName, lastName.")
    public String getUsersForAssign(
            @ToolParam(description = ALARM_ID_PARAM_DESCRIPTION) @NotBlank String alarmId,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = CUSTOMER_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'firstName', 'lastName', 'email'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        return JsonUtils.toString(clientService.getClient().getUsersForAssign(
                alarmId,
                parseIntOrDefault(pageSize, 10),
                parseIntOrDefault(page, 0),
                sanitizeStringParam(textSearch),
                sanitizeStringParam(sortProperty),
                sanitizeStringParam(sortOrder)));
    }

    @PeOnly
    @Tool(description = "Use this to get a paginated list of users in a specific entity group. PE only.")
    public String getUsersByEntityGroupId(
            @ToolParam(description = ENTITY_GROUP_ID_PARAM_DESCRIPTION) @NotBlank String entityGroupId,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = CUSTOMER_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'firstName', 'lastName', 'email'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        if (ThingsBoardEdition.CE == clientService.getEdition()) {
            return PE_ONLY_AVAILABLE;
        }
        return JsonUtils.toString(clientService.getClient().getUsersByEntityGroupId(
                entityGroupId,
                parseIntOrDefault(pageSize, 10),
                parseIntOrDefault(page, 0),
                sanitizeStringParam(textSearch),
                sanitizeStringParam(sortProperty),
                sanitizeStringParam(sortOrder)));
    }

}
