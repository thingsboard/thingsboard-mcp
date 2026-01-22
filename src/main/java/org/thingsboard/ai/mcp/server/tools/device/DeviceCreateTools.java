package org.thingsboard.ai.mcp.server.tools.device;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.McpTools;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceCreateTools implements McpTools {

    private final RestClientService clientService;

    @Tool(description =
            "### [PRIMARY TOOL] Create or Update Device (Simple/Standard)\n" +
                    "**USE THIS FIRST** for 90% of device tasks. It is the most reliable way to ensure a device exists " +
                    "and is correctly assigned. It automatically handles name-to-ID lookups for customers and groups.\n\n" +

                    "**Best for:**\n" +
                    "- Creating a new device by name.\n" +
                    "- Moving a device to a different customer using their title (e.g., 'Customer A').\n" +
                    "- Adding a device to a group using the group name (e.g., 'Thermostats').\n" +
                    "- Updating basic fields like 'label' or 'type'.\n\n" +

                    "**Constraint:** Only use this for standard properties. If you need to modify nested 'deviceData', " +
                    "firmware, or transport settings, use 'saveDevice' instead.")
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
            Optional<Device> existingOpt = client.getTenantDevice(name);
            Device device;
            boolean created;

            if (existingOpt.isPresent()) {
                device = existingOpt.get();
                created = false;
            } else {
                device = new Device();
                device.setName(name);
                if (type != null && !type.isBlank()) device.setType(type);
                if (label != null && !label.isBlank()) device.setLabel(label);


                if (deviceProfileId != null && !deviceProfileId.isBlank()) {
                    device.setDeviceProfileId(new DeviceProfileId(UUID.fromString(deviceProfileId)));
                }

                if (customerId != null && !customerId.isBlank()) {
                    device.setCustomerId(new CustomerId(UUID.fromString(customerId)));
                } else if (customerTitle != null && !customerTitle.isBlank()) {
                    var cust = client.getTenantCustomer(customerTitle).orElseGet(() -> {
                        Customer c = new Customer();
                        c.setTitle(customerTitle);
                        return client.saveCustomer(c);
                    });
                    device.setCustomerId(cust.getId());
                }

                device = client.createDevice(device);
                created = true;
            }

            if ((groupId != null && !groupId.isBlank()) || (groupName != null && !groupName.isBlank())) {
                EntityGroupId egId = resolveDeviceGroupId(groupId, groupName);
                if (egId != null) {
                    client.addEntitiesToEntityGroup(egId, List.of(device.getId()));
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
            return JacksonUtil.toString(result);

        } catch (Exception e) {
            var error = new java.util.LinkedHashMap<String, Object>();
            error.put("error", e.getClass().getSimpleName());
            error.put("message", e.getMessage());
            return JacksonUtil.toString(error);
        }
    }

    private EntityGroupId resolveDeviceGroupId(String groupId, String groupName) {
        var client = clientService.getClient();

        try {
            if (groupId != null && !groupId.isBlank()) {
                return new EntityGroupId(UUID.fromString(groupId));
            }
            if (groupName != null && !groupName.isBlank()) {
                return client.getEntityGroupsByType(EntityType.DEVICE).stream()
                        .filter(g -> groupName.equalsIgnoreCase(g.getName()))
                        .map(EntityGroupInfo::getId)
                        .findFirst()
                        .orElse(null);
            }
        } catch (Exception ignored) {}
        return null;
    }

}
