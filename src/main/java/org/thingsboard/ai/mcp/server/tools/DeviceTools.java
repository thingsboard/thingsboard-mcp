package org.thingsboard.ai.mcp.server.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.UUID;

import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.DEVICE_INFO_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

@Service
public class DeviceTools {

    @Autowired
    private RestClientService clientService;

    @Tool(description = "Returns a page of devices owned by tenant. " +
            PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    public String getTenantDevices(String type, int pageSize, int page, String textSearch) {
        var result = clientService.getClient().getTenantDevices(type, new PageLink(pageSize, page, textSearch));
        return JacksonUtil.toString(result.getData());
    }

    @Tool(description = "Returns a page of devices objects assigned to customer. " +
            PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getCustomerDevices(String customerId, String type, int pageSize, int page, String textSearch) {
        var result = clientService.getClient().getCustomerDevices(new CustomerId(UUID.fromString(customerId)), type, new PageLink(pageSize, page, textSearch));
        return JacksonUtil.toString(result.getData());
    }

    @Tool(description = "Returns a page of devices info objects assigned to customer. " +
            PAGE_DATA_PARAMETERS + DEVICE_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getCustomerDeviceInfos(String customerId, String type, String deviceProfileId, int pageSize, int page, String textSearch) {
        DeviceProfileId profileId = deviceProfileId != null ? new DeviceProfileId(UUID.fromString(deviceProfileId)) : null;
        var result = clientService.getClient().getCustomerDeviceInfos(new CustomerId(UUID.fromString(customerId)), type, profileId, new PageLink(pageSize, page, textSearch));
        return JacksonUtil.toString(result.getData());
    }

    @Tool(description = "Fetch the Device object based on the provided Device Id. " +
            "If the user has the authority of 'TENANT_ADMIN', the server checks that the device is owned by the same tenant. " +
            "If the user has the authority of 'CUSTOMER_USER', the server checks that the device is assigned to the same customer." +
            TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getDeviceById(String deviceId) {
        return JacksonUtil.toString(clientService.getClient().getDeviceById(new DeviceId(UUID.fromString(deviceId))));
    }

    @Tool(description = "Fetch the Device Info object based on the provided Device Id. " +
            "If the user has the authority of 'Tenant Administrator', the server checks that the device is owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the device is assigned to the same customer. " +
            DEVICE_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getDeviceInfoById(String deviceId) {
        return JacksonUtil.toString(clientService.getClient().getDeviceInfoById(new DeviceId(UUID.fromString(deviceId))));
    }

    @Tool(description = "Returns a page of devices assigned to edge. " +
            PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getEdgeDevices(String edgeId, int pageSize, int page, String textSearch) {
        var result = clientService.getClient().getEdgeDevices(new EdgeId(UUID.fromString(edgeId)), new PageLink(pageSize, page, textSearch));
        return JacksonUtil.toString(result.getData());
    }

    @Tool(description = "Returns a page of devices info objects owned by tenant. " +
            PAGE_DATA_PARAMETERS + DEVICE_INFO_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH)
    public String getTenantDeviceInfos(String type, boolean active, String deviceProfileId, int pageSize, int page, String textSearch) {
        DeviceProfileId profileId = deviceProfileId != null ? new DeviceProfileId(UUID.fromString(deviceProfileId)) : null;
        var result = clientService.getClient().getTenantDeviceInfos(type, active, profileId, new PageLink(pageSize, page, textSearch));
        return JacksonUtil.toString(result.getData());
    }

    @Tool(description = "Requested device must be owned by tenant that the user belongs to. " +
            "Device name is an unique property of device. So it can be used to identify the device." + TENANT_AUTHORITY_PARAGRAPH)
    public String getTenantDevice(String deviceName) {
        return JacksonUtil.toString(clientService.getClient().getTenantDevice(deviceName));
    }

}
