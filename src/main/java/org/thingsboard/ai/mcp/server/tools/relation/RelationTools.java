package org.thingsboard.ai.mcp.server.tools.relation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.annotation.ToolGroup;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.McpTools;
import org.thingsboard.ai.mcp.server.util.JacksonUtil;
import org.thingsboard.client.model.EntityRelation;
import org.thingsboard.client.model.RelationTypeGroup;

import java.util.HashMap;
import java.util.Map;

import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.RELATION_TYPE_GROUP_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.RELATION_TYPE_PARAM_DESCRIPTION;

@Service
@RequiredArgsConstructor
@ToolGroup("relation")
public class RelationTools implements McpTools {

    private final RestClientService clientService;

    private static final String RELATION_JSON_EXAMPLE = """
            {
              "from": {"entityType": "ASSET", "id": "<fromId>"},
              "to": {"entityType": "DEVICE", "id": "<toId>"},
              "type": "Contains",
              "typeGroup": "COMMON"
            }""";

    @Tool(description = "Use this to create or update a relation between two entities. Unique key: (from, to, typeGroup, type). 'typeGroup' defaults to COMMON.")
    public String saveRelation(
            @ToolParam(description = "JSON relation object. " + RELATION_JSON_EXAMPLE)
            @NotBlank @Valid String relationJson) {
        EntityRelation relation = JacksonUtil.fromString(relationJson, EntityRelation.class);
        if (relation.getTypeGroup() == null) {
            relation.typeGroup(RelationTypeGroup.COMMON);
        }
        return JacksonUtil.toString(clientService.getClient().saveRelation(relation));
    }

    @Tool(description = "Use this to delete a specific relation identified by (from, to, typeGroup, type). Defaults typeGroup to COMMON.")
    public String deleteRelation(
            @ToolParam(description = "From entity id (UUID).") @NotBlank String fromIdStr,
            @ToolParam(description = "From entity type (e.g., DEVICE, ASSET, TENANT, CUSTOMER, etc.).") @NotBlank String fromTypeStr,
            @ToolParam(description = "Relation type (e.g., Contains, Manages, Uses).") @NotBlank String relationType,
            @ToolParam(required = false, description = "Relation type group (default: COMMON).") String relationTypeGroupStr,
            @ToolParam(description = "To entity id (UUID).") @NotBlank String toIdStr,
            @ToolParam(description = "To entity type (e.g., DEVICE, ASSET, TENANT, CUSTOMER, etc.).") @NotBlank String toTypeStr) {
        try {
            String relationTypeGroup = parseRelationTypeGroup(relationTypeGroupStr).name();
            clientService.getClient().deleteRelation(
                    fromIdStr, fromTypeStr, relationType,
                    toIdStr, toTypeStr, relationTypeGroup
            );
            return "{\"status\":\"OK\"}";
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", e.getMessage());
            return JacksonUtil.toString(err);
        }
    }

    @Tool(description = "Use this to delete all relations (both directions) for an entity in the COMMON group.")
    public String deleteRelations(
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String strEntityId,
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String strEntityType) {
        try {
            clientService.getClient().deleteRelations(strEntityId, strEntityType);
            return "{\"status\":\"OK\"}";
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("entityId", strEntityId);
            err.put("entityType", strEntityType);
            err.put("message", e.getMessage());
            return JacksonUtil.toString(err);
        }
    }

    @Tool(description = "Use this to get a specific relation between two entities.")
    public String getRelation(
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String strFromId,
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String strFromType,
            @ToolParam(description = RELATION_TYPE_PARAM_DESCRIPTION) @NotBlank String relationType,
            @ToolParam(required = false, description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION) String strRelationTypeGroup,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String strToId,
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String strToType) {
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup);
        return JacksonUtil.toString(clientService.getClient().getRelation(
                strFromId, strFromType, relationType,
                strToId, strToType, typeGroup.name()
        ));
    }

    @Tool(description = "Use this to list all outgoing relations (FROM direction) for an entity. Returns RelationInfo with entity names.")
    public String findInfoByFrom(
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String strFromId,
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String strFromType,
            @ToolParam(required = false, description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION) String strRelationTypeGroup) {
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup);
        return JacksonUtil.toString(clientService.getClient().findEntityRelationInfosByFrom(
                strFromType, strFromId, typeGroup.name()
        ));
    }

    @Tool(description = "Use this to list outgoing relations of a specific type for an entity.")
    public String findByFromWithRelationType(
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String strFromId,
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String strFromType,
            @ToolParam(description = RELATION_TYPE_PARAM_DESCRIPTION) @NotBlank String relationType,
            @ToolParam(required = false, description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION) String strRelationTypeGroup) {
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup);
        return JacksonUtil.toString(clientService.getClient().findEntityRelationsByFromAndRelationType(
                strFromType, strFromId, relationType, typeGroup.name()
        ));
    }

    @Tool(description = "Use this to list all incoming relations (TO direction) for an entity. Returns RelationInfo with entity names.")
    public String findInfoByTo(
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String strToId,
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String strToType,
            @ToolParam(required = false, description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION) String strRelationTypeGroup) {
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup);
        return JacksonUtil.toString(clientService.getClient().findEntityRelationInfosByTo(
                strToType, strToId, typeGroup.name()
        ));
    }

    @Tool(description = "Use this to list incoming relations of a specific type for an entity.")
    public String findByToWithRelationType(
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String strToId,
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String strToType,
            @ToolParam(description = RELATION_TYPE_PARAM_DESCRIPTION) @NotBlank String relationType,
            @ToolParam(required = false, description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION) String strRelationTypeGroup) {
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup);
        return JacksonUtil.toString(clientService.getClient().findEntityRelationsByToAndRelationType(
                strToType, strToId, relationType, typeGroup.name()
        ));
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
