package org.thingsboard.ai.mcp.server.tools.device;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.annotation.PeOnly;
import org.thingsboard.ai.mcp.server.annotation.ToolGroup;
import org.thingsboard.ai.mcp.server.data.ThingsBoardEdition;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.McpTools;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.DEVICE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.DEVICE_NAME_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.DEVICE_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.DEVICE_TYPE_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_GROUP_IDS_CREATE_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_GROUP_ID_CREATE_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_GROUP_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PE_ONLY_AVAILABLE;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.util.ToolUtils.createPageLink;

@Service
@RequiredArgsConstructor
@ToolGroup("device")
public class DeviceTools implements McpTools {

    private static final String DEVICE_JSON_EXAMPLE = """
            {
              "name": "A4B72CCDFF233",
              "type": "default",
              "label": "Room 234 Sensor",
              "deviceProfileId": {"entityType": "DEVICE_PROFILE", "id": "<profileId>"}
            }""";

    private final RestClientService clientService;

    @Tool(description = "Use this to save/update a device from raw JSON. Advanced tool: use 'createOrUpsertDevice' first for standard tasks. " +
                    "Best for modifying 'deviceData', firmware/software IDs, or bulk updates with full JSON. " +
                    "Omit 'id' to create; include 'id' to update. Profile: 'deviceProfileId' takes precedence over 'type'. " +
                    "PE: use 'entityGroupId'/'entityGroupIds' for groups.")
    public String saveDevice(
            @ToolParam(description = "JSON device object. Omit 'id' to create; include 'id' to update. " + DEVICE_JSON_EXAMPLE)
            @NotBlank @Valid String deviceJson,
            @ToolParam(required = false, description = "Optional access token to set initial credentials during device creation.")
            String accessToken,
            @ToolParam(required = false, description = "(PE only) " + ENTITY_GROUP_ID_CREATE_PARAM_DESCRIPTION)
            @NotBlank String entityGroupId,
            @ToolParam(required = false, description = "(PE only) " + ENTITY_GROUP_IDS_CREATE_PARAM_DESCRIPTION)
            @NotBlank String entityGroupIds) {
        Device device = JacksonUtil.fromString(deviceJson, Device.class);
        if (entityGroupId != null) {
            return JacksonUtil.toString(clientService.getClient().saveDevice(device, accessToken, new EntityGroupId(UUID.fromString(entityGroupId)), null));
        } else if (StringUtils.isNotBlank(entityGroupIds)) {
            return JacksonUtil.toString(clientService.getClient().saveDevice(device, accessToken, null, entityGroupIds));
        } else {
            return JacksonUtil.toString(clientService.getClient().saveDevice(device, accessToken));
        }
    }

    @Tool(description = "Use this to permanently delete a device, its credentials, and all relations by id.")
    public String deleteDevice(@ToolParam(description = DEVICE_ID_PARAM_DESCRIPTION) @NotBlank String deviceId) {
        try {
            DeviceId id = new DeviceId(UUID.fromString(deviceId));

            clientService.getClient().deleteDevice(id);
            return "{\"status\":\"OK\",\"id\":\"" + deviceId + "\"}";
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("id", deviceId);
            err.put("message", e.getMessage());
            return JacksonUtil.toString(err);
        }
    }

    @Tool(description = "Use this to get a device by its id.")
    public String getDeviceById(@ToolParam(description = DEVICE_ID_PARAM_DESCRIPTION) @NotBlank String deviceId) {
        return JacksonUtil.toString(clientService.getClient().getDeviceById(new DeviceId(UUID.fromString(deviceId))));
    }

    @Tool(description = "Use this to get device credentials (e.g., ACCESS_TOKEN) by device id.")
    public String getDeviceCredentialsByDeviceId(@ToolParam(description = DEVICE_ID_PARAM_DESCRIPTION) @NotBlank String deviceId) {
        return JacksonUtil.toString(clientService.getClient().getDeviceCredentialsByDeviceId(new DeviceId(UUID.fromString(deviceId))));
    }

    @Tool(description = "Use this to get a paginated list of devices owned by the tenant. Filter by type.")
    public String getTenantDevices(
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = DEVICE_TYPE_DESCRIPTION) String type,
            @ToolParam(required = false, description = DEVICE_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'name', 'deviceProfileName', 'label', 'customerTitle'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return JacksonUtil.toString(clientService.getClient().getTenantDevices(type, pageLink));
    }

    @Tool(description = "Use this to get a device by its unique name within the tenant.")
    public String getTenantDevice(@ToolParam(description = DEVICE_NAME_DESCRIPTION) @NotBlank String deviceName) {
        return JacksonUtil.toString(clientService.getClient().getTenantDevice(deviceName));
    }

    @Tool(description = "Use this to get a paginated list of devices assigned to a specific customer. Filter by type.")
    public String getCustomerDevices(
            @ToolParam(description = CUSTOMER_ID_PARAM_DESCRIPTION) @NotBlank String customerId,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = DEVICE_TYPE_DESCRIPTION) String type,
            @ToolParam(required = false, description = DEVICE_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'name', 'deviceProfileName', 'label', 'customerTitle'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return JacksonUtil.toString(clientService.getClient().getCustomerDevices(new CustomerId(UUID.fromString(customerId)), type, pageLink));
    }

    @PeOnly
    @Tool(description = "Use this to get a paginated list of devices available to the current user. PE only.")
    public String getUserDevices(
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = DEVICE_TYPE_DESCRIPTION) String type,
            @ToolParam(required = false, description = DEVICE_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'name', 'type', 'deviceProfileName', 'label', 'customerTitle'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        if (ThingsBoardEdition.CE == clientService.getEdition()) {
            return PE_ONLY_AVAILABLE;
        }
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return JacksonUtil.toString(clientService.getClient().getUserDevices(type, pageLink));
    }

    @Tool(description = "Use this to get multiple devices by their ids (comma-separated).")
    public String getDevicesByIds(@ToolParam(description = "A string of devices ids, separated by comma ','") @NotBlank String devicesIds) {
        List<DeviceId> deviceIdList = Arrays.stream(devicesIds.split(",")).map(UUID::fromString).map(DeviceId::new).toList();
        return JacksonUtil.toString(clientService.getClient().getDevicesByIds(deviceIdList));
    }

    @PeOnly
    @Tool(description = "Use this to get a paginated list of devices in a specific entity group. PE only.")
    public String getDevicesByEntityGroupId(
            @ToolParam(description = ENTITY_GROUP_ID_PARAM_DESCRIPTION) @NotBlank String entityGroupId,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = DEVICE_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'firstName', 'lastName', 'email'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        if (ThingsBoardEdition.CE == clientService.getEdition()) {
            return PE_ONLY_AVAILABLE;
        }
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return JacksonUtil.toString(clientService.getClient().getDevicesByEntityGroupId(new EntityGroupId(UUID.fromString(entityGroupId)), pageLink));
    }

}
