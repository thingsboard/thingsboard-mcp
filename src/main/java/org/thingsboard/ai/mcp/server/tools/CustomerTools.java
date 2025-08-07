package org.thingsboard.ai.mcp.server.tools;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.UUID;

import static org.thingsboard.ai.mcp.server.util.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.util.ControllerConstants.CUSTOMER_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.util.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.ai.mcp.server.util.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.util.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.util.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.util.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.util.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.ai.mcp.server.util.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.ai.mcp.server.util.ToolUtils.createPageLink;

@Service
@RequiredArgsConstructor
public class CustomerTools {

    private final RestClientService clientService;

    private static final String CUSTOMER_SECURITY_CHECK = "If the user has the authority of 'Tenant Administrator', the server checks that the customer is owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the user belongs to the customer.";

    @Tool(description = "Get the Customer object based on the provided Customer Id. " + CUSTOMER_SECURITY_CHECK + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getCustomerById(@ToolParam(description = CUSTOMER_ID_PARAM_DESCRIPTION) String customerId) {
        return JacksonUtil.toString(clientService.getClient().getCustomerById(new CustomerId(UUID.fromString(customerId))));
    }

    @Tool(description = "Get the short customer object that contains only the title and 'isPublic' flag. " + CUSTOMER_SECURITY_CHECK + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getShortCustomerInfoById(@ToolParam(description = CUSTOMER_ID_PARAM_DESCRIPTION) String customerId) {
        return JacksonUtil.toString(clientService.getClient().getShortCustomerInfoById(new CustomerId(UUID.fromString(customerId))));
    }

    @Tool(description = "Get the title of the customer. " + CUSTOMER_SECURITY_CHECK + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getCustomerTitleById(@ToolParam(description = CUSTOMER_ID_PARAM_DESCRIPTION) String customerId) {
        return clientService.getClient().getCustomerTitleById(new CustomerId(UUID.fromString(customerId)));
    }

    @Tool(description = "Returns a page of customers owned by tenant. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    public String getCustomers(
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) int pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) int page,
            @ToolParam(required = false, description = CUSTOMER_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'title', 'email', 'country', 'city'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return JacksonUtil.toString(clientService.getClient().getCustomers(pageLink).getData());
    }

    @Tool(description = "Get the Customer using Customer Title. " + TENANT_AUTHORITY_PARAGRAPH)
    public String getTenantCustomer(@ToolParam(description = "A string value representing the Customer title.") String customerTitle) {
        return JacksonUtil.toString(clientService.getClient().getTenantCustomer(customerTitle));
    }

}
