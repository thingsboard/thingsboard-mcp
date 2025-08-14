package org.thingsboard.ai.mcp.server.tools.group;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.annotation.PeOnly;
import org.thingsboard.ai.mcp.server.data.ThingsBoardEdition;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.McpTools;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_GROUP_ID;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PE_ONLY_AVAILABLE;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.RBAC_GROUP_READ_CHECK;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

@Service
@RequiredArgsConstructor
public class EntityGroupTools implements McpTools {

    private static final String ENTITY_GROUP_DESCRIPTION = "Entity group allows you to group multiple entities of the same entity type (Device, Asset, Customer, User, Dashboard, etc). " +
            "Entity Group always have an owner - particular Tenant or Customer. Each entity may belong to multiple groups simultaneously.";
    private static final String ENTITY_GROUP_INFO_DESCRIPTION = "Entity Group Info extends Entity Group object and adds 'ownerIds' - a list of owner ids.";
    private static final String ENTITY_GROUP_UNIQUE_KEY = "Entity group name is unique in the scope of owner and entity type. For example, you can't create two tenant device groups called 'Water meters'. " +
            "However, you may create device and asset group with the same name. And also you may create groups with the same name for two different customers of the same tenant. ";
    private static final String OWNER_TYPE_DESCRIPTION = "Tenant or Customer";
    private static final String OWNER_ID_DESCRIPTION = "A string value representing the Tenant or Customer id";
    private static final String ENTITY_GROUP_TYPE_PARAMETER_DESCRIPTION = "Entity Group type";

    private static final Set<EntityType> ALLOWED_TYPES = Set.of(EntityType.CUSTOMER, EntityType.ASSET, EntityType.DEVICE, EntityType.USER, EntityType.ENTITY_VIEW, EntityType.DASHBOARD, EntityType.EDGE);

    private final RestClientService clientService;

    @PeOnly
    @Tool(description = "Fetch the Entity Group object based on the provided Entity Group Id. "
            + PE_ONLY_AVAILABLE + ENTITY_GROUP_DESCRIPTION + ENTITY_GROUP_INFO_DESCRIPTION +
            "\n\n" + ENTITY_GROUP_UNIQUE_KEY +
            TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_GROUP_READ_CHECK)
    public String getEntityGroupById(@ToolParam(description = ENTITY_GROUP_ID) @NotBlank String entityGroupId) {
        if (ThingsBoardEdition.CE == clientService.getEdition()) {
            return PE_ONLY_AVAILABLE;
        }
        return JacksonUtil.toString(clientService.getClient().getEntityGroupById(new EntityGroupId(UUID.fromString(entityGroupId))));
    }

    @PeOnly
    @Tool(description = "Fetch the list of Entity Group Info objects based on the provided Entity Type. "
            + PE_ONLY_AVAILABLE + ENTITY_GROUP_DESCRIPTION + ENTITY_GROUP_INFO_DESCRIPTION +
            TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_GROUP_READ_CHECK)
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
    @Tool(description = "Fetch the Entity Group object based on the provided owner, type and name. "
            + PE_ONLY_AVAILABLE + ENTITY_GROUP_DESCRIPTION + ENTITY_GROUP_INFO_DESCRIPTION +
            TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_GROUP_READ_CHECK)
    public String getEntityGroupByOwnerAndNameAndType(
            @ToolParam(description = OWNER_TYPE_DESCRIPTION) String strOwnerType,
            @ToolParam(description = OWNER_ID_DESCRIPTION) String strOwnerId,
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
    @Tool(description = "Fetch the list of Entity Group Info objects based on the provided Owner Id and Entity Type. "
            + PE_ONLY_AVAILABLE + ENTITY_GROUP_DESCRIPTION + ENTITY_GROUP_INFO_DESCRIPTION +
            TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_GROUP_READ_CHECK)
    public String getEntityGroupsByOwnerAndType(
            @ToolParam(description = OWNER_TYPE_DESCRIPTION) @NotBlank String strOwnerType,
            @ToolParam(description = OWNER_ID_DESCRIPTION) @NotBlank String strOwnerId,
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
    @Tool(description = "Returns a list of groups that contain the specified Entity Id. " +
            "For example, all device groups that contain specific device. " +
            "The list always contain at least one element - special group 'All'." + PE_ONLY_AVAILABLE +
            PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
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
    @Tool(description = "Fetch the list of Entity Group Info objects based on the provided entity group ids list. "
            + PE_ONLY_AVAILABLE + ENTITY_GROUP_DESCRIPTION + ENTITY_GROUP_INFO_DESCRIPTION +
            TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_GROUP_READ_CHECK)
    public String getEntityGroupsByIds(@ToolParam(description = "A list of entity ids, separated by comma ','") @NotBlank String... entityIds) {
        if (ThingsBoardEdition.CE == clientService.getEdition()) {
            return PE_ONLY_AVAILABLE;
        }
        return JacksonUtil.toString(clientService.getClient().getEntityGroupsByIds(Arrays.stream(entityIds).map(UUID::fromString).map(EntityGroupId::new).toList()));
    }

}
