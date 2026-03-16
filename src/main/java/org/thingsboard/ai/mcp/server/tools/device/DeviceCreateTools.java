package org.thingsboard.ai.mcp.server.tools.device;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.annotation.ToolGroup;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.McpTools;
import org.thingsboard.ai.mcp.server.util.JsonUtils;
import org.thingsboard.client.ApiException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.client.model.Customer;
import org.thingsboard.client.model.CustomerId;
import org.thingsboard.client.model.Device;
import org.thingsboard.client.model.DeviceProfileId;
import org.thingsboard.client.model.EntityGroupInfo;
import org.thingsboard.client.model.EntityType;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ToolGroup("device")
public class DeviceCreateTools implements McpTools {

    private final RestClientService clientService;

    @Tool(description = "Use this to create or update a device by name. Primary tool for 90% of device tasks. " +
            "Handles name-to-ID lookups for customers and groups automatically. " +
            "Best for: creating devices, assigning to customers by title, adding to groups by name, updating label/type. " +
            "For nested 'deviceData', firmware, or transport settings, use 'saveDevice' instead.")
    public String createOrUpsertDevice(
            @ToolParam(description = "Device name (unique per tenant).") @NotBlank String name,
            @ToolParam(required = false, description = "Device type (string).") String type,
            @ToolParam(required = false, description = "Device label.") String label,
            @ToolParam(required = false, description = "Device Profile UUID (optional).") String deviceProfileId,
            @ToolParam(required = false, description = "Customer UUID to assign (optional).") String customerId,
            @ToolParam(required = false, description = "Customer title to assign if customerId not provided (optional).") String customerTitle,
            @ToolParam(required = false, description = "DEVICE Group UUID to add the device to (optional).") String groupId,
            @ToolParam(required = false, description = "DEVICE Group name to add the device to if groupId not provided (optional).") String groupName
    ) {
        var client = clientService.getClient();

        try {
            Device device;
            boolean created;

            try {
                device = client.getTenantDeviceByName(name);
                created = false;
            } catch (ApiException e) {
                device = new Device();
                device.name(name);
                if (type != null && !type.isBlank()) device.type(type);
                if (label != null && !label.isBlank()) device.label(label);

                if (deviceProfileId != null && !deviceProfileId.isBlank()) {
                    device.setDeviceProfileId(new DeviceProfileId().id(UUID.fromString(deviceProfileId)).entityType(EntityType.DEVICE_PROFILE));
                }

                String resolvedCustomerId = null;
                if (customerId != null && !customerId.isBlank()) {
                    resolvedCustomerId = customerId;
                } else if (customerTitle != null && !customerTitle.isBlank()) {
                    Customer cust;
                    try {
                        cust = client.getTenantCustomer(customerTitle);
                    } catch (ApiException ex) {
                        Customer c = new Customer();
                        c.title(customerTitle);
                        cust = client.saveCustomer(c, null, null, null, null, null);
                    }
                    resolvedCustomerId = cust.getId().getId().toString();
                }

                // customerId is read-only on Device model — set it via JSON round-trip
                if (resolvedCustomerId != null) {
                    ObjectNode deviceNode = JsonUtils.getMapper().valueToTree(device);
                    ObjectNode custIdNode = JsonUtils.getMapper().createObjectNode();
                    custIdNode.put("id", resolvedCustomerId);
                    custIdNode.put("entityType", "CUSTOMER");
                    deviceNode.set("customerId", custIdNode);
                    device = JsonUtils.getMapper().treeToValue(deviceNode, Device.class);
                }

                device = client.saveDevice(device, null, null, null, null, null, null);
                created = true;
            }

            if ((groupId != null && !groupId.isBlank()) || (groupName != null && !groupName.isBlank())) {
                String egId = resolveDeviceGroupId(groupId, groupName);
                if (egId != null) {
                    client.addEntitiesToEntityGroup(egId, List.of(device.getId().getId().toString()));
                }
            }

            var result = new java.util.LinkedHashMap<String, Object>();
            result.put("created", created);
            result.put("deviceId", device.getId().getId().toString());
            result.put("name", device.getName());
            result.put("type", device.getType());
            result.put("label", device.getLabel());
            result.put("customerId", device.getCustomerId() != null ? device.getCustomerId().getId().toString() : null);
            result.put("deviceProfileId", device.getDeviceProfileId() != null ? device.getDeviceProfileId().getId().toString() : null);
            return JsonUtils.toString(result);

        } catch (Exception e) {
            var error = new java.util.LinkedHashMap<String, Object>();
            error.put("error", e.getClass().getSimpleName());
            error.put("message", e.getMessage());
            return JsonUtils.toString(error);
        }
    }

    private String resolveDeviceGroupId(String groupId, String groupName) {
        var client = clientService.getClient();

        try {
            if (groupId != null && !groupId.isBlank()) {
                return groupId;
            }
            if (groupName != null && !groupName.isBlank()) {
                List<EntityGroupInfo> groups = client.getAllEntityGroupsByType(EntityType.DEVICE.getValue(), null);
                if (groups != null) {
                    return groups.stream()
                            .filter(g -> groupName.equalsIgnoreCase(g.getName()))
                            .map(g -> g.getId().getId().toString())
                            .findFirst()
                            .orElse(null);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

}
