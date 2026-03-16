package org.thingsboard.ai.mcp.server.tools.query;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.annotation.PeOnly;
import org.thingsboard.ai.mcp.server.annotation.ToolGroup;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.McpTools;
import org.thingsboard.ai.mcp.server.util.JacksonUtil;
import org.thingsboard.client.model.ApiUsageStateFilter;
import org.thingsboard.client.model.AssetSearchQueryFilter;
import org.thingsboard.client.model.AssetTypeFilter;
import org.thingsboard.client.model.DeviceSearchQueryFilter;
import org.thingsboard.client.model.DeviceTypeFilter;
import org.thingsboard.client.model.EdgeSearchQueryFilter;
import org.thingsboard.client.model.EdgeTypeFilter;
import org.thingsboard.client.model.EntitiesByGroupNameFilter;
import org.thingsboard.client.model.EntityCountQuery;
import org.thingsboard.client.model.EntityDataPageLink;
import org.thingsboard.client.model.EntityDataQuery;
import org.thingsboard.client.model.EntityGroupFilter;
import org.thingsboard.client.model.EntityGroupListFilter;
import org.thingsboard.client.model.EntityGroupNameFilter;
import org.thingsboard.client.model.EntityKey;
import org.thingsboard.client.model.EntityListFilter;
import org.thingsboard.client.model.EntityNameFilter;
import org.thingsboard.client.model.EntityTypeFilter;
import org.thingsboard.client.model.EntityViewSearchQueryFilter;
import org.thingsboard.client.model.EntityViewTypeFilter;
import org.thingsboard.client.model.KeyFilter;
import org.thingsboard.client.model.RelationsQueryFilter;
import org.thingsboard.client.model.SingleEntityFilter;
import org.thingsboard.client.model.StateEntityOwnerFilter;

import java.util.List;

import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.API_USAGE;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ASSET_QUERY_FILTER;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ASSET_TYPE;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.DEVICE_QUERY_FILTER;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.DEVICE_TYPE;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.EDGE_QUERY_FILTER;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.EDGE_TYPE;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITIES_BY_GROUP_NAME_FILTER;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_FIELDS_JSON;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_GROUP_FILTER;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_GROUP_LIST_FILTER;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_GROUP_NAME_FILTER;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_LIST;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_NAME;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_OWNER_FILTER;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_TYPE_FILTER;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_VIEW_TYPE;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.EV_QUERY_FILTER;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.KEY_FILTERS_JSON;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.LATEST_VALUES_JSON;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.RELATIONS_QUERY_FILTER;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.SINGLE_ENTITY;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.util.ToolUtils.createPageLink;
import static org.thingsboard.ai.mcp.server.util.ToolUtils.parseEntityKeys;
import static org.thingsboard.ai.mcp.server.util.ToolUtils.parseKeyFilters;

@Service
@RequiredArgsConstructor
@ToolGroup("edq")
public class EntityQueryTools implements McpTools {

    // Routing hint: tells the LLM WHEN to pick EDQ over simple list+telemetry tools
    private static final String EDQ_ROUTING = "Prefer over simple list/get tools when you need to filter entities by attribute/telemetry values (e.g., temperature > 50) or fetch fields + attributes + telemetry in one query. ";
    private static final String EDQ_GUIDES = " Call getEdqGuide() and getKeyFiltersGuide() first.";
    private static final String COUNT_GUIDES = " Call getEdqCountGuide() and getKeyFiltersGuide() first.";

    private final RestClientService clientService;

    // Entity Data Query:
    @Tool(description = EDQ_ROUTING + "Use this to find entity data for a single entity by id." + EDQ_GUIDES)
    public String findEntityDataBySingleEntityFilter(
            @ToolParam(description = SINGLE_ENTITY) @NotBlank String singleEntityFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson,
            @ToolParam(required = false, description = ENTITY_FIELDS_JSON) String entityFieldsJson,
            @ToolParam(required = false, description = LATEST_VALUES_JSON) String latestValuesJson,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        SingleEntityFilter singleEntityFilter = JacksonUtil.fromString(singleEntityFilterJson, SingleEntityFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        List<EntityKey> entityFields = parseEntityKeys(entityFieldsJson);
        List<EntityKey> latestValues = parseEntityKeys(latestValuesJson);
        EntityDataQuery query = new EntityDataQuery()
                .entityFilter(singleEntityFilter)
                .pageLink(pageLink)
                .entityFields(entityFields)
                .latestValues(latestValues)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @PeOnly
    @Tool(description = EDQ_ROUTING + "Use this to find entity data for entities in a specific group. PE only." + EDQ_GUIDES)
    public String findEntityDataByEntityGroupFilter(
            @ToolParam(description = ENTITY_GROUP_FILTER) @NotBlank String entityGroupFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson,
            @ToolParam(required = false, description = ENTITY_FIELDS_JSON) String entityFieldsJson,
            @ToolParam(required = false, description = LATEST_VALUES_JSON) String latestValuesJson,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        EntityGroupFilter entityGroupFilter = JacksonUtil.fromString(entityGroupFilterJson, EntityGroupFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        List<EntityKey> entityFields = parseEntityKeys(entityFieldsJson);
        List<EntityKey> latestValues = parseEntityKeys(latestValuesJson);
        EntityDataQuery query = new EntityDataQuery()
                .entityFilter(entityGroupFilter)
                .pageLink(pageLink)
                .entityFields(entityFields)
                .latestValues(latestValues)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = EDQ_ROUTING + "Use this to find entity data for multiple entities by their ids." + EDQ_GUIDES)
    public String findEntityDataByEntityListFilter(
            @ToolParam(description = ENTITY_LIST) @NotBlank String entityListFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson,
            @ToolParam(required = false, description = ENTITY_FIELDS_JSON) String entityFieldsJson,
            @ToolParam(required = false, description = LATEST_VALUES_JSON) String latestValuesJson,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        EntityListFilter entityListFilter = JacksonUtil.fromString(entityListFilterJson, EntityListFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        List<EntityKey> entityFields = parseEntityKeys(entityFieldsJson);
        List<EntityKey> latestValues = parseEntityKeys(latestValuesJson);
        EntityDataQuery query = new EntityDataQuery()
                .entityFilter(entityListFilter)
                .pageLink(pageLink)
                .entityFields(entityFields)
                .latestValues(latestValues)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = EDQ_ROUTING + "Use this to find entity data by name prefix ('starts with'). For DEVICE/ASSET/EDGE/ENTITY_VIEW with known profile, use the corresponding type filter instead." + EDQ_GUIDES)
    public String findEntityDataByEntityNameFilter(
            @ToolParam(description = ENTITY_NAME) @NotBlank String entityNameFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson,
            @ToolParam(required = false, description = ENTITY_FIELDS_JSON) String entityFieldsJson,
            @ToolParam(required = false, description = LATEST_VALUES_JSON) String latestValuesJson,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        EntityNameFilter entityNameFilter = JacksonUtil.fromString(entityNameFilterJson, EntityNameFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        List<EntityKey> entityFields = parseEntityKeys(entityFieldsJson);
        List<EntityKey> latestValues = parseEntityKeys(latestValuesJson);
        EntityDataQuery query = new EntityDataQuery()
                .entityFilter(entityNameFilter)
                .pageLink(pageLink)
                .entityFields(entityFields)
                .latestValues(latestValues)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = EDQ_ROUTING + "Use this to find entity data for all entities of a type (DEVICE, ASSET, CUSTOMER, USER, DASHBOARD, ENTITY_VIEW, EDGE, TENANT)." + EDQ_GUIDES)
    public String findEntityDataByEntityTypeFilter(
            @ToolParam(description = ENTITY_TYPE_FILTER) @NotBlank String entityTypeFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson,
            @ToolParam(required = false, description = ENTITY_FIELDS_JSON) String entityFieldsJson,
            @ToolParam(required = false, description = LATEST_VALUES_JSON) String latestValuesJson,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        EntityTypeFilter entityTypeFilter = JacksonUtil.fromString(entityTypeFilterJson, EntityTypeFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        List<EntityKey> entityFields = parseEntityKeys(entityFieldsJson);
        List<EntityKey> latestValues = parseEntityKeys(latestValuesJson);
        EntityDataQuery query = new EntityDataQuery()
                .entityFilter(entityTypeFilter)
                .pageLink(pageLink)
                .entityFields(entityFields)
                .latestValues(latestValues)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @PeOnly
    @Tool(description = EDQ_ROUTING + "Use this to find entity data for entities across multiple groups by group ids. PE only." + EDQ_GUIDES)
    public String findEntityDataByEntityGroupListFilter(
            @ToolParam(description = ENTITY_GROUP_LIST_FILTER) @NotBlank String entityGroupListFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson,
            @ToolParam(required = false, description = ENTITY_FIELDS_JSON) String entityFieldsJson,
            @ToolParam(required = false, description = LATEST_VALUES_JSON) String latestValuesJson,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        EntityGroupListFilter entityGroupListFilter = JacksonUtil.fromString(entityGroupListFilterJson, EntityGroupListFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        List<EntityKey> entityFields = parseEntityKeys(entityFieldsJson);
        List<EntityKey> latestValues = parseEntityKeys(latestValuesJson);
        EntityDataQuery query = new EntityDataQuery()
                .entityFilter(entityGroupListFilter)
                .pageLink(pageLink)
                .entityFields(entityFields)
                .latestValues(latestValues)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @PeOnly
    @Tool(description = EDQ_ROUTING + "Use this to find entity data for groups matching a name prefix. PE only." + EDQ_GUIDES)
    public String findEntityDataByEntityGroupNameFilter(
            @ToolParam(description = ENTITY_GROUP_NAME_FILTER) @NotBlank String entityGroupNameFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson,
            @ToolParam(required = false, description = ENTITY_FIELDS_JSON) String entityFieldsJson,
            @ToolParam(required = false, description = LATEST_VALUES_JSON) String latestValuesJson,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        EntityGroupNameFilter entityGroupNameFilter = JacksonUtil.fromString(entityGroupNameFilterJson, EntityGroupNameFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        List<EntityKey> entityFields = parseEntityKeys(entityFieldsJson);
        List<EntityKey> latestValues = parseEntityKeys(latestValuesJson);
        EntityDataQuery query = new EntityDataQuery()
                .entityFilter(entityGroupNameFilter)
                .pageLink(pageLink)
                .entityFields(entityFields)
                .latestValues(latestValues)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @PeOnly
    @Tool(description = EDQ_ROUTING + "Use this to find entity data for entities in a group by group name. PE only." + EDQ_GUIDES)
    public String findEntityDataByEntitiesGroupNameFilter(
            @ToolParam(description = ENTITIES_BY_GROUP_NAME_FILTER) @NotBlank String entitiesByGroupNameFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson,
            @ToolParam(required = false, description = ENTITY_FIELDS_JSON) String entityFieldsJson,
            @ToolParam(required = false, description = LATEST_VALUES_JSON) String latestValuesJson,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        EntitiesByGroupNameFilter entitiesByGroupNameFilter = JacksonUtil.fromString(entitiesByGroupNameFilterJson, EntitiesByGroupNameFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        List<EntityKey> entityFields = parseEntityKeys(entityFieldsJson);
        List<EntityKey> latestValues = parseEntityKeys(latestValuesJson);
        EntityDataQuery query = new EntityDataQuery()
                .entityFilter(entitiesByGroupNameFilter)
                .pageLink(pageLink)
                .entityFields(entityFields)
                .latestValues(latestValues)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = EDQ_ROUTING + "Use this to find entity data for the owner (Tenant or Customer) of a specified entity." + EDQ_GUIDES)
    public String findEntityDataByStateEntityOwnerFilter(
            @ToolParam(description = ENTITY_OWNER_FILTER) @NotBlank String stateEntityOwnerFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson,
            @ToolParam(required = false, description = ENTITY_FIELDS_JSON) String entityFieldsJson,
            @ToolParam(required = false, description = LATEST_VALUES_JSON) String latestValuesJson,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        StateEntityOwnerFilter stateEntityOwnerFilter = JacksonUtil.fromString(stateEntityOwnerFilterJson, StateEntityOwnerFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        List<EntityKey> entityFields = parseEntityKeys(entityFieldsJson);
        List<EntityKey> latestValues = parseEntityKeys(latestValuesJson);
        EntityDataQuery query = new EntityDataQuery()
                .entityFilter(stateEntityOwnerFilter)
                .pageLink(pageLink)
                .entityFields(entityFields)
                .latestValues(latestValues)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = EDQ_ROUTING + "Use this to find entity data for assets by profile/type and optional name prefix." + EDQ_GUIDES)
    public String findEntityDataByAssetTypeFilter(
            @ToolParam(description = ASSET_TYPE) @NotBlank String assetTypeFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson,
            @ToolParam(required = false, description = ENTITY_FIELDS_JSON) String entityFieldsJson,
            @ToolParam(required = false, description = LATEST_VALUES_JSON) String latestValuesJson,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        AssetTypeFilter assetTypeFilter = JacksonUtil.fromString(assetTypeFilterJson, AssetTypeFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        List<EntityKey> entityFields = parseEntityKeys(entityFieldsJson);
        List<EntityKey> latestValues = parseEntityKeys(latestValuesJson);
        EntityDataQuery query = new EntityDataQuery()
                .entityFilter(assetTypeFilter)
                .pageLink(pageLink)
                .entityFields(entityFields)
                .latestValues(latestValues)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = EDQ_ROUTING + "Use this to find entity data for devices by profile/type and optional name prefix." + EDQ_GUIDES)
    public String findEntityDataByDeviceTypeFilter(
            @ToolParam(description = DEVICE_TYPE) @NotBlank String deviceTypeFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson,
            @ToolParam(required = false, description = ENTITY_FIELDS_JSON) String entityFieldsJson,
            @ToolParam(required = false, description = LATEST_VALUES_JSON) String latestValuesJson,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        DeviceTypeFilter deviceTypeFilter = JacksonUtil.fromString(deviceTypeFilterJson, DeviceTypeFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        List<EntityKey> entityFields = parseEntityKeys(entityFieldsJson);
        List<EntityKey> latestValues = parseEntityKeys(latestValuesJson);
        EntityDataQuery query = new EntityDataQuery()
                .entityFilter(deviceTypeFilter)
                .pageLink(pageLink)
                .entityFields(entityFields)
                .latestValues(latestValues)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = EDQ_ROUTING + "Use this to find entity data for edges by type and optional name prefix." + EDQ_GUIDES)
    public String findEntityDataByEdgeTypeFilter(
            @ToolParam(description = EDGE_TYPE) @NotBlank String edgeTypeFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson,
            @ToolParam(required = false, description = ENTITY_FIELDS_JSON) String entityFieldsJson,
            @ToolParam(required = false, description = LATEST_VALUES_JSON) String latestValuesJson,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        EdgeTypeFilter edgeTypeFilter = JacksonUtil.fromString(edgeTypeFilterJson, EdgeTypeFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        List<EntityKey> entityFields = parseEntityKeys(entityFieldsJson);
        List<EntityKey> latestValues = parseEntityKeys(latestValuesJson);
        EntityDataQuery query = new EntityDataQuery()
                .entityFilter(edgeTypeFilter)
                .pageLink(pageLink)
                .entityFields(entityFields)
                .latestValues(latestValues)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = EDQ_ROUTING + "Use this to find entity data for entity views by type and optional name prefix." + EDQ_GUIDES)
    public String findEntityDataByEntityViewTypeFilter(
            @ToolParam(description = ENTITY_VIEW_TYPE) @NotBlank String entityViewTypeFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson,
            @ToolParam(required = false, description = ENTITY_FIELDS_JSON) String entityFieldsJson,
            @ToolParam(required = false, description = LATEST_VALUES_JSON) String latestValuesJson,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        EntityViewTypeFilter entityViewTypeFilter = JacksonUtil.fromString(entityViewTypeFilterJson, EntityViewTypeFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        List<EntityKey> entityFields = parseEntityKeys(entityFieldsJson);
        List<EntityKey> latestValues = parseEntityKeys(latestValuesJson);
        EntityDataQuery query = new EntityDataQuery()
                .entityFilter(entityViewTypeFilter)
                .pageLink(pageLink)
                .entityFields(entityFields)
                .latestValues(latestValues)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Use this to find API usage data. If customer id provided, returns customer's usage; otherwise tenant's." + EDQ_GUIDES)
    public String findEntityDataByApiUsageStateFilter(
            @ToolParam(description = API_USAGE) @NotBlank String apiUsageStateFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson,
            @ToolParam(required = false, description = ENTITY_FIELDS_JSON) String entityFieldsJson,
            @ToolParam(required = false, description = LATEST_VALUES_JSON) String latestValuesJson,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        ApiUsageStateFilter apiUsageStateFilter = JacksonUtil.fromString(apiUsageStateFilterJson, ApiUsageStateFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        List<EntityKey> entityFields = parseEntityKeys(entityFieldsJson);
        List<EntityKey> latestValues = parseEntityKeys(latestValuesJson);
        EntityDataQuery query = new EntityDataQuery()
                .entityFilter(apiUsageStateFilter)
                .pageLink(pageLink)
                .entityFields(entityFields)
                .latestValues(latestValues)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = EDQ_ROUTING + "Use this to find entity data for entities related to a root entity via relations." + EDQ_GUIDES)
    public String findEntityDataByRelationsQueryFilter(
            @ToolParam(description = RELATIONS_QUERY_FILTER) @NotBlank String relationsQueryFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson,
            @ToolParam(required = false, description = ENTITY_FIELDS_JSON) String entityFieldsJson,
            @ToolParam(required = false, description = LATEST_VALUES_JSON) String latestValuesJson,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        RelationsQueryFilter relationsQueryFilter = JacksonUtil.fromString(relationsQueryFilterJson, RelationsQueryFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        List<EntityKey> entityFields = parseEntityKeys(entityFieldsJson);
        List<EntityKey> latestValues = parseEntityKeys(latestValuesJson);
        EntityDataQuery query = new EntityDataQuery()
                .entityFilter(relationsQueryFilter)
                .pageLink(pageLink)
                .entityFields(entityFields)
                .latestValues(latestValues)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = EDQ_ROUTING + "Use this to find entity data for assets related to a root entity. Filters by relation type and asset types." + EDQ_GUIDES)
    public String findEntityDataByAssetSearchQueryFilter(
            @ToolParam(description = ASSET_QUERY_FILTER) @NotBlank String assetSearchQueryFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson,
            @ToolParam(required = false, description = ENTITY_FIELDS_JSON) String entityFieldsJson,
            @ToolParam(required = false, description = LATEST_VALUES_JSON) String latestValuesJson,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        AssetSearchQueryFilter assetSearchQueryFilter = JacksonUtil.fromString(assetSearchQueryFilterJson, AssetSearchQueryFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        List<EntityKey> entityFields = parseEntityKeys(entityFieldsJson);
        List<EntityKey> latestValues = parseEntityKeys(latestValuesJson);
        EntityDataQuery query = new EntityDataQuery()
                .entityFilter(assetSearchQueryFilter)
                .pageLink(pageLink)
                .entityFields(entityFields)
                .latestValues(latestValues)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = EDQ_ROUTING + "Use this to find entity data for devices related to a root entity. Filters by relation type and device types." + EDQ_GUIDES)
    public String findEntityDataByDeviceSearchQueryFilter(
            @ToolParam(description = DEVICE_QUERY_FILTER) @NotBlank String deviceSearchQueryFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson,
            @ToolParam(required = false, description = ENTITY_FIELDS_JSON) String entityFieldsJson,
            @ToolParam(required = false, description = LATEST_VALUES_JSON) String latestValuesJson,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        DeviceSearchQueryFilter deviceSearchQueryFilter = JacksonUtil.fromString(deviceSearchQueryFilterJson, DeviceSearchQueryFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        List<EntityKey> entityFields = parseEntityKeys(entityFieldsJson);
        List<EntityKey> latestValues = parseEntityKeys(latestValuesJson);
        EntityDataQuery query = new EntityDataQuery()
                .entityFilter(deviceSearchQueryFilter)
                .pageLink(pageLink)
                .entityFields(entityFields)
                .latestValues(latestValues)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = EDQ_ROUTING + "Use this to find entity data for entity views related to a root entity. Filters by relation type and view types." + EDQ_GUIDES)
    public String findEntityDataByEntityViewSearchQueryFilter(
            @ToolParam(description = EV_QUERY_FILTER) @NotBlank String entityViewSearchQueryFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson,
            @ToolParam(required = false, description = ENTITY_FIELDS_JSON) String entityFieldsJson,
            @ToolParam(required = false, description = LATEST_VALUES_JSON) String latestValuesJson,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        EntityViewSearchQueryFilter entityViewSearchQueryFilter = JacksonUtil.fromString(entityViewSearchQueryFilterJson, EntityViewSearchQueryFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        List<EntityKey> entityFields = parseEntityKeys(entityFieldsJson);
        List<EntityKey> latestValues = parseEntityKeys(latestValuesJson);
        EntityDataQuery query = new EntityDataQuery()
                .entityFilter(entityViewSearchQueryFilter)
                .pageLink(pageLink)
                .entityFields(entityFields)
                .latestValues(latestValues)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = EDQ_ROUTING + "Use this to find entity data for edges related to a root entity. Filters by relation type and edge types." + EDQ_GUIDES)
    public String findEntityDataByEdgeQueryFilter(
            @ToolParam(description = EDGE_QUERY_FILTER) @NotBlank String edgeSearchQueryFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson,
            @ToolParam(required = false, description = ENTITY_FIELDS_JSON) String entityFieldsJson,
            @ToolParam(required = false, description = LATEST_VALUES_JSON) String latestValuesJson,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        EdgeSearchQueryFilter edgeSearchQueryFilter = JacksonUtil.fromString(edgeSearchQueryFilterJson, EdgeSearchQueryFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        List<EntityKey> entityFields = parseEntityKeys(entityFieldsJson);
        List<EntityKey> latestValues = parseEntityKeys(latestValuesJson);
        EntityDataQuery query = new EntityDataQuery()
                .entityFilter(edgeSearchQueryFilter)
                .pageLink(pageLink)
                .entityFields(entityFields)
                .latestValues(latestValues)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    // Entity Count Query:
    @Tool(description = "Use this to count entities matching a single entity filter with optional keyFilters. " + COUNT_GUIDES)
    public String countBySingleEntityFilter(
            @ToolParam(description = SINGLE_ENTITY) @NotBlank String singleEntityFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson) {
        SingleEntityFilter singleEntityFilter = JacksonUtil.fromString(singleEntityFilterJson, SingleEntityFilter.class);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        EntityCountQuery query = new EntityCountQuery()
                .entityFilter(singleEntityFilter)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @PeOnly
    @Tool(description = "Use this to count entities in a specific group with optional keyFilters. PE only. " + COUNT_GUIDES)
    public String countByEntityGroupFilter(
            @ToolParam(description = ENTITY_GROUP_FILTER) @NotBlank String entityGroupFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson) {
        EntityGroupFilter entityGroupFilter = JacksonUtil.fromString(entityGroupFilterJson, EntityGroupFilter.class);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        EntityCountQuery query = new EntityCountQuery()
                .entityFilter(entityGroupFilter)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Use this to count entities by their ids with optional keyFilters. " + COUNT_GUIDES)
    public String countByEntityListFilter(
            @ToolParam(description = ENTITY_LIST) @NotBlank String entityListFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson) {
        EntityListFilter entityListFilter = JacksonUtil.fromString(entityListFilterJson, EntityListFilter.class);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        EntityCountQuery query = new EntityCountQuery()
                .entityFilter(entityListFilter)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Use this to count entities matching a name prefix with optional keyFilters. " + COUNT_GUIDES)
    public String countByEntityNameFilter(
            @ToolParam(description = ENTITY_NAME) @NotBlank String entityNameFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson) {
        EntityNameFilter entityNameFilter = JacksonUtil.fromString(entityNameFilterJson, EntityNameFilter.class);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        EntityCountQuery query = new EntityCountQuery()
                .entityFilter(entityNameFilter)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Use this to count all entities of a type (DEVICE, ASSET, CUSTOMER, etc.) with optional keyFilters. " + COUNT_GUIDES)
    public String countByEntityTypeFilter(
            @ToolParam(description = ENTITY_TYPE_FILTER) @NotBlank String entityTypeFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson) {
        EntityTypeFilter entityTypeFilter = JacksonUtil.fromString(entityTypeFilterJson, EntityTypeFilter.class);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        EntityCountQuery query = new EntityCountQuery()
                .entityFilter(entityTypeFilter)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @PeOnly
    @Tool(description = "Use this to count entities across multiple groups with optional keyFilters. PE only. " + COUNT_GUIDES)
    public String countByEntityGroupListFilter(
            @ToolParam(description = ENTITY_GROUP_LIST_FILTER) @NotBlank String entityGroupListFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson) {
        EntityGroupListFilter entityGroupListFilter = JacksonUtil.fromString(entityGroupListFilterJson, EntityGroupListFilter.class);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        EntityCountQuery query = new EntityCountQuery()
                .entityFilter(entityGroupListFilter)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @PeOnly
    @Tool(description = "Use this to count entity groups matching a name prefix with optional keyFilters. PE only. " + COUNT_GUIDES)
    public String countByEntityGroupNameFilter(
            @ToolParam(description = ENTITY_GROUP_NAME_FILTER) @NotBlank String entityGroupNameFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson) {
        EntityGroupNameFilter entityGroupNameFilter = JacksonUtil.fromString(entityGroupNameFilterJson, EntityGroupNameFilter.class);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        EntityCountQuery query = new EntityCountQuery()
                .entityFilter(entityGroupNameFilter)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @PeOnly
    @Tool(description = "Use this to count entities in a group by group name with optional keyFilters. PE only. " + COUNT_GUIDES)
    public String countByEntitiesGroupNameFilter(
            @ToolParam(description = ENTITIES_BY_GROUP_NAME_FILTER) @NotBlank String entitiesByGroupNameFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson) {
        EntitiesByGroupNameFilter entitiesByGroupNameFilter = JacksonUtil.fromString(entitiesByGroupNameFilterJson, EntitiesByGroupNameFilter.class);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        EntityCountQuery query = new EntityCountQuery()
                .entityFilter(entitiesByGroupNameFilter)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Use this to count assets by profile/type and optional name prefix with keyFilters. " + COUNT_GUIDES)
    public String countByAssetTypeFilter(
            @ToolParam(description = ASSET_TYPE) @NotBlank String assetTypeFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson) {
        AssetTypeFilter assetTypeFilter = JacksonUtil.fromString(assetTypeFilterJson, AssetTypeFilter.class);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        EntityCountQuery query = new EntityCountQuery()
                .entityFilter(assetTypeFilter)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Use this to count devices by profile/type and optional name prefix with keyFilters. " + COUNT_GUIDES)
    public String countByDeviceTypeFilter(
            @ToolParam(description = DEVICE_TYPE) @NotBlank String deviceTypeFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson) {
        DeviceTypeFilter deviceTypeFilter = JacksonUtil.fromString(deviceTypeFilterJson, DeviceTypeFilter.class);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        EntityCountQuery query = new EntityCountQuery()
                .entityFilter(deviceTypeFilter)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Use this to count edges by type and optional name prefix with keyFilters. " + COUNT_GUIDES)
    public String countByEdgeTypeFilter(
            @ToolParam(description = EDGE_TYPE) @NotBlank String edgeTypeFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson) {
        EdgeTypeFilter edgeTypeFilter = JacksonUtil.fromString(edgeTypeFilterJson, EdgeTypeFilter.class);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        EntityCountQuery query = new EntityCountQuery()
                .entityFilter(edgeTypeFilter)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Use this to count entity views by type and optional name prefix with keyFilters. " + COUNT_GUIDES)
    public String countByEntityViewTypeFilter(
            @ToolParam(description = ENTITY_VIEW_TYPE) @NotBlank String entityViewTypeFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson) {
        EntityViewTypeFilter entityViewTypeFilter = JacksonUtil.fromString(entityViewTypeFilterJson, EntityViewTypeFilter.class);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        EntityCountQuery query = new EntityCountQuery()
                .entityFilter(entityViewTypeFilter)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Use this to get API usage count. Customer-scoped if customer id set, otherwise tenant-scoped. " + COUNT_GUIDES)
    public String countByApiUsageStateFilter(
            @ToolParam(description = API_USAGE) @NotBlank String apiUsageStateFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson) {
        ApiUsageStateFilter apiUsageStateFilter = JacksonUtil.fromString(apiUsageStateFilterJson, ApiUsageStateFilter.class);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        EntityCountQuery query = new EntityCountQuery()
                .entityFilter(apiUsageStateFilter)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Use this to count entities related to a root entity with optional keyFilters. " + COUNT_GUIDES)
    public String countByRelationsQueryFilter(
            @ToolParam(description = RELATIONS_QUERY_FILTER) @NotBlank String relationsQueryFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson) {
        RelationsQueryFilter relationsQueryFilter = JacksonUtil.fromString(relationsQueryFilterJson, RelationsQueryFilter.class);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        EntityCountQuery query = new EntityCountQuery()
                .entityFilter(relationsQueryFilter)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Use this to count assets related to a root entity by relation type and asset types. " + COUNT_GUIDES)
    public String countByAssetSearchQueryFilter(
            @ToolParam(description = ASSET_QUERY_FILTER) @NotBlank String assetSearchQueryFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson) {
        AssetSearchQueryFilter assetSearchQueryFilter = JacksonUtil.fromString(assetSearchQueryFilterJson, AssetSearchQueryFilter.class);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        EntityCountQuery query = new EntityCountQuery()
                .entityFilter(assetSearchQueryFilter)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Use this to count devices related to a root entity by relation type and device types. " + COUNT_GUIDES)
    public String countByDeviceSearchQueryFilter(
            @ToolParam(description = DEVICE_QUERY_FILTER) @NotBlank String deviceSearchQueryFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson) {
        DeviceSearchQueryFilter deviceSearchQueryFilter = JacksonUtil.fromString(deviceSearchQueryFilterJson, DeviceSearchQueryFilter.class);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        EntityCountQuery query = new EntityCountQuery()
                .entityFilter(deviceSearchQueryFilter)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Use this to count entity views related to a root entity by relation type and view types. " + COUNT_GUIDES)
    public String countByEntityViewSearchQueryFilter(
            @ToolParam(description = EV_QUERY_FILTER) @NotBlank String entityViewSearchQueryFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson) {
        EntityViewSearchQueryFilter entityViewSearchQueryFilter = JacksonUtil.fromString(entityViewSearchQueryFilterJson, EntityViewSearchQueryFilter.class);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        EntityCountQuery query = new EntityCountQuery()
                .entityFilter(entityViewSearchQueryFilter)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Use this to count edges related to a root entity by relation type and edge types. " + COUNT_GUIDES)
    public String countByEdgeQueryFilter(
            @ToolParam(description = EDGE_QUERY_FILTER) @NotBlank String edgeSearchQueryFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS_JSON) String keyFiltersJson) {
        EdgeSearchQueryFilter edgeSearchQueryFilter = JacksonUtil.fromString(edgeSearchQueryFilterJson, EdgeSearchQueryFilter.class);
        List<KeyFilter> keyFilters = parseKeyFilters(keyFiltersJson);
        EntityCountQuery query = new EntityCountQuery()
                .entityFilter(edgeSearchQueryFilter)
                .keyFilters(keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

}
