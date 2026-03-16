package org.thingsboard.ai.mcp.server.tools.customer;

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
import org.thingsboard.ai.mcp.server.util.JacksonUtil;
import org.thingsboard.ai.mcp.server.util.ToolUtils;
import org.thingsboard.client.model.Customer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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

@Service
@RequiredArgsConstructor
@ToolGroup("customer")
public class CustomerTools implements McpTools {

    private static final String CUSTOMER_JSON_EXAMPLE = """
            {
              "title": "Customer A",
              "country": "US",
              "city": "New York"
            }""";

    private final RestClientService clientService;

    @Tool(description = "Use this to create or update a customer. Omit 'id' to create; include 'id' to update. Customer titles are unique per tenant. PE: use 'entityGroupId'/'entityGroupIds' for groups.")
    public String saveCustomer(
            @ToolParam(description = "JSON customer object. Omit 'id' to create; include 'id' to update. " + CUSTOMER_JSON_EXAMPLE)
            @NotBlank @Valid String customerJson,
            @ToolParam(required = false, description = "(PE only) " + ENTITY_GROUP_ID_CREATE_PARAM_DESCRIPTION)
            @NotBlank String entityGroupId,
            @ToolParam(required = false, description = "(PE only) " + ENTITY_GROUP_IDS_CREATE_PARAM_DESCRIPTION)
            @NotBlank String entityGroupIds) {
        Customer customer = JacksonUtil.fromString(customerJson, Customer.class);
        if (StringUtils.isNotBlank(entityGroupId)) {
            return JacksonUtil.toString(clientService.getClient().saveCustomer(customer, entityGroupId, null, null, null, null));
        } else if (StringUtils.isNotBlank(entityGroupIds)) {
            return JacksonUtil.toString(clientService.getClient().saveCustomer(customer, null, Arrays.asList(entityGroupIds.split(",")), null, null, null));
        } else {
            return JacksonUtil.toString(clientService.getClient().saveCustomer(customer, null, null, null, null, null));
        }
    }

    @Tool(description = "Use this to permanently delete a customer and all its users. Assigned dashboards, assets, devices will be unassigned but not deleted.")
    public String deleteCustomer(@ToolParam(description = CUSTOMER_ID_PARAM_DESCRIPTION) @NotBlank String customerIdStr) {
        try {
            clientService.getClient().deleteCustomer(customerIdStr);
            return "{\"status\":\"OK\",\"id\":\"" + customerIdStr + "\"}";
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("id", customerIdStr);
            err.put("message", e.getMessage());
            return JacksonUtil.toString(err);
        }
    }

    @Tool(description = "Use this to get a customer by its id.")
    public String getCustomerById(@ToolParam(description = CUSTOMER_ID_PARAM_DESCRIPTION) @NotBlank String customerId) {
        return JacksonUtil.toString(clientService.getClient().getCustomerById(customerId));
    }

    @Tool(description = "Use this to get a paginated list of customers owned by the tenant.")
    public String getCustomers(
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = CUSTOMER_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'title', 'email', 'country', 'city'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        return JacksonUtil.toString(clientService.getClient().getCustomers(
                ToolUtils.parseIntOrDefault(pageSize, ToolUtils.PAGE_SIZE),
                ToolUtils.parseIntOrDefault(page, ToolUtils.PAGE_NUMBER),
                ToolUtils.sanitizeStringParam(textSearch),
                ToolUtils.sanitizeStringParam(sortProperty),
                ToolUtils.sanitizeStringParam(sortOrder)));
    }

    @Tool(description = "Use this to get a customer by its unique title within the tenant.")
    public String getTenantCustomer(@ToolParam(description = "A string value representing the Customer title.") @NotBlank String customerTitle) {
        return JacksonUtil.toString(clientService.getClient().getTenantCustomer(customerTitle));
    }

    @PeOnly
    @Tool(description = "Use this to get a paginated list of customers available to the current user. PE only.")
    public String getUserCustomers(
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = CUSTOMER_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'title', 'email', 'country', 'city'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        if (ThingsBoardEdition.CE == clientService.getEdition()) {
            return PE_ONLY_AVAILABLE;
        }
        // PE-only API methods accept String params for pagination (unlike CE methods that use Integer)
        return JacksonUtil.toString(clientService.getClient().getUserCustomers(
                ToolUtils.sanitizeStringParam(pageSize),
                ToolUtils.sanitizeStringParam(page),
                ToolUtils.sanitizeStringParam(textSearch),
                ToolUtils.sanitizeStringParam(sortProperty),
                ToolUtils.sanitizeStringParam(sortOrder)));
    }

    @PeOnly
    @Tool(description = "Use this to get a paginated list of customers in a specific entity group. PE only.")
    public String getCustomersByEntityGroupId(
            @ToolParam(description = ENTITY_GROUP_ID_PARAM_DESCRIPTION) @NotBlank String entityGroupId,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = CUSTOMER_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'title', 'email', 'country', 'city'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        if (ThingsBoardEdition.CE == clientService.getEdition()) {
            return PE_ONLY_AVAILABLE;
        }
        // PE-only API methods accept String params for pagination (unlike CE methods that use Integer)
        return JacksonUtil.toString(clientService.getClient().getCustomersByEntityGroupId(
                entityGroupId,
                ToolUtils.sanitizeStringParam(pageSize),
                ToolUtils.sanitizeStringParam(page),
                ToolUtils.sanitizeStringParam(textSearch),
                ToolUtils.sanitizeStringParam(sortProperty),
                ToolUtils.sanitizeStringParam(sortOrder)));
    }

}
