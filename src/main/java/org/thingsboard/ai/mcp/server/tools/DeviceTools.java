package org.thingsboard.ai.mcp.server.tools;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;

import java.util.Arrays;
import java.util.UUID;

import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.ASSET_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.ASSET_TYPE_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.DEVICE_ACTIVE_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.DEVICE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.DEVICE_INFO_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.DEVICE_NAME_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.DEVICE_PROFILE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.EDGE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.ai.mcp.server.tools.ToolUtils.createPageLink;
import static org.thingsboard.ai.mcp.server.tools.ToolUtils.createTimePageLink;

@Service
@RequiredArgsConstructor
public class DeviceTools {

    private final RestClientService clientService;

    @Tool(description = "Fetch the Device object based on the provided Device Id. " +
            "If the user has the authority of 'TENANT_ADMIN', the server checks that the device is owned by the same tenant. " +
            "If the user has the authority of 'CUSTOMER_USER', the server checks that the device is assigned to the same customer." +
            TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getDeviceById(@ToolParam(description = DEVICE_ID_PARAM_DESCRIPTION) String deviceId) {
        return JacksonUtil.toString(clientService.getClient().getDeviceById(new DeviceId(UUID.fromString(deviceId))));
    }

    @Tool(description = "Fetch the Device Info object based on the provided Device Id. " +
            "If the user has the authority of 'Tenant Administrator', the server checks that the device is owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the device is assigned to the same customer. " +
            DEVICE_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getDeviceInfoById(@ToolParam(description = DEVICE_ID_PARAM_DESCRIPTION) String deviceId) {
        return JacksonUtil.toString(clientService.getClient().getDeviceInfoById(new DeviceId(UUID.fromString(deviceId))));
    }

    @Tool(description = "Get device credentials by device id. If during device creation there wasn't specified any credentials, platform generates random 'ACCESS_TOKEN' credentials. " +
            TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getDeviceCredentialsByDeviceId(@ToolParam(description = DEVICE_ID_PARAM_DESCRIPTION) String deviceId) {
        return JacksonUtil.toString(clientService.getClient().getDeviceCredentialsByDeviceId(new DeviceId(UUID.fromString(deviceId))));
    }

    @Tool(description = "Returns a page of devices owned by tenant. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    public String getTenantDevices(
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) int pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) int page,
            @ToolParam(required = false, description = ASSET_TYPE_DESCRIPTION) String type,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'name', 'deviceProfileName', 'label', 'customerTitle'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return JacksonUtil.toString(clientService.getClient().getTenantDevices(type, pageLink).getData());
    }

    @Tool(description = "Returns a page of devices info objects owned by tenant. " +
            PAGE_DATA_PARAMETERS + DEVICE_INFO_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH)
    public String getTenantDeviceInfos(
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) int pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) int page,
            @ToolParam(required = false, description = ASSET_TYPE_DESCRIPTION) String type,
            @ToolParam(required = false, description = DEVICE_PROFILE_ID_PARAM_DESCRIPTION) String deviceProfileId,
            @ToolParam(required = false, description = DEVICE_ACTIVE_PARAM_DESCRIPTION) boolean active,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'name', 'deviceProfileName', 'label', 'customerTitle'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        DeviceProfileId profileId = deviceProfileId != null ? new DeviceProfileId(UUID.fromString(deviceProfileId)) : null;
        return JacksonUtil.toString(clientService.getClient().getTenantDeviceInfos(type, active, profileId, pageLink).getData());
    }

    @Tool(description = "Requested device must be owned by tenant that the user belongs to. " +
            "Device name is an unique property of device. So it can be used to identify the device." + TENANT_AUTHORITY_PARAGRAPH)
    public String getTenantDevice(@ToolParam(description = DEVICE_NAME_DESCRIPTION) String deviceName) {
        return JacksonUtil.toString(clientService.getClient().getTenantDevice(deviceName));
    }

    @Tool(description = "Returns a page of devices objects assigned to customer. " +
            PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getCustomerDevices(
            @ToolParam(description = CUSTOMER_ID_PARAM_DESCRIPTION) String customerId,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) int pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) int page,
            @ToolParam(required = false, description = ASSET_TYPE_DESCRIPTION) String type,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'name', 'deviceProfileName', 'label', 'customerTitle'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return JacksonUtil.toString(clientService.getClient().getCustomerDevices(new CustomerId(UUID.fromString(customerId)), type, pageLink).getData());
    }

    @Tool(description = "Returns a page of devices info objects assigned to customer. " +
            PAGE_DATA_PARAMETERS + DEVICE_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getCustomerDeviceInfos(
            @ToolParam(description = CUSTOMER_ID_PARAM_DESCRIPTION) String customerId,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) int pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) int page,
            @ToolParam(required = false, description = ASSET_TYPE_DESCRIPTION) String type,
            @ToolParam(required = false, description = DEVICE_PROFILE_ID_PARAM_DESCRIPTION) String deviceProfileId,
            @ToolParam(required = false, description = DEVICE_ACTIVE_PARAM_DESCRIPTION) boolean active,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'name', 'deviceProfileName', 'label', 'customerTitle'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        DeviceProfileId profileId = deviceProfileId != null ? new DeviceProfileId(UUID.fromString(deviceProfileId)) : null;
        return JacksonUtil.toString(clientService.getClient().getCustomerDeviceInfos(new CustomerId(UUID.fromString(customerId)), type, profileId, pageLink).getData());
    }

    @Tool(description = "Get Devices By Ids. Requested devices must be owned by tenant or assigned to customer which user is performing the request. " + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getDevicesByIds(@ToolParam(description = "A list of assets ids, separated by comma ','") String... devicesIds) {
        return JacksonUtil.toString(clientService.getClient().getDevicesByIds(Arrays.stream(devicesIds).map(UUID::fromString).map(DeviceId::new).toList()));
    }

    @Tool(description = "Returns a page of devices assigned to edge. " +
            PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getEdgeDevices(
            @ToolParam(description = EDGE_ID_PARAM_DESCRIPTION) String edgeId,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) int pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) int page,
            @ToolParam(required = false, description = ASSET_TYPE_DESCRIPTION) String type,
            @ToolParam(required = false, description = DEVICE_PROFILE_ID_PARAM_DESCRIPTION) String deviceProfileId,
            @ToolParam(required = false, description = DEVICE_ACTIVE_PARAM_DESCRIPTION) boolean active,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'name', 'type', 'label', 'clearTs', 'customerTitle'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder,
            @ToolParam(required = false, description = "Timestamp. Assets with creation time before it won't be queried") long startTime,
            @ToolParam(required = false, description = "Timestamp. Assets with creation time after it won't be queried") long endTime) throws ThingsboardException {
        TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
        return JacksonUtil.toString(clientService.getClient().getEdgeDevices(new EdgeId(UUID.fromString(edgeId)), pageLink).getData());
    }

    @Tool(description = "The platform gives an ability to load OTA (over-the-air) packages to devices. " +
            "It can be done in two different ways: device scope or device profile scope." +
            "In the response you will find the number of devices with specified device profile, but without previously defined device scope OTA package. " +
            "It can be useful when you want to define number of devices that will be affected with future OTA package" + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public Long countByDeviceProfileAndEmptyOtaPackage(
            @ToolParam(description = "Ota package type. Allowed values: 'FIRMWARE', 'SOFTWARE'") String otaPackageType,
            @ToolParam(description = DEVICE_PROFILE_ID_PARAM_DESCRIPTION) String deviceProfileId) {
        OtaPackageType type = OtaPackageType.valueOf(otaPackageType);
        return clientService.getClient().countByDeviceProfileAndEmptyOtaPackage(type, new DeviceProfileId(UUID.fromString(deviceProfileId)));
    }

}
