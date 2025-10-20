package org.thingsboard.ai.mcp.server.tools.relation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;

import java.util.HashMap;
import java.util.Map;

import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.RELATION_INFO_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.RELATION_TYPE_GROUP_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.RELATION_TYPE_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

@Service
@RequiredArgsConstructor
public class RelationTools implements McpTools {

    private final RestClientService clientService;

    private static final String RELATION_JSON_EXAMPLE =
            """
                    ```json
                    {
                      "from": { "entityType": "ASSET",  "id": "784f394c-42b6-435a-983c-b7beff2784f9" },
                      "to":   { "entityType": "DEVICE", "id": "784f394c-42b6-435a-983c-b7beff2784f2" },
                      "type": "Contains",
                      "typeGroup": "COMMON",
                      "additionalInfo": { "edge": false }
                    }
                    ```""";

    private static final String SECURITY_CHECKS_ENTITIES_DESCRIPTION = "\n\nIf the user has the authority of 'System Administrator', the server checks that 'from' and 'to' entities are owned by the sysadmin. " +
            "If the user has the authority of 'Tenant Administrator', the server checks that 'from' and 'to' entities are owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the 'from' and 'to' entities are assigned to the same customer.";

    @Tool(description =
            "Create or update a relation between two entities. If a relation with the same (from, to, typeGroup, type) exists, it is updated; " +
                    "otherwise, a new relation is created.\n\n" +
                    "### Keys & Defaults\n" +
                    "- Unique key: (from.id, to.id, typeGroup, type)\n" +
                    "- `typeGroup` defaults to **COMMON** when omitted\n" +
                    "- `additionalInfo` is optional JSON\n\n" +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String saveRelation(
            @ToolParam(description = "A JSON string representing the EntityRelation." + RELATION_JSON_EXAMPLE)
            @NotBlank @Valid String relationJson) {
        EntityRelation relation = JacksonUtil.fromString(relationJson, EntityRelation.class);
        if (relation.getTypeGroup() == null) {
            relation.setTypeGroup(RelationTypeGroup.COMMON);
        }
        return JacksonUtil.toString(clientService.getClient().saveRelationV2(relation));
    }

    @Tool(description =
            "Delete a relation between two entities. Requires from/to ids & types, relation type, and optional relationTypeGroup (defaults to COMMON). " +
                    "Deletes exactly the relation identified by (from, to, typeGroup, type). " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String deleteRelation(
            @ToolParam(description = "From entity id (UUID).") @NotBlank String fromIdStr,
            @ToolParam(description = "From entity type (e.g., DEVICE, ASSET, TENANT, CUSTOMER, etc.).") @NotBlank String fromTypeStr,
            @ToolParam(description = "Relation type (e.g., Contains, Manages, Uses).") @NotBlank String relationType,
            @ToolParam(required = false, description = "Relation type group (default: COMMON).") String relationTypeGroupStr,
            @ToolParam(description = "To entity id (UUID).") @NotBlank String toIdStr,
            @ToolParam(description = "To entity type (e.g., DEVICE, ASSET, TENANT, CUSTOMER, etc.).") @NotBlank String toTypeStr) {
        try {
            EntityId fromId = EntityIdFactory.getByTypeAndId(fromTypeStr, fromIdStr);
            EntityId toId = EntityIdFactory.getByTypeAndId(toTypeStr, toIdStr);
            RelationTypeGroup relationTypeGroup;
            try {
                relationTypeGroup = RelationTypeGroup.valueOf(relationTypeGroupStr);
            } catch (Exception e) {
                relationTypeGroup = RelationTypeGroup.COMMON;
            }
            clientService.getClient().deleteRelationV2(
                    fromId, relationType,
                    relationTypeGroup,
                    toId
            );
            return "{\"status\":\"OK\"}";
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", e.getMessage());
            return JacksonUtil.toString(err);
        }
    }

    @Tool(description = "Delete all relations (both 'from' and 'to' directions) for the specified entity within the COMMON relation group. "
            + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String deleteRelations(
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String strEntityId,
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String strEntityType) {
        try {
            EntityId entityId = EntityIdFactory.getByTypeAndId(strEntityType, strEntityId);
            clientService.getClient().deleteRelations(entityId);
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

    @Tool(description = "Returns relation object between two specified entities if present. Otherwise throws exception. " + SECURITY_CHECKS_ENTITIES_DESCRIPTION)
    public String getRelation(
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String strFromId,
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String strFromType,
            @ToolParam(description = RELATION_TYPE_PARAM_DESCRIPTION) @NotBlank String relationType,
            @ToolParam(required = false, description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION) String strRelationTypeGroup,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String strToId,
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String strToType) {
        EntityId fromId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        EntityId toId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup);
        return JacksonUtil.toString(clientService.getClient().getRelation(fromId, relationType, typeGroup, toId));
    }

    @Tool(description = "Returns list of relation objects for the specified entity by the 'from' direction. " + SECURITY_CHECKS_ENTITIES_DESCRIPTION)
    public String findByFrom(
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String strFromId,
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String strFromType,
            @ToolParam(required = false, description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION) String strRelationTypeGroup) {
        EntityId entityId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup);
        return JacksonUtil.toString(clientService.getClient().findByFrom(entityId, typeGroup));
    }

    @Tool(description = "Returns list of relation info objects for the specified entity by the 'from' direction. " + SECURITY_CHECKS_ENTITIES_DESCRIPTION + " " + RELATION_INFO_DESCRIPTION)
    public String findInfoByFrom(
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String strFromId,
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String strFromType,
            @ToolParam(required = false, description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION) String strRelationTypeGroup) {
        EntityId entityId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup);
        return JacksonUtil.toString(clientService.getClient().findInfoByFrom(entityId, typeGroup));
    }

    @Tool(description = "Returns list of relation objects for the specified entity by the 'from' direction and relation type. " + SECURITY_CHECKS_ENTITIES_DESCRIPTION)
    public String findByFromWithRelationType(
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String strFromId,
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String strFromType,
            @ToolParam(description = RELATION_TYPE_PARAM_DESCRIPTION) @NotBlank String relationType,
            @ToolParam(required = false, description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION) String strRelationTypeGroup) {
        EntityId entityId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup);
        return JacksonUtil.toString(clientService.getClient().findByFrom(entityId, relationType, typeGroup));
    }

    @Tool(description = "Returns list of relation objects for the specified entity by the 'to' direction. " + SECURITY_CHECKS_ENTITIES_DESCRIPTION)
    public String findByTo(
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String strToId,
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String strToType,
            @ToolParam(required = false, description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION) String strRelationTypeGroup) {
        EntityId entityId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup);
        return JacksonUtil.toString(clientService.getClient().findByTo(entityId, typeGroup));
    }

    @Tool(description = "Returns list of relation info objects for the specified entity by the 'to' direction. " + SECURITY_CHECKS_ENTITIES_DESCRIPTION + " " + RELATION_INFO_DESCRIPTION)
    public String findInfoByTo(
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String strToId,
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String strToType,
            @ToolParam(required = false, description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION) String strRelationTypeGroup) {
        EntityId entityId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup);
        return JacksonUtil.toString(clientService.getClient().findInfoByTo(entityId, typeGroup));
    }

    @Tool(description = "Returns list of relation objects for the specified entity by the 'to' direction and relation type. " + SECURITY_CHECKS_ENTITIES_DESCRIPTION)
    public String findByToWithRelationType(
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String strToId,
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String strToType,
            @ToolParam(description = RELATION_TYPE_PARAM_DESCRIPTION) @NotBlank String relationType,
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
