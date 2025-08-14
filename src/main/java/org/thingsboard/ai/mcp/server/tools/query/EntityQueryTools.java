package org.thingsboard.ai.mcp.server.tools.query;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.McpTools;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityDataQuery;

import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ASSET_TYPE;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.DEVICE_TYPE;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.EDGE_TYPE;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_COUNT_QUERY_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_TYPE_FILTER;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

@Service
@RequiredArgsConstructor
public class EntityQueryTools implements McpTools {

    private final RestClientService clientService;

    @Tool(description = "Count Entities by Query. " + ENTITY_COUNT_QUERY_DESCRIPTION)
    public String countEntitiesByQuery(
            @ToolParam(description = "A JSON value representing the entity count query. See description above for more details.")
            String strQuery) {
        EntityCountQuery query = JacksonUtil.fromString(strQuery, EntityCountQuery.class);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

//    @Tool(description = "Find Entity Data by Query. " + ENTITY_DATA_QUERY_DESCRIPTION)
//    public String findEntityDataByQuery(
//            @ToolParam(description = "A JSON value representing the entity data query. See description above for more details.")
//            String strQuery) {
//        EntityDataQuery query = JacksonUtil.fromString(strQuery, EntityDataQuery.class);
//        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
//    }

    @Tool(description = ENTITY_TYPE_FILTER + "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByEntityTypeQuery(
            @ToolParam(description = "A JSON value representing the entity query with 'entityType' filter type.") String strQuery) {
        EntityDataQuery query = JacksonUtil.fromString(strQuery, EntityDataQuery.class);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = DEVICE_TYPE + "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByDeviceEntityTypeQuery(
            @ToolParam(description = "A JSON value representing the entity query with 'entityType' filter type.") String strQuery) {
        EntityDataQuery query = JacksonUtil.fromString(strQuery, EntityDataQuery.class);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = ASSET_TYPE + "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByAssetEntityTypeQuery(
            @ToolParam(description = "A JSON value representing the entity query with 'entityType' filter type.") String strQuery) {
        EntityDataQuery query = JacksonUtil.fromString(strQuery, EntityDataQuery.class);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = EDGE_TYPE + "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByEdgeEntityTypeQuery(
            @ToolParam(description = "A JSON value representing the entity query with 'entityType' filter type.") String strQuery) {
        EntityDataQuery query = JacksonUtil.fromString(strQuery, EntityDataQuery.class);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Find Alarm Data by Query. Allows to run complex queries to search the alarms based on the combination of alarm fields filter and multiple key filters. " +
            "Returns the paginated result of the query that contains requested alarm fields and latest values of requested attributes and time series data." +
            TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String findAlarmDataByQuery(
            @ToolParam(description = "A JSON value representing the alarm data query.")
            String strQuery) {
        AlarmDataQuery query = JacksonUtil.fromString(strQuery, AlarmDataQuery.class);
        return JacksonUtil.toString(clientService.getClient().findAlarmDataByQuery(query));
    }

    @Tool(description = "Count Alarms by Query. Allows to run complex queries to count the alarms based on the combination of alarm fields filter and multiple key filters. " +
            "Returns the number of alarms that match the query definition." +
            TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String countAlarmsByQuery(
            @ToolParam(description = "A JSON value representing the alarm count query.")
            String strQuery) {
        AlarmCountQuery query = JacksonUtil.fromString(strQuery, AlarmCountQuery.class);
        return JacksonUtil.toString(clientService.getClient().countAlarmsByQuery(query));
    }
//
//    // Entity Count Filter Type Specific Tools
//    @Tool(description = "Count using Single Entity Filter. Use this when you need to count only one entity based on its ID. " +
//            "For example, to count a specific device by its ID." + ENTITY_COUNT_QUERY_DESCRIPTION)
//    public String countSingleEntity(
//            @ToolParam(description = "A JSON value representing the entity count query with 'singleEntity' filter type.")
//            String strQuery) {
//        EntityCountQuery query = JacksonUtil.fromString(strQuery, EntityCountQuery.class);
//        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
//    }
//
//    @Tool(description = "Count using Entity Group Filter. Use this when you need to count multiple entities of the same type " +
//            "using the entity group type and ID. For example, to count all devices that belong to a specific group." + ENTITY_COUNT_QUERY_DESCRIPTION)
//    public String countEntityGroup(
//            @ToolParam(description = "A JSON value representing the entity count query with 'entityGroup' filter type.")
//            String strQuery) {
//        EntityCountQuery query = JacksonUtil.fromString(strQuery, EntityCountQuery.class);
//        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
//    }
//
//    @Tool(description = "Count using Entity List Filter. Use this when you need to count entities of the same type using their IDs. " +
//            "For example, to count specific devices by their IDs." + ENTITY_COUNT_QUERY_DESCRIPTION)
//    public String countEntityList(
//            @ToolParam(description = "A JSON value representing the entity count query with 'entityList' filter type.")
//            String strQuery) {
//        EntityCountQuery query = JacksonUtil.fromString(strQuery, EntityCountQuery.class);
//        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
//    }
//
//    @Tool(description = "Count using Entity Name Filter. Use this when you need to count entities of the same type using a 'starts with' " +
//            "expression over their name. For example, to count all devices which name starts with a specific prefix." + ENTITY_COUNT_QUERY_DESCRIPTION)
//    public String countEntityName(
//            @ToolParam(description = "A JSON value representing the entity count query with 'entityName' filter type.")
//            String strQuery) {
//        EntityCountQuery query = JacksonUtil.fromString(strQuery, EntityCountQuery.class);
//        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
//    }
//
//    @Tool(description = "Count using Entity Type Filter. Use this when you need to count entities based on their type " +
//            "(CUSTOMER, USER, DASHBOARD, ASSET, DEVICE, etc). For example, to count all tenant customers." + ENTITY_COUNT_QUERY_DESCRIPTION)
//    public String countEntityType(
//            @ToolParam(description = "A JSON value representing the entity count query with 'entityType' filter type.")
//            String strQuery) {
//        EntityCountQuery query = JacksonUtil.fromString(strQuery, EntityCountQuery.class);
//        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
//    }
//
//    @Tool(description = "Count using Entity Group List Filter. Use this when you need to count multiple groups of the same type " +
//            "using specified IDs. For example, to count specific device groups by their IDs." + ENTITY_COUNT_QUERY_DESCRIPTION)
//    public String countEntityGroupList(
//            @ToolParam(description = "A JSON value representing the entity count query with 'entityGroupList' filter type.")
//            String strQuery) {
//        EntityCountQuery query = JacksonUtil.fromString(strQuery, EntityCountQuery.class);
//        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
//    }
//
//    @Tool(description = "Count using Entity Group Name Filter. Use this when you need to count entity groups based on their type " +
//            "and a 'starts with' expression over their name. For example, to count all device groups which name starts with a specific prefix." + ENTITY_COUNT_QUERY_DESCRIPTION)
//    public String countEntityGroupName(
//            @ToolParam(description = "A JSON value representing the entity count query with 'entityGroupName' filter type.")
//            String strQuery) {
//        EntityCountQuery query = JacksonUtil.fromString(strQuery, EntityCountQuery.class);
//        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
//    }
//
//    @Tool(description = "Count using Entities by Group Name Filter. Use this when you need to count entities that belong to a group " +
//            "based on the entity type and the group name. For example, to count all devices which belong to a group with a specific name." + ENTITY_COUNT_QUERY_DESCRIPTION)
//    public String countEntitiesByGroupName(
//            @ToolParam(description = "A JSON value representing the entity count query with 'entitiesByGroupName' filter type.")
//            String strQuery) {
//        EntityCountQuery query = JacksonUtil.fromString(strQuery, EntityCountQuery.class);
//        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
//    }
//
//    @Tool(description = "Count using Entity Owner Filter. Use this when you need to count the owner (Tenant or Customer) " +
//            "of a specified entity. For example, to count the owner of a specific device." + ENTITY_COUNT_QUERY_DESCRIPTION)
//    public String countEntityOwner(
//            @ToolParam(description = "A JSON value representing the entity count query with 'stateEntityOwner' filter type.")
//            String strQuery) {
//        EntityCountQuery query = JacksonUtil.fromString(strQuery, EntityCountQuery.class);
//        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
//    }
//
//    @Tool(description = "Count using Asset Type Filter. Use this when you need to count assets based on their type " +
//            "and a 'starts with' expression over their name. For example, to count all assets of a specific type which name starts with a prefix." + ENTITY_COUNT_QUERY_DESCRIPTION)
//    public String countAssetType(
//            @ToolParam(description = "A JSON value representing the entity count query with 'assetType' filter type.")
//            String strQuery) {
//        EntityCountQuery query = JacksonUtil.fromString(strQuery, EntityCountQuery.class);
//        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
//    }
//
//    @Tool(description = "Count using Device Type Filter. Use this when you need to count devices based on their type " +
//            "and a 'starts with' expression over their name. For example, to count all devices of a specific type which name starts with a prefix." + ENTITY_COUNT_QUERY_DESCRIPTION)
//    public String countDeviceType(
//            @ToolParam(description = "A JSON value representing the entity count query with 'deviceType' filter type.")
//            String strQuery) {
//        EntityCountQuery query = JacksonUtil.fromString(strQuery, EntityCountQuery.class);
//        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
//    }
//
//    @Tool(description = "Count using Edge Type Filter. Use this when you need to count edge instances based on their type " +
//            "and a 'starts with' expression over their name. For example, to count all edge instances of a specific type which name starts with a prefix." + ENTITY_COUNT_QUERY_DESCRIPTION)
//    public String countEdgeType(
//            @ToolParam(description = "A JSON value representing the entity count query with 'edgeType' filter type.")
//            String strQuery) {
//        EntityCountQuery query = JacksonUtil.fromString(strQuery, EntityCountQuery.class);
//        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
//    }
//
//    @Tool(description = "Count using Entity View Type Filter. Use this when you need to count entity views based on their type " +
//            "and a 'starts with' expression over their name. For example, to count all entity views of a specific type which name starts with a prefix." + ENTITY_COUNT_QUERY_DESCRIPTION)
//    public String countEntityViewType(
//            @ToolParam(description = "A JSON value representing the entity count query with 'entityViewType' filter type.")
//            String strQuery) {
//        EntityCountQuery query = JacksonUtil.fromString(strQuery, EntityCountQuery.class);
//        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
//    }
//
//    @Tool(description = "Count using API Usage Filter. Use this when you need to count API Usage based on optional customer ID. " +
//            "If the customer ID is not set, returns current tenant API usage." + ENTITY_COUNT_QUERY_DESCRIPTION)
//    public String countApiUsage(
//            @ToolParam(description = "A JSON value representing the entity count query with 'apiUsageState' filter type.")
//            String strQuery) {
//        EntityCountQuery query = JacksonUtil.fromString(strQuery, EntityCountQuery.class);
//        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
//    }
//
//    @Tool(description = "Count using Relations Query Filter. Use this when you need to count entities that are related to a provided root entity. " +
//            "For example, to count all devices and assets which are related to a specific asset." + ENTITY_COUNT_QUERY_DESCRIPTION)
//    public String countRelations(
//            @ToolParam(description = "A JSON value representing the entity count query with 'relationsQuery' filter type.")
//            String strQuery) {
//        EntityCountQuery query = JacksonUtil.fromString(strQuery, EntityCountQuery.class);
//        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
//    }
//
//    @Tool(description = "Count using Asset Search Query Filter. Use this when you need to count assets that are related to a provided root entity " +
//            "based on relation type and asset types. For example, to count specific types of assets related to a specific asset." + ENTITY_COUNT_QUERY_DESCRIPTION)
//    public String countAssetSearch(
//            @ToolParam(description = "A JSON value representing the entity count query with 'assetSearchQuery' filter type.")
//            String strQuery) {
//        EntityCountQuery query = JacksonUtil.fromString(strQuery, EntityCountQuery.class);
//        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
//    }
//
//    @Tool(description = "Count using Device Search Query Filter. Use this when you need to count devices that are related to a provided root entity " +
//            "based on relation type and device types. For example, to count specific types of devices related to a specific asset." + ENTITY_COUNT_QUERY_DESCRIPTION)
//    public String countDeviceSearch(
//            @ToolParam(description = "A JSON value representing the entity count query with 'deviceSearchQuery' filter type.")
//            String strQuery) {
//        EntityCountQuery query = JacksonUtil.fromString(strQuery, EntityCountQuery.class);
//        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
//    }
//
//    @Tool(description = "Count using Entity View Search Query Filter. Use this when you need to count entity views that are related to a provided root entity " +
//            "based on relation type and entity view types. For example, to count specific types of entity views related to a specific asset." + ENTITY_COUNT_QUERY_DESCRIPTION)
//    public String countEntityViewSearch(
//            @ToolParam(description = "A JSON value representing the entity count query with 'entityViewSearchQuery' filter type.")
//            String strQuery) {
//        EntityCountQuery query = JacksonUtil.fromString(strQuery, EntityCountQuery.class);
//        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
//    }
//
//    @Tool(description = "Count using Edge Search Query Filter. Use this when you need to count edge instances that are related to a provided root entity " +
//            "based on relation type and edge types. For example, to count specific types of edge instances related to a specific asset." + ENTITY_COUNT_QUERY_DESCRIPTION)
//    public String countEdgeSearch(
//            @ToolParam(description = "A JSON value representing the entity count query with 'edgeSearchQuery' filter type.")
//            String strQuery) {
//        EntityCountQuery query = JacksonUtil.fromString(strQuery, EntityCountQuery.class);
//        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
//    }
//
//    // Entity Filter Type Specific Tools
//    @Tool(description = "Query using Single Entity Filter. Use this when you need to filter only one entity based on its ID. " +
//            "For example, to select a specific device by its ID." + ENTITY_DATA_QUERY_DESCRIPTION)
//    public String querySingleEntity(
//            @ToolParam(description = "A JSON value representing the entity query with 'singleEntity' filter type.")
//            String strQuery) {
//        EntityDataQuery query = JacksonUtil.fromString(strQuery, EntityDataQuery.class);
//        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
//    }
//
//    @Tool(description = "Query using Entity Group Filter. Use this when you need to filter multiple entities of the same type " +
//            "using the entity group type and ID. For example, to select all devices that belong to a specific group." + ENTITY_DATA_QUERY_DESCRIPTION)
//    public String queryEntityGroup(
//            @ToolParam(description = "A JSON value representing the entity query with 'entityGroup' filter type.")
//            String strQuery) {
//        EntityDataQuery query = JacksonUtil.fromString(strQuery, EntityDataQuery.class);
//        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
//    }
//
//    @Tool(description = "Query using Entity List Filter. Use this when you need to filter entities of the same type using their IDs. " +
//            "For example, to select specific devices by their IDs." + ENTITY_DATA_QUERY_DESCRIPTION)
//    public String queryEntityList(
//            @ToolParam(description = "A JSON value representing the entity query with 'entityList' filter type.")
//            String strQuery) {
//        EntityDataQuery query = JacksonUtil.fromString(strQuery, EntityDataQuery.class);
//        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
//    }
//
//    @Tool(description = "Query using Entity Name Filter. Use this when you need to filter entities of the same type using a 'starts with' " +
//            "expression over their name. For example, to select all devices which name starts with a specific prefix." + ENTITY_DATA_QUERY_DESCRIPTION)
//    public String queryEntityName(
//            @ToolParam(description = "A JSON value representing the entity query with 'entityName' filter type.")
//            String strQuery) {
//        EntityDataQuery query = JacksonUtil.fromString(strQuery, EntityDataQuery.class);
//        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
//    }
//
//    @Tool(description = "Query using Entity Type Filter. Use this when you need to filter entities based on their type " +
//            "(CUSTOMER, USER, DASHBOARD, ASSET, DEVICE, etc). For example, to select all tenant customers." + ENTITY_DATA_QUERY_DESCRIPTION)
//    public String queryEntityType(
//            @ToolParam(description = "A JSON value representing the entity query with 'entityType' filter type.")
//            String strQuery) {
//        EntityDataQuery query = JacksonUtil.fromString(strQuery, EntityDataQuery.class);
//        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
//    }
//
//    @Tool(description = "Query using Entity Group List Filter. Use this when you need to return multiple groups of the same type " +
//            "using specified IDs. For example, to select specific device groups by their IDs." + ENTITY_DATA_QUERY_DESCRIPTION)
//    public String queryEntityGroupList(
//            @ToolParam(description = "A JSON value representing the entity query with 'entityGroupList' filter type.")
//            String strQuery) {
//        EntityDataQuery query = JacksonUtil.fromString(strQuery, EntityDataQuery.class);
//        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
//    }
//
//    @Tool(description = "Query using Entity Group Name Filter. Use this when you need to filter entity groups based on their type " +
//            "and a 'starts with' expression over their name. For example, to select all device groups which name starts with a specific prefix." + ENTITY_DATA_QUERY_DESCRIPTION)
//    public String queryEntityGroupName(
//            @ToolParam(description = "A JSON value representing the entity query with 'entityGroupName' filter type.")
//            String strQuery) {
//        EntityDataQuery query = JacksonUtil.fromString(strQuery, EntityDataQuery.class);
//        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
//    }
//
//    @Tool(description = "Query using Entities by Group Name Filter. Use this when you need to filter entities that belong to a group " +
//            "based on the entity type and the group name. For example, to select all devices which belong to a group with a specific name." + ENTITY_DATA_QUERY_DESCRIPTION)
//    public String queryEntitiesByGroupName(
//            @ToolParam(description = "A JSON value representing the entity query with 'entitiesByGroupName' filter type.")
//            String strQuery) {
//        EntityDataQuery query = JacksonUtil.fromString(strQuery, EntityDataQuery.class);
//        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
//    }
//
//    @Tool(description = "Query using Entity Owner Filter. Use this when you need to fetch the owner (Tenant or Customer) " +
//            "of a specified entity. For example, to select the owner of a specific device.")
//    public String queryEntityOwner(
//            @ToolParam(description = "A JSON value representing the entity query with 'stateEntityOwner' filter type.")
//            String strQuery) {
//        EntityDataQuery query = JacksonUtil.fromString(strQuery, EntityDataQuery.class);
//        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
//    }
//
//    @Tool(description = "Query using Asset Type Filter. Use this when you need to filter assets based on their type " +
//            "and a 'starts with' expression over their name. For example, to select all assets of a specific type which name starts with a prefix.")
//    public String queryAssetType(
//            @ToolParam(description = "A JSON value representing the entity query with 'assetType' filter type.")
//            String strQuery) {
//        EntityDataQuery query = JacksonUtil.fromString(strQuery, EntityDataQuery.class);
//        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
//    }
//
//    @Tool(description = "Query using Device Type Filter. Use this when you need to filter devices based on their type " +
//            "and a 'starts with' expression over their name. For example, to select all devices of a specific type which name starts with a prefix.")
//    public String queryDeviceType(
//            @ToolParam(description = "A JSON value representing the entity query with 'deviceType' filter type.")
//            String strQuery) {
//        EntityDataQuery query = JacksonUtil.fromString(strQuery, EntityDataQuery.class);
//        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
//    }
//
//    @Tool(description = "Query using Edge Type Filter. Use this when you need to filter edge instances based on their type " +
//            "and a 'starts with' expression over their name. For example, to select all edge instances of a specific type which name starts with a prefix.")
//    public String queryEdgeType(
//            @ToolParam(description = "A JSON value representing the entity query with 'edgeType' filter type.")
//            String strQuery) {
//        EntityDataQuery query = JacksonUtil.fromString(strQuery, EntityDataQuery.class);
//        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
//    }
//
//    @Tool(description = "Query using Entity View Type Filter. Use this when you need to filter entity views based on their type " +
//            "and a 'starts with' expression over their name. For example, to select all entity views of a specific type which name starts with a prefix.")
//    public String queryEntityViewType(
//            @ToolParam(description = "A JSON value representing the entity query with 'entityViewType' filter type.")
//            String strQuery) {
//        EntityDataQuery query = JacksonUtil.fromString(strQuery, EntityDataQuery.class);
//        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
//    }
//
//    @Tool(description = "Query using API Usage Filter. Use this when you need to query for API Usage based on optional customer ID. " +
//            "If the customer ID is not set, returns current tenant API usage.")
//    public String queryApiUsage(
//            @ToolParam(description = "A JSON value representing the entity query with 'apiUsageState' filter type.")
//            String strQuery) {
//        EntityDataQuery query = JacksonUtil.fromString(strQuery, EntityDataQuery.class);
//        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
//    }
//
//    @Tool(description = "Query using Relations Query Filter. Use this when you need to filter entities that are related to a provided root entity. " +
//            "For example, to select all devices and assets which are related to a specific asset.")
//    public String queryRelations(
//            @ToolParam(description = "A JSON value representing the entity query with 'relationsQuery' filter type.")
//            String strQuery) {
//        EntityDataQuery query = JacksonUtil.fromString(strQuery, EntityDataQuery.class);
//        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
//    }
//
//    @Tool(description = "Query using Asset Search Query Filter. Use this when you need to filter assets that are related to a provided root entity " +
//            "based on relation type and asset types. For example, to select specific types of assets related to a specific asset.")
//    public String queryAssetSearch(
//            @ToolParam(description = "A JSON value representing the entity query with 'assetSearchQuery' filter type.")
//            String strQuery) {
//        EntityDataQuery query = JacksonUtil.fromString(strQuery, EntityDataQuery.class);
//        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
//    }
//
//    @Tool(description = "Query using Device Search Query Filter. Use this when you need to filter devices that are related to a provided root entity " +
//            "based on relation type and device types. For example, to select specific types of devices related to a specific asset.")
//    public String queryDeviceSearch(
//            @ToolParam(description = "A JSON value representing the entity query with 'deviceSearchQuery' filter type.")
//            String strQuery) {
//        EntityDataQuery query = JacksonUtil.fromString(strQuery, EntityDataQuery.class);
//        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
//    }
//
//    @Tool(description = "Query using Entity View Search Query Filter. Use this when you need to filter entity views that are related to a provided root entity " +
//            "based on relation type and entity view types. For example, to select specific types of entity views related to a specific asset.")
//    public String queryEntityViewSearch(
//            @ToolParam(description = "A JSON value representing the entity query with 'entityViewSearchQuery' filter type.")
//            String strQuery) {
//        EntityDataQuery query = JacksonUtil.fromString(strQuery, EntityDataQuery.class);
//        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
//    }
//
//    @Tool(description = "Query using Edge Search Query Filter. Use this when you need to filter edge instances that are related to a provided root entity " +
//            "based on relation type and edge types. For example, to select specific types of edge instances related to a specific asset.")
//    public String queryEdgeSearch(
//            @ToolParam(description = "A JSON value representing the entity query with 'edgeSearchQuery' filter type.")
//            String strQuery) {
//        EntityDataQuery query = JacksonUtil.fromString(strQuery, EntityDataQuery.class);
//        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
//    }

}
