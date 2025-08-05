package org.thingsboard.ai.mcp.server.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.UUID;

import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

@Service
public class CustomerTools {

    @Autowired
    private RestClientService clientService;

    @Tool(description = "Get the Customer object based on the provided Customer Id. " +
            "If the user has the authority of 'Tenant Administrator', the server checks that the customer is owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the user belongs to the customer. " + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getCustomerById(String customerId) {
        return JacksonUtil.toString(clientService.getClient().getCustomerById(new CustomerId(UUID.fromString(customerId))));
    }

    @Tool(description = "Get the short customer object that contains only the title and 'isPublic' flag. " +
            "If the user has the authority of 'Tenant Administrator', the server checks that the customer is owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the user belongs to the customer. " + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getShortCustomerInfoById(String customerId) {
        return JacksonUtil.toString(clientService.getClient().getShortCustomerInfoById(new CustomerId(UUID.fromString(customerId))));
    }

    @Tool(description = "Get the title of the customer. " +
            "If the user has the authority of 'Tenant Administrator', the server checks that the customer is owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the user belongs to the customer. " + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getCustomerTitleById(String customerId) {
        return clientService.getClient().getCustomerTitleById(new CustomerId(UUID.fromString(customerId)));
    }

    @Tool(description = "Returns a page of customers owned by tenant. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    public String getCustomers(int pageSize, int page, String textSearch) {
        var result = clientService.getClient().getCustomers(new PageLink(pageSize, page, textSearch));
        return JacksonUtil.toString(result.getData());
    }

    @Tool(description = "Get the Customer using Customer Title. " + TENANT_AUTHORITY_PARAGRAPH)
    public String getTenantCustomer(String customerTitle) {
        return JacksonUtil.toString(clientService.getClient().getTenantCustomer(customerTitle));
    }
}