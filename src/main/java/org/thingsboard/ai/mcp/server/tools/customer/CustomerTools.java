package org.thingsboard.ai.mcp.server.tools.customer;

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
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.ai.mcp.server.util.ToolUtils.createPageLink;

@Service
@RequiredArgsConstructor
public class CustomerTools implements McpTools {

    private static final String CUSTOMER_JSON_EXAMPLE = """
            ```json
            {
              "id": { "entityType": "CUSTOMER", "id": "d3cfc080-a295-11f0-848c-93db0ade7d93" },
              "title": "Room-234",
              "label": "Room 234 Sensor",
              "type": "building-zone",
              "assetProfileId": { "entityType": "ASSET_PROFILE", "id": "716a92d0-9d36-11f0-a79c-e726b4e8048a" }
            }
            ```
            """;

    private final RestClientService clientService;

    private static final String CUSTOMER_SECURITY_CHECK = "If the user has the authority of 'Tenant Administrator', the server checks that the customer is owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the user belongs to the customer.";

    @Tool(description =
            "Create or update a Customer. Remove 'id', 'tenantId' from the request body to create new Customer entity. " +
                    "If 'id' is provided, the existing Customer is updated. Asset names must be unique within a tenant \n" +

                    "### Platform Edition:\n" +
                    "- In ThingsBoard PE, you can also attach the Customer to an entity group using the `entityGroupId` or to multiple groups" +
                    "using `entityGroupIds` parameter.\n" +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String saveCustomer(
            @ToolParam(description = "A JSON string representing the Customer entity. Omit 'id' to create a new Customer; include 'id' to update an existing one." + CUSTOMER_JSON_EXAMPLE)
            @NotBlank @Valid String customerJson,
            @ToolParam(required = false, description = "(PE only) " + ENTITY_GROUP_ID_CREATE_PARAM_DESCRIPTION)
            @NotBlank String entityGroupId,
            @ToolParam(required = false, description = "(PE only) " + ENTITY_GROUP_IDS_CREATE_PARAM_DESCRIPTION)
            @NotBlank String entityGroupIds) {
        Customer customer = JacksonUtil.fromString(customerJson, Customer.class);
        if (StringUtils.isNotBlank(entityGroupId)) {
            return JacksonUtil.toString(clientService.getClient().saveCustomer(customer, new EntityGroupId(UUID.fromString(entityGroupId)), null));
        } else if (StringUtils.isNotBlank(entityGroupIds)) {
            return JacksonUtil.toString(clientService.getClient().saveCustomer(customer, null, entityGroupIds));
        } else {
            return JacksonUtil.toString(clientService.getClient().saveCustomer(customer));
        }
    }

    @Tool(description = "Delete the customer. Deletes the customer and all customer users. All assigned dashboards, assets, devices, etc will be unassigned, but not deleted" +
            "Referencing non-existing asset Id will cause an error. " + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String deleteCustomer(@ToolParam(description = CUSTOMER_ID_PARAM_DESCRIPTION) @NotBlank String customerIdStr) {
        try {
            CustomerId customerId = new CustomerId(UUID.fromString(customerIdStr));
            clientService.getClient().deleteCustomer(customerId);
            return "{\"status\":\"OK\",\"id\":\"" + customerId + "\"}";
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("id", customerIdStr);
            err.put("message", e.getMessage());
            return JacksonUtil.toString(err);
        }
    }

    @Tool(description = "Get the Customer object based on the provided Customer Id. " + CUSTOMER_SECURITY_CHECK + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getCustomerById(@ToolParam(description = CUSTOMER_ID_PARAM_DESCRIPTION) @NotBlank String customerId) {
        return JacksonUtil.toString(clientService.getClient().getCustomerById(new CustomerId(UUID.fromString(customerId))));
    }

    @Tool(description = "Returns a page of customers owned by tenant. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    public String getCustomers(
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = CUSTOMER_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'title', 'email', 'country', 'city'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return JacksonUtil.toString(clientService.getClient().getCustomers(pageLink));
    }

    @Tool(description = "Get the Customer using Customer Title. " + TENANT_AUTHORITY_PARAGRAPH)
    public String getTenantCustomer(@ToolParam(description = "A string value representing the Customer title.") @NotBlank String customerTitle) {
        return JacksonUtil.toString(clientService.getClient().getTenantCustomer(customerTitle));
    }

    @PeOnly
    @Tool(description = "Returns a page of customers available for the user. " + PE_ONLY_AVAILABLE + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    public String getUserCustomers(
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = CUSTOMER_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'title', 'email', 'country', 'city'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        if (ThingsBoardEdition.CE == clientService.getEdition()) {
            return PE_ONLY_AVAILABLE;
        }
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return JacksonUtil.toString(clientService.getClient().getUserCustomers(pageLink));
    }

    @PeOnly
    @Tool(description = "Returns a page of Customer objects that belongs to specified Entity Group Id. " + PE_ONLY_AVAILABLE + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_GROUP_READ_CHECK)
    public String getCustomersByEntityGroupId(
            @ToolParam(description = ENTITY_GROUP_ID_PARAM_DESCRIPTION) @NotBlank String entityGroupId,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = CUSTOMER_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'title', 'email', 'country', 'city'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        if (ThingsBoardEdition.CE == clientService.getEdition()) {
            return PE_ONLY_AVAILABLE;
        }
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return JacksonUtil.toString(clientService.getClient().getCustomersByEntityGroupId(new EntityGroupId(UUID.fromString(entityGroupId)), pageLink));
    }

}
