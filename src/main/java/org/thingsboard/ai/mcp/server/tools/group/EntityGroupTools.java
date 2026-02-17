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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

    private static final Set<EntityType> ALLOWED_TYPES = Set.of(EntityType.CUSTOMER, EntityType.ASSET, EntityType.DEVICE, EntityType.USER, EntityType.ENTITY_VIEW, EntityType.DASHBOARD, EntityType.EDGE);

    private final RestClientService clientService;

    @PeOnly
    @Tool(description = "Use this to create or update an entity group. Omit 'id' to create; include 'id' to update. Group names are unique per owner + entity type. Supported types: CUSTOMER, ASSET, DEVICE, USER, ENTITY_VIEW, DASHBOARD, EDGE. PE only.")
    public String saveEntityGroup(
            @ToolParam(description = "JSON entity group object. Omit 'id' to create; include 'id' to update. " + ENTITY_GROUP_JSON_EXAMPLE)
            @NotBlank @Valid String entityGroupJson) {
        EntityGroup eg = JacksonUtil.fromString(entityGroupJson, EntityGroup.class);
        return JacksonUtil.toString(clientService.getClient().saveEntityGroup(eg));
    }

    @PeOnly
    @Tool(description = "Use this to delete an entity group by id. Entities remain in the 'All' group. Cannot delete the 'All' group. PE only.")
    public String deleteEntityGroup(@ToolParam(description = ENTITY_GROUP_ID_PARAM_DESCRIPTION) @NotBlank String entityGroupIdStr) {
        try {
            EntityGroupId entityGroupId = new EntityGroupId(UUID.fromString(entityGroupIdStr));
            clientService.getClient().deleteEntityGroup(entityGroupId);
            return "{\"status\":\"OK\",\"id\":\"" + entityGroupId + "\"}";
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("id", entityGroupIdStr);
            err.put("message", e.getMessage());
            return JacksonUtil.toString(err);
        }
    }

    @PeOnly
    @Tool(description = "Use this to add entities to an entity group. Cannot add to the 'All' group. PE only.")
    public String addEntitiesToEntityGroup(
            @ToolParam(description = ENTITY_GROUP_ID_PARAM_DESCRIPTION) @NotBlank String entityGroupIdStr,
            @ToolParam(description = "Comma-separated list of entity IDs (UUIDs). to be added to the entity group") @NotBlank String entityIdsStr,
            @ToolParam(description = ENTITY_GROUP_TYPE_PARAMETER_DESCRIPTION) @NotBlank String entityGroupType) {
        try {
            List<EntityId> entityIds = Arrays.stream(entityIdsStr.split(",")).map(entityId -> EntityIdFactory.getByTypeAndId(entityGroupType, entityId)).toList();
            EntityGroupId entityGroupId = new EntityGroupId(UUID.fromString(entityGroupIdStr));
            clientService.getClient().addEntitiesToEntityGroup(entityGroupId, entityIds);
            return "{\"status\":\"OK\"}";
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("id", entityGroupIdStr);
            err.put("message", e.getMessage());
            return JacksonUtil.toString(err);
        }
    }

    @PeOnly
    @Tool(description = "Use this to remove entities from an entity group. Cannot remove from the 'All' group. PE only.")
    public String removeEntitiesFromEntityGroup(
            @ToolParam(description = "Entity Group ID (UUID).") @NotBlank String entityGroupIdStr,
            @ToolParam(description = "Comma-separated list of entity IDs (UUIDs) to be removed from the entity group.") @NotBlank String entityIdsStr,
            @ToolParam(description = ENTITY_GROUP_TYPE_PARAMETER_DESCRIPTION) @NotBlank String entityGroupType) {
        try {
            List<EntityId> entityIds = Arrays.stream(entityIdsStr.split(",")).map(entityId -> EntityIdFactory.getByTypeAndId(entityGroupType, entityId)).toList();
            EntityGroupId entityGroupId = new EntityGroupId(UUID.fromString(entityGroupIdStr));
            clientService.getClient().removeEntitiesFromEntityGroup(entityGroupId, entityIds);
            return "{\"success\": true}";
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("id", entityGroupIdStr);
            err.put("message", e.getMessage());
            return JacksonUtil.toString(err);
        }
    }

    @PeOnly
    @Tool(description = "Use this to get an entity group by its id. Returns EntityGroupInfo with owner IDs. PE only.")
    public String getEntityGroupById(@ToolParam(description = ENTITY_GROUP_ID_PARAM_DESCRIPTION) @NotBlank String entityGroupId) {
        if (ThingsBoardEdition.CE == clientService.getEdition()) {
            return PE_ONLY_AVAILABLE;
        }
        return JacksonUtil.toString(clientService.getClient().getEntityGroupById(new EntityGroupId(UUID.fromString(entityGroupId))));
    }

    @PeOnly
    @Tool(description = "Use this to list all entity groups of a specific type (CUSTOMER, ASSET, DEVICE, USER, ENTITY_VIEW, DASHBOARD, EDGE). PE only.")
    public String getEntityGroupsByType(
            @ToolParam(description = ENTITY_GROUP_TYPE_PARAMETER_DESCRIPTION + ". Allowed types: 'CUSTOMER', 'ASSET', 'DEVICE', 'USER', 'ENTITY_VIEW', 'DASHBOARD', 'EDGE'") String entityType) {
        if (ThingsBoardEdition.CE == clientService.getEdition()) {
            return PE_ONLY_AVAILABLE;
        }
        EntityType type = EntityType.valueOf(entityType);
        if (!ALLOWED_TYPES.contains(type)) {
            throw new IllegalArgumentException("Unsupported entityType: " + type + ". Allowed: CUSTOMER, ASSET, DEVICE, USER, ENTITY_VIEW, DASHBOARD, EDGE");
        }
        return JacksonUtil.toString(clientService.getClient().getEntityGroupsByType(type));
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
        EntityId ownerId = EntityIdFactory.getByTypeAndId(strOwnerType, strOwnerId);
        EntityType type = EntityType.valueOf(entityType);
        if (!ALLOWED_TYPES.contains(type)) {
            throw new IllegalArgumentException("Unsupported entityType: " + type + ". Allowed: CUSTOMER, ASSET, DEVICE, USER, ENTITY_VIEW, DASHBOARD, EDGE");
        }
        return JacksonUtil.toString(clientService.getClient().getEntityGroupInfoByOwnerAndNameAndType(ownerId, type, name));
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
        EntityId ownerId = EntityIdFactory.getByTypeAndId(strOwnerType, strOwnerId);
        EntityType type = EntityType.valueOf(entityType);
        if (!ALLOWED_TYPES.contains(type)) {
            throw new IllegalArgumentException("Unsupported entityType: " + type + ". Allowed: CUSTOMER, ASSET, DEVICE, USER, ENTITY_VIEW, DASHBOARD, EDGE");
        }
        return JacksonUtil.toString(clientService.getClient().getEntityGroupsByOwnerAndType(ownerId, type));
    }

    @PeOnly
    @Tool(description = "Use this to list all groups containing a specific entity. Always includes the 'All' group. PE only.")
    public String getEntityGroupsForEntity(
            @ToolParam(description = ENTITY_GROUP_TYPE_PARAMETER_DESCRIPTION + ". Allowed types: 'CUSTOMER', 'ASSET', 'DEVICE', 'USER', 'ENTITY_VIEW', 'DASHBOARD', 'EDGE'") String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) String strEntityId) {
        if (ThingsBoardEdition.CE == clientService.getEdition()) {
            return PE_ONLY_AVAILABLE;
        }
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, strEntityId);
        if (!ALLOWED_TYPES.contains(entityId.getEntityType())) {
            throw new IllegalArgumentException("Unsupported entityType: " + entityId.getEntityType() + ". Allowed: CUSTOMER, ASSET, DEVICE, USER, ENTITY_VIEW, DASHBOARD, EDGE");
        }
        return JacksonUtil.toString(clientService.getClient().getEntityGroupsForEntity(entityId));
    }

    @PeOnly
    @Tool(description = "Use this to get multiple entity groups by their ids (comma-separated). PE only.")
    public String getEntityGroupsByIds(@ToolParam(description = "A string of entity ids, separated by comma ','") @NotBlank String entityIds) {
        if (ThingsBoardEdition.CE == clientService.getEdition()) {
            return PE_ONLY_AVAILABLE;
        }
        List<EntityGroupId> entityGroupIds = Arrays.stream(entityIds.split(",")).map(UUID::fromString).map(EntityGroupId::new).toList();
        return JacksonUtil.toString(clientService.getClient().getEntityGroupsByIds(entityGroupIds));
    }

}
