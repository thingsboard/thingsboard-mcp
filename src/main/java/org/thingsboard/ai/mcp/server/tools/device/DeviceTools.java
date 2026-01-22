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
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PE_ONLY_AVAILABLE;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.RBAC_GROUP_READ_CHECK;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.ai.mcp.server.util.ToolUtils.createPageLink;

@Service
@RequiredArgsConstructor
public class DeviceTools implements McpTools {

    private static final String DEVICE_JSON_EXAMPLE =
            """
                    ```json
                    {
                      "id": { "entityType": "DEVICE", "id": "d3cfc080-a295-11f0-848c-93db0ade7d93" },
                      "tenantId": { "entityType": "TENANT", "id": "d3cfc080-a295-11f0-848c-93db0ade7d93" },
                      "customerId": { "entityType": "CUSTOMER", "id": "d3cfc080-a295-11f0-848c-93db0ade7d93" },
                      "name": "A4B72CCDFF233",
                      "type": "default",
                      "label": "Room 234 Sensor",
                      "deviceProfileId": { "entityType": "DEVICE_PROFILE", "id": "716a92d0-9d36-11f0-a79c-e726b4e8048a" },
                      "firmwareId": { "entityType": "OTA_PACKAGE", "id": "784f394c-42b6-435a-983c-b7beff2784f9" },
                      "softwareId": { "entityType": "OTA_PACKAGE", "id": "784f394c-42b6-435a-983c-b7beff2784f9" },
                      "deviceData": {
                        "configuration": { "type": "DEFAULT" },
                        "transportConfiguration": { "type": "DEFAULT" }
                      }
                    }
                    ```""";

    private final RestClientService clientService;

    @Tool(description =
            "### [ADVANCED TOOL] Save/Update Device (Raw JSON/Full Config)\n" +
                    "**USE ONLY IF** the primary 'createOrUpsertDevice' tool is insufficient. This tool requires a full " +
                    "JSON structure and existing UUIDs. It does NOT perform name-to-ID resolution.\n\n" +

                    "**Best for:**\n" +
                    "- Modifying nested 'deviceData' (configuration/transport settings).\n" +
                    "- Updating Firmware (firmwareId) or Software (softwareId) packages.\n" +
                    "- Performing bulk updates where you already have the full Device JSON object.\n\n" +

                    "**Warning:** Requires exact JSON format. Omit 'id' to create; include 'id' to update. " +
                    "If you only have a name and want to create/assign a device, use 'createOrUpsertDevice' instead." +

                    "### Device Profile selection logic:\n" +
                    "- You can define the device’s profile either by specifying **deviceProfileId** or by setting a **type**.\n" +
                    "- If both `deviceProfileId` and `type` are provided, **deviceProfileId takes precedence** — the platform uses that profile.\n" +
                    "- If only `type` is provided, the platform will find or create a profile with that type name.\n" +
                    "- If neither is specified, the **default device profile** of the tenant is used.\n" +

                    "### Credentials:\n" +
                    "- Optionally, pass an `accessToken` to set initial credentials for the device.\n" +
                    "- If omitted, the platform will generate default credentials automatically.\n" +

                    "### Platform Edition:\n" +
                    "- In ThingsBoard PE, you can also attach the device to an entity group using the `entityGroupId` parameter or to multiple groups" +
                    "using `entityGroupIds` parameter.\n" +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String saveDevice(
            @ToolParam(description = "A JSON string representing the Device entity. Omit 'id' to create a new device; include 'id' to update an existing one." + DEVICE_JSON_EXAMPLE)
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

    @Tool(description = "Delete a device by id. Deletes the device, its credentials, and all relations. " +
            "Referencing a non-existing device id will cause an error. " + TENANT_AUTHORITY_PARAGRAPH)
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

    @Tool(description = "Fetch the Device object based on the provided Device Id. " +
            "If the user has the authority of 'TENANT_ADMIN', the server checks that the device is owned by the same tenant. " +
            "If the user has the authority of 'CUSTOMER_USER', the server checks that the device is assigned to the same customer." +
            TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getDeviceById(@ToolParam(description = DEVICE_ID_PARAM_DESCRIPTION) @NotBlank String deviceId) {
        return JacksonUtil.toString(clientService.getClient().getDeviceById(new DeviceId(UUID.fromString(deviceId))));
    }

    @Tool(description = "Get device credentials by device id. If during device creation there wasn't specified any credentials, platform generates random 'ACCESS_TOKEN' credentials. " +
            TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getDeviceCredentialsByDeviceId(@ToolParam(description = DEVICE_ID_PARAM_DESCRIPTION) @NotBlank String deviceId) {
        return JacksonUtil.toString(clientService.getClient().getDeviceCredentialsByDeviceId(new DeviceId(UUID.fromString(deviceId))));
    }

    @Tool(description = "Returns a page of devices owned by tenant. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
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

    @Tool(description = "Requested device must be owned by tenant that the user belongs to. " +
            "Device name is an unique property of device. So it can be used to identify the device." + TENANT_AUTHORITY_PARAGRAPH)
    public String getTenantDevice(@ToolParam(description = DEVICE_NAME_DESCRIPTION) @NotBlank String deviceName) {
        return JacksonUtil.toString(clientService.getClient().getTenantDevice(deviceName));
    }

    @Tool(description = "Returns a page of devices objects assigned to customer. " +
            PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
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
    @Tool(description = "Returns a page of device objects available for the current user. " + PE_ONLY_AVAILABLE + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
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

    @Tool(description = "Get Devices By Ids. Requested devices must be owned by tenant or assigned to customer which user is performing the request. " + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getDevicesByIds(@ToolParam(description = "A string of devices ids, separated by comma ','") @NotBlank String devicesIds) {
        List<DeviceId> deviceIdList = Arrays.stream(devicesIds.split(",")).map(UUID::fromString).map(DeviceId::new).toList();
        return JacksonUtil.toString(clientService.getClient().getDevicesByIds(deviceIdList));
    }

    @PeOnly
    @Tool(description = "Returns a page of device objects that belongs to specified Entity Group Id. " + PE_ONLY_AVAILABLE + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_GROUP_READ_CHECK)
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
