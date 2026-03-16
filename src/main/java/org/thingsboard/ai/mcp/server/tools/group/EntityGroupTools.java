package org.thingsboard.ai.mcp.server.tools.group;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.annotation.PeOnly;
import org.thingsboard.ai.mcp.server.annotation.ToolGroup;
import org.thingsboard.ai.mcp.server.data.ThingsBoardEdition;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.McpTools;
import org.thingsboard.ai.mcp.server.util.JsonUtils;
import org.thingsboard.client.model.EntityGroup;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_GROUP_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PE_ONLY_AVAILABLE;

@Service
@RequiredArgsConstructor
@ToolGroup("group")
public class EntityGroupTools implements McpTools {

    private static final String ENTITY_GROUP_JSON_EXAMPLE = """
            {
              "type": "DEVICE",
              "name": "Water meters",
              "ownerId": {"entityType": "TENANT", "id": "<tenantId>"}
            }""";

    private static final String ENTITY_GROUP_TYPE_PARAMETER_DESCRIPTION = "Entity Group type";

    private static final Set<String> ALLOWED_TYPES = Set.of("CUSTOMER", "ASSET", "DEVICE", "USER", "ENTITY_VIEW", "DASHBOARD", "EDGE");

    private final RestClientService clientService;

    @PeOnly
    @Tool(description = "Use this to create or update an entity group. Omit 'id' to create; include 'id' to update. Group names are unique per owner + entity type. Supported types: CUSTOMER, ASSET, DEVICE, USER, ENTITY_VIEW, DASHBOARD, EDGE. PE only.")
    public String saveEntityGroup(
            @ToolParam(description = "JSON entity group object. Omit 'id' to create; include 'id' to update. " + ENTITY_GROUP_JSON_EXAMPLE)
            @NotBlank @Valid String entityGroupJson) {
        EntityGroup eg = JsonUtils.fromString(entityGroupJson, EntityGroup.class);
        return JsonUtils.toString(clientService.getClient().saveEntityGroup(eg));
    }

    @PeOnly
    @Tool(description = "Use this to delete an entity group by id. Entities remain in the 'All' group. Cannot delete the 'All' group. PE only.")
    public String deleteEntityGroup(@ToolParam(description = ENTITY_GROUP_ID_PARAM_DESCRIPTION) @NotBlank String entityGroupIdStr) {
        try {
            clientService.getClient().deleteEntityGroup(entityGroupIdStr);
            return "{\"status\":\"OK\",\"id\":\"" + entityGroupIdStr + "\"}";
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("id", entityGroupIdStr);
            err.put("message", e.getMessage());
            return JsonUtils.toString(err);
        }
    }

    @PeOnly
    @Tool(description = "Use this to add entities to an entity group. Cannot add to the 'All' group. PE only.")
    public String addEntitiesToEntityGroup(
            @ToolParam(description = ENTITY_GROUP_ID_PARAM_DESCRIPTION) @NotBlank String entityGroupIdStr,
            @ToolParam(description = "Comma-separated list of entity IDs (UUIDs). to be added to the entity group") @NotBlank String entityIdsStr,
            @ToolParam(description = ENTITY_GROUP_TYPE_PARAMETER_DESCRIPTION) @NotBlank String entityGroupType) {
        try {
            List<String> entityIds = Arrays.stream(entityIdsStr.split(",")).map(String::trim).toList();
            clientService.getClient().addEntitiesToEntityGroup(entityGroupIdStr, entityIds);
            return "{\"status\":\"OK\"}";
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("id", entityGroupIdStr);
            err.put("message", e.getMessage());
            return JsonUtils.toString(err);
        }
    }

    @PeOnly
    @Tool(description = "Use this to remove entities from an entity group. Cannot remove from the 'All' group. PE only.")
    public String removeEntitiesFromEntityGroup(
            @ToolParam(description = "Entity Group ID (UUID).") @NotBlank String entityGroupIdStr,
            @ToolParam(description = "Comma-separated list of entity IDs (UUIDs) to be removed from the entity group.") @NotBlank String entityIdsStr,
            @ToolParam(description = ENTITY_GROUP_TYPE_PARAMETER_DESCRIPTION) @NotBlank String entityGroupType) {
        try {
            List<String> entityIds = Arrays.stream(entityIdsStr.split(",")).map(String::trim).toList();
            clientService.getClient().removeEntitiesFromEntityGroup(entityGroupIdStr, entityIds);
            return "{\"success\": true}";
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("id", entityGroupIdStr);
            err.put("message", e.getMessage());
            return JsonUtils.toString(err);
        }
    }

    @PeOnly
    @Tool(description = "Use this to get an entity group by its id. Returns EntityGroupInfo with owner IDs. PE only.")
    public String getEntityGroupById(@ToolParam(description = ENTITY_GROUP_ID_PARAM_DESCRIPTION) @NotBlank String entityGroupId) {
        if (ThingsBoardEdition.CE == clientService.getEdition()) {
            return PE_ONLY_AVAILABLE;
        }
        return JsonUtils.toString(clientService.getClient().getEntityGroupById(entityGroupId));
    }

    @PeOnly
    @Tool(description = "Use this to list all entity groups of a specific type (CUSTOMER, ASSET, DEVICE, USER, ENTITY_VIEW, DASHBOARD, EDGE). PE only.")
    public String getEntityGroupsByType(
            @ToolParam(description = ENTITY_GROUP_TYPE_PARAMETER_DESCRIPTION + ". Allowed types: 'CUSTOMER', 'ASSET', 'DEVICE', 'USER', 'ENTITY_VIEW', 'DASHBOARD', 'EDGE'") String entityType) {
        if (ThingsBoardEdition.CE == clientService.getEdition()) {
            return PE_ONLY_AVAILABLE;
        }
        if (!ALLOWED_TYPES.contains(entityType)) {
            throw new IllegalArgumentException("Unsupported entityType: " + entityType + ". Allowed: CUSTOMER, ASSET, DEVICE, USER, ENTITY_VIEW, DASHBOARD, EDGE");
        }
        return JsonUtils.toString(clientService.getClient().getAllEntityGroupsByType(entityType, null));
    }

    @PeOnly
    @Tool(description = "Use this to get an entity group by owner, type, and name. PE only.")
    public String getEntityGroupByOwnerAndNameAndType(
            @ToolParam(description = "Owner entity type: TENANT or CUSTOMER") String strOwnerType,
            @ToolParam(description = "Owner entity id (UUID)") String strOwnerId,
            @ToolParam(description = ENTITY_GROUP_TYPE_PARAMETER_DESCRIPTION + ". Allowed types: 'CUSTOMER', 'ASSET', 'DEVICE', 'USER', 'ENTITY_VIEW', 'DASHBOARD', 'EDGE'") String entityType,
            @ToolParam(description = "Entity Group name") String name) {
        if (ThingsBoardEdition.CE == clientService.getEdition()) {
            return PE_ONLY_AVAILABLE;
        }
        if (!ALLOWED_TYPES.contains(entityType)) {
            throw new IllegalArgumentException("Unsupported entityType: " + entityType + ". Allowed: CUSTOMER, ASSET, DEVICE, USER, ENTITY_VIEW, DASHBOARD, EDGE");
        }
        return JsonUtils.toString(clientService.getClient().getEntityGroupByOwnerAndNameAndType(strOwnerType, strOwnerId, entityType, name));
    }

    @PeOnly
    @Tool(description = "Use this to list entity groups by owner and type. PE only.")
    public String getEntityGroupsByOwnerAndType(
            @ToolParam(description = "Owner entity type: TENANT or CUSTOMER") @NotBlank String strOwnerType,
            @ToolParam(description = "Owner entity id (UUID)") @NotBlank String strOwnerId,
            @ToolParam(description = ENTITY_GROUP_TYPE_PARAMETER_DESCRIPTION + ". Allowed types: 'CUSTOMER', 'ASSET', 'DEVICE', 'USER', 'ENTITY_VIEW', 'DASHBOARD', 'EDGE'") @NotBlank String entityType) {
        if (ThingsBoardEdition.CE == clientService.getEdition()) {
            return PE_ONLY_AVAILABLE;
        }
        if (!ALLOWED_TYPES.contains(entityType)) {
            throw new IllegalArgumentException("Unsupported entityType: " + entityType + ". Allowed: CUSTOMER, ASSET, DEVICE, USER, ENTITY_VIEW, DASHBOARD, EDGE");
        }
        return JsonUtils.toString(clientService.getClient().getEntityGroupsByOwnerAndTypeAndPageLink(
                strOwnerType, strOwnerId, entityType, "1000", "0", null, null, null
        ));
    }

    @PeOnly
    @Tool(description = "Use this to list all groups containing a specific entity. Always includes the 'All' group. PE only.")
    public String getEntityGroupsForEntity(
            @ToolParam(description = ENTITY_GROUP_TYPE_PARAMETER_DESCRIPTION + ". Allowed types: 'CUSTOMER', 'ASSET', 'DEVICE', 'USER', 'ENTITY_VIEW', 'DASHBOARD', 'EDGE'") String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) String strEntityId) {
        if (ThingsBoardEdition.CE == clientService.getEdition()) {
            return PE_ONLY_AVAILABLE;
        }
        if (!ALLOWED_TYPES.contains(entityType)) {
            throw new IllegalArgumentException("Unsupported entityType: " + entityType + ". Allowed: CUSTOMER, ASSET, DEVICE, USER, ENTITY_VIEW, DASHBOARD, EDGE");
        }
        return JsonUtils.toString(clientService.getClient().getEntityGroupsForEntity(entityType, strEntityId));
    }

    @PeOnly
    @Tool(description = "Use this to get multiple entity groups by their ids (comma-separated). PE only.")
    public String getEntityGroupsByIds(@ToolParam(description = "A string of entity ids, separated by comma ','") @NotBlank String entityIds) {
        if (ThingsBoardEdition.CE == clientService.getEdition()) {
            return PE_ONLY_AVAILABLE;
        }
        List<String> entityGroupIds = Arrays.stream(entityIds.split(",")).map(String::trim).toList();
        return JsonUtils.toString(clientService.getClient().getEntityGroupsByIds(entityGroupIds));
    }

}
