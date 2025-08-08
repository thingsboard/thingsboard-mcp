package org.thingsboard.ai.mcp.server.tools.relation;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.McpTools;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;

import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.RELATION_INFO_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.RELATION_TYPE_GROUP_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.RELATION_TYPE_PARAM_DESCRIPTION;

@Service
@RequiredArgsConstructor
public class RelationTools implements McpTools {

    private final RestClientService clientService;

    private static final String SECURITY_CHECKS_ENTITIES_DESCRIPTION = "\n\nIf the user has the authority of 'System Administrator', the server checks that 'from' and 'to' entities are owned by the sysadmin. " +
            "If the user has the authority of 'Tenant Administrator', the server checks that 'from' and 'to' entities are owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the 'from' and 'to' entities are assigned to the same customer.";

    @Tool(description = "Returns relation object between two specified entities if present. Otherwise throws exception. " + SECURITY_CHECKS_ENTITIES_DESCRIPTION)
    public String getRelation(
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) String strFromId,
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) String strFromType,
            @ToolParam(description = RELATION_TYPE_PARAM_DESCRIPTION) String relationType,
            @ToolParam(required = false, description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION) String strRelationTypeGroup,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) String strToId,
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) String strToType) {
        EntityId fromId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        EntityId toId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup);
        return JacksonUtil.toString(clientService.getClient().getRelation(fromId, relationType, typeGroup, toId));
    }

    @Tool(description = "Returns list of relation objects for the specified entity by the 'from' direction. " + SECURITY_CHECKS_ENTITIES_DESCRIPTION)
    public String findByFrom(
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) String strFromId,
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) String strFromType,
            @ToolParam(required = false, description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION) String strRelationTypeGroup) {
        EntityId entityId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup);
        return JacksonUtil.toString(clientService.getClient().findByFrom(entityId, typeGroup));
    }

    @Tool(description = "Returns list of relation info objects for the specified entity by the 'from' direction. " + SECURITY_CHECKS_ENTITIES_DESCRIPTION + " " + RELATION_INFO_DESCRIPTION)
    public String findInfoByFrom(
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) String strFromId,
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) String strFromType,
            @ToolParam(required = false, description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION) String strRelationTypeGroup) {
        EntityId entityId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup);
        return JacksonUtil.toString(clientService.getClient().findInfoByFrom(entityId, typeGroup));
    }

    @Tool(description = "Returns list of relation objects for the specified entity by the 'from' direction and relation type. " + SECURITY_CHECKS_ENTITIES_DESCRIPTION)
    public String findByFrom(
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) String strFromId,
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) String strFromType,
            @ToolParam(description = RELATION_TYPE_PARAM_DESCRIPTION) String relationType,
            @ToolParam(required = false, description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION) String strRelationTypeGroup) {
        EntityId entityId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup);
        return JacksonUtil.toString(clientService.getClient().findByFrom(entityId, relationType, typeGroup));
    }

    @Tool(description = "Returns list of relation objects for the specified entity by the 'to' direction. " + SECURITY_CHECKS_ENTITIES_DESCRIPTION)
    public String findByTo(
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) String strToId,
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) String strToType,
            @ToolParam(required = false, description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION) String strRelationTypeGroup) {
        EntityId entityId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup);
        return JacksonUtil.toString(clientService.getClient().findByTo(entityId, typeGroup));
    }

    @Tool(description = "Returns list of relation info objects for the specified entity by the 'to' direction. " + SECURITY_CHECKS_ENTITIES_DESCRIPTION + " " + RELATION_INFO_DESCRIPTION)
    public String findInfoByTo(
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) String strToId,
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) String strToType,
            @ToolParam(required = false, description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION) String strRelationTypeGroup) {
        EntityId entityId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup);
        return JacksonUtil.toString(clientService.getClient().findByTo(entityId, typeGroup));
    }

    @Tool(description = "Returns list of relation objects for the specified entity by the 'to' direction and relation type. " + SECURITY_CHECKS_ENTITIES_DESCRIPTION)
    public String findByTo(
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) String strToId,
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) String strToType,
            @ToolParam(description = RELATION_TYPE_PARAM_DESCRIPTION) String relationType,
            @ToolParam(required = false, description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION) String strRelationTypeGroup) {
        EntityId entityId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup);
        return JacksonUtil.toString(clientService.getClient().findByTo(entityId, relationType, typeGroup));
    }

    private RelationTypeGroup parseRelationTypeGroup(String strRelationTypeGroup) {
        RelationTypeGroup result = RelationTypeGroup.COMMON;
        if (StringUtils.isNotBlank(strRelationTypeGroup)) {
            try {
                result = RelationTypeGroup.valueOf(strRelationTypeGroup);
            } catch (IllegalArgumentException ignored) {}
        }
        return result;
    }

}
