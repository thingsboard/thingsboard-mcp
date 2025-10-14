package org.thingsboard.ai.mcp.server.tools.query;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.annotation.PeOnly;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.McpTools;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.ApiUsageStateFilter;
import org.thingsboard.server.common.data.query.AssetSearchQueryFilter;
import org.thingsboard.server.common.data.query.AssetTypeFilter;
import org.thingsboard.server.common.data.query.DeviceSearchQueryFilter;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.query.EdgeSearchQueryFilter;
import org.thingsboard.server.common.data.query.EdgeTypeFilter;
import org.thingsboard.server.common.data.query.EntitiesByGroupNameFilter;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityGroupFilter;
import org.thingsboard.server.common.data.query.EntityGroupListFilter;
import org.thingsboard.server.common.data.query.EntityGroupNameFilter;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityListFilter;
import org.thingsboard.server.common.data.query.EntityNameFilter;
import org.thingsboard.server.common.data.query.EntityTypeFilter;
import org.thingsboard.server.common.data.query.EntityViewSearchQueryFilter;
import org.thingsboard.server.common.data.query.EntityViewTypeFilter;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.RelationsQueryFilter;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.query.StateEntityOwnerFilter;

import javax.validation.Valid;
import java.util.List;

import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ALARM_DATA_QUERY_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.API_USAGE;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ASSET_QUERY_FILTER;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ASSET_TYPE;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.DEVICE_QUERY_FILTER;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.DEVICE_TYPE;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.EDGE_QUERY_FILTER;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.EDGE_TYPE;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITIES_BY_GROUP_NAME_FILTER;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_GROUP_FILTER;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_GROUP_LIST_FILTER;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_GROUP_NAME_FILTER;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_LIST;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_NAME;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_OWNER_FILTER;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_TYPE_FILTER;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_VIEW_TYPE;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.EV_QUERY_FILTER;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.FILTER_KEY;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.KEY_FILTERS;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PE_ONLY_AVAILABLE;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.RELATIONS_QUERY_FILTER;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.SINGLE_ENTITY;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.ai.mcp.server.util.ToolUtils.createPageLink;

@Service
@RequiredArgsConstructor
public class EntityQueryTools implements McpTools {

    private final RestClientService clientService;

    // Entity Data Query:
    @Tool(description = "Find Entity Data for single entity by its id. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataBySingleEntityFilter(
            @ToolParam(description = SINGLE_ENTITY) @Valid @NotNull SingleEntityFilter singleEntityFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(singleEntityFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @PeOnly
    @Tool(description = "Find Entity Data for entities of the same type using the entity group type and id. " +
            "Supports fetching entity fields (e.g., name, type), and latest values from attributes or timeseries (telemetry) with ability to define (or not)" +
            "operations complex logical expressions over entity field, attribute or latest time series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide(). " + PE_ONLY_AVAILABLE)
    public String findEntityDataByEntityGroupFilter(
            @ToolParam(description = ENTITY_GROUP_FILTER) @Valid @NotNull EntityGroupFilter entityGroupFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(entityGroupFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Find Entity Data for entities of the same type using their ids. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByEntityListFilter(
            @ToolParam(description = ENTITY_LIST) @Valid @NotNull EntityListFilter entityListFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(entityListFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Find Entity Data for entities of the same type using the **'starts with'** expression over entity name. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByEntityNameFilter(
            @ToolParam(description = ENTITY_NAME) @Valid @NotNull EntityNameFilter entityNameFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(entityNameFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Find Entity Data for entities based on their type (CUSTOMER, USER, DASHBOARD, ASSET, DEVICE, etc). " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByEntityTypeFilter(
            @ToolParam(description = ENTITY_TYPE_FILTER) @Valid @NotNull EntityTypeFilter entityTypeFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(entityTypeFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @PeOnly
    @Tool(description = "Find Entity Data for multiple groups of the same type using specified ids. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide(). " + PE_ONLY_AVAILABLE)
    public String findEntityDataByEntityGroupListFilter(
            @ToolParam(description = ENTITY_GROUP_LIST_FILTER) @Valid @NotNull EntityGroupListFilter entityGroupListFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(entityGroupListFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @PeOnly
    @Tool(description = "Find Entity Data for entity groups based on their type and the **'starts with'** expression over their name. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide()." + PE_ONLY_AVAILABLE)
    public String findEntityDataByEntityGroupNameFilter(
            @ToolParam(description = ENTITY_GROUP_NAME_FILTER) @Valid @NotNull EntityGroupNameFilter entityGroupNameFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(entityGroupNameFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @PeOnly
    @Tool(description = "Find Entity Data for entities that belong to group based on the entity type and the group name. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide(). " + PE_ONLY_AVAILABLE)
    public String findEntityDataByEntitiesGroupNameFilter(
            @ToolParam(description = ENTITIES_BY_GROUP_NAME_FILTER) @Valid @NotNull EntitiesByGroupNameFilter entitiesByGroupNameFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(entitiesByGroupNameFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Find Entity Data for owner (Tenant or Customer) of the specified entity. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByStateEntityOwnerFilter(
            @ToolParam(description = ENTITY_OWNER_FILTER) @Valid @NotNull StateEntityOwnerFilter stateEntityOwnerFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(stateEntityOwnerFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Find Entity Data for assets based on their type and the **'starts with'** expression over their name. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByAssetTypeFilter(
            @ToolParam(description = ASSET_TYPE) @Valid @NotNull AssetTypeFilter assetTypeFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(assetTypeFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Find Entity Data for devices based on their type and the **'starts with'** expression over their name. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByDeviceTypeFilter(
            @ToolParam(description = DEVICE_TYPE) @Valid @NotNull DeviceTypeFilter deviceTypeFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(deviceTypeFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Find Entity Data for edges based on their type and the **'starts with'** expression over their name. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByEdgeTypeFilter(
            @ToolParam(description = EDGE_TYPE) @Valid @NotNull EdgeTypeFilter edgeTypeFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(edgeTypeFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Find Entity Data for entity views based on their type and the **'starts with'** expression over their name. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByEntityViewTypeFilter(
            @ToolParam(description = ENTITY_VIEW_TYPE) @Valid @NotNull EntityViewTypeFilter entityViewTypeFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(entityViewTypeFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Find Entity Data for api usage based on optional customer id. If the customer id is not set, returns current tenant API usage. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByApiUsageStateFilter(
            @ToolParam(description = API_USAGE) @Valid @NotNull ApiUsageStateFilter apiUsageStateFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(apiUsageStateFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Find Entity Data for entities that are related to the provided root entity. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByRelationsQueryFilter(
            @ToolParam(description = RELATIONS_QUERY_FILTER) @Valid @NotNull RelationsQueryFilter relationsQueryFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(relationsQueryFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Find Entity Data for assets that are related to the provided root entity. Filters related assets based on the relation type and set of asset types. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByAssetSearchQueryFilter(
            @ToolParam(description = ASSET_QUERY_FILTER) @Valid @NotNull AssetSearchQueryFilter assetSearchQueryFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(assetSearchQueryFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Find Entity Data for devices that are related to the provided root entity.  Filters related devices based on the relation type and set of devices types. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByDeviceSearchQueryFilter(
            @ToolParam(description = DEVICE_QUERY_FILTER) @Valid @NotNull DeviceSearchQueryFilter deviceSearchQueryFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(deviceSearchQueryFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Find Entity Data for entity views that are related to the provided root entity. Filters related entity views based on the relation type and set of entity view types. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByEntityViewSearchQueryFilter(
            @ToolParam(description = EV_QUERY_FILTER) @Valid @NotNull EntityViewSearchQueryFilter entityViewSearchQueryFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(entityViewSearchQueryFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Find Entity Data for edges that are related to the provided root entity. Filters related edges based on the relation type and set of edge types. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByEdgeQueryFilter(
            @ToolParam(description = EDGE_QUERY_FILTER) @Valid @NotNull EdgeSearchQueryFilter edgeSearchQueryFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(edgeSearchQueryFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    // Entity Count Query:
    @Tool(description = "Find Entity Data count of single entity by its id. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String countBySingleEntityFilter(
            @ToolParam(description = SINGLE_ENTITY) @Valid @NotNull SingleEntityFilter singleEntityFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters) {
        EntityCountQuery query = new EntityCountQuery(singleEntityFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @PeOnly
    @Tool(description = "Find Entity Data count of entities of the same type using the entity group type and id. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide(). " + PE_ONLY_AVAILABLE)
    public String countByEntityGroupFilter(
            @ToolParam(description = ENTITY_GROUP_FILTER) @Valid @NotNull EntityGroupFilter entityGroupFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters) {
        EntityCountQuery query = new EntityCountQuery(entityGroupFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Find Entity Data count of entities of the same type using their ids. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String countByEntityListFilter(
            @ToolParam(description = ENTITY_LIST) @Valid @NotNull EntityListFilter entityListFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters) {
        EntityCountQuery query = new EntityCountQuery(entityListFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Find Entity Data count of entities of the same type using the **'starts with'** expression over entity name. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String countByEntityNameFilter(
            @ToolParam(description = ENTITY_NAME) @Valid @NotNull EntityNameFilter entityNameFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters) {
        EntityCountQuery query = new EntityCountQuery(entityNameFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Find Entity Data count of entities based on their type (CUSTOMER, USER, DASHBOARD, ASSET, DEVICE, etc). " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String countByEntityTypeFilter(
            @ToolParam(description = ENTITY_TYPE_FILTER) @Valid @NotNull EntityTypeFilter entityTypeFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters) {
        EntityCountQuery query = new EntityCountQuery(entityTypeFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @PeOnly
    @Tool(description = "Find Entity Data count of multiple groups of the same type using specified ids. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String countByEntityGroupListFilter(
            @ToolParam(description = ENTITY_GROUP_LIST_FILTER) @Valid @NotNull EntityGroupListFilter entityGroupListFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters) {
        EntityCountQuery query = new EntityCountQuery(entityGroupListFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @PeOnly
    @Tool(description = "Find Entity Data count of entity groups based on their type and the **'starts with'** expression over their name. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide(). " + PE_ONLY_AVAILABLE)
    public String countByEntityGroupNameFilter(
            @ToolParam(description = ENTITY_GROUP_NAME_FILTER) @Valid @NotNull EntityGroupNameFilter entityGroupNameFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters) {
        EntityCountQuery query = new EntityCountQuery(entityGroupNameFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @PeOnly
    @Tool(description = "Find Entity Data count of entities that belong to group based on the entity type and the group name. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide(). " + PE_ONLY_AVAILABLE)
    public String countByEntitiesGroupNameFilter(
            @ToolParam(description = ENTITIES_BY_GROUP_NAME_FILTER) @Valid @NotNull EntitiesByGroupNameFilter entitiesByGroupNameFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters) {
        EntityCountQuery query = new EntityCountQuery(entitiesByGroupNameFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Find Entity Data count of owner (Tenant or Customer) of the specified entity. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String countByStateEntityOwnerFilter(
            @ToolParam(description = ENTITY_OWNER_FILTER) @Valid @NotNull StateEntityOwnerFilter stateEntityOwnerFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters) {
        EntityCountQuery query = new EntityCountQuery(stateEntityOwnerFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Find Entity Data count of assets based on their type and the **'starts with'** expression over their name. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String countByAssetTypeFilter(
            @ToolParam(description = ASSET_TYPE) @Valid @NotNull AssetTypeFilter assetTypeFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters) {
        EntityCountQuery query = new EntityCountQuery(assetTypeFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Find Entity Data count of devices based on their type and the **'starts with'** expression over their name. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String countByDeviceTypeFilter(
            @ToolParam(description = DEVICE_TYPE) @Valid @NotNull DeviceTypeFilter deviceTypeFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters) {
        EntityCountQuery query = new EntityCountQuery(deviceTypeFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Find Entity Data count of edges based on their type and the **'starts with'** expression over their name. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String countByEdgeTypeFilter(
            @ToolParam(description = EDGE_TYPE) @Valid @NotNull EdgeTypeFilter edgeTypeFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters) {
        EntityCountQuery query = new EntityCountQuery(edgeTypeFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Find Entity Data count of entity views based on their type and the **'starts with'** expression over their name. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String countByEntityViewTypeFilter(
            @ToolParam(description = ENTITY_VIEW_TYPE) @Valid @NotNull EntityViewTypeFilter entityViewTypeFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters) {
        EntityCountQuery query = new EntityCountQuery(entityViewTypeFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Find Entity Data count of api usage based on optional customer id. If the customer id is not set, returns current tenant API usage. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String countByApiUsageStateFilter(
            @ToolParam(description = API_USAGE) @Valid @NotNull ApiUsageStateFilter apiUsageStateFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters) {
        EntityCountQuery query = new EntityCountQuery(apiUsageStateFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Find Entity Data count of entities that are related to the provided root entity. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String countByRelationsQueryFilter(
            @ToolParam(description = RELATIONS_QUERY_FILTER) @Valid @NotNull RelationsQueryFilter relationsQueryFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters) {
        EntityCountQuery query = new EntityCountQuery(relationsQueryFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Find Entity Data count of assets that are related to the provided root entity. Filters related assets based on the relation type and set of asset types. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String countByAssetSearchQueryFilter(
            @ToolParam(description = ASSET_QUERY_FILTER) @Valid @NotNull AssetSearchQueryFilter assetSearchQueryFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters) {
        EntityCountQuery query = new EntityCountQuery(assetSearchQueryFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Find Entity Data count of devices that are related to the provided root entity. Filters related devices based on the relation type and set of devices types. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String countByDeviceSearchQueryFilter(
            @ToolParam(description = DEVICE_QUERY_FILTER) @Valid @NotNull DeviceSearchQueryFilter deviceSearchQueryFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters) {
        EntityCountQuery query = new EntityCountQuery(deviceSearchQueryFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Find Entity Data count of entity views that are related to the provided root entity. Filters related entity views based on the relation type and set of entity views types. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String countByEntityViewSearchQueryFilter(
            @ToolParam(description = EV_QUERY_FILTER) @Valid @NotNull EntityViewSearchQueryFilter entityViewSearchQueryFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters) {
        EntityCountQuery query = new EntityCountQuery(entityViewSearchQueryFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Find Entity Data count of edges that are related to the provided root entity. Filters related edges based on the relation type and set of edges types. " +
            "Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. " +
            "Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. " +
            "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String countByEdgeQueryFilter(
            @ToolParam(description = EDGE_QUERY_FILTER) @Valid @NotNull EdgeSearchQueryFilter edgeSearchQueryFilter,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilter> keyFilters) {
        EntityCountQuery query = new EntityCountQuery(edgeSearchQueryFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = ALARM_DATA_QUERY_DESCRIPTION + "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findAlarmDataByQuery(
            @ToolParam(description = "A JSON value representing the alarm data query.") AlarmDataQuery alarmDataQuery) {
        return JacksonUtil.toString(clientService.getClient().findAlarmDataByQuery(alarmDataQuery));
    }

    @Tool(description = "Count Alarms by Query. Allows to run complex queries to count the alarms based on the combination of alarm fields filter and multiple key filters. " +
            "Returns the number of alarms that match the query definition." +
            TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String countAlarmsByQuery(
            @ToolParam(description = "A JSON value representing the alarm count query.") AlarmCountQuery alarmCountQuery) {
        return JacksonUtil.toString(clientService.getClient().countAlarmsByQuery(alarmCountQuery));
    }

}
