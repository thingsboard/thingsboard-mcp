package org.thingsboard.ai.mcp.server.tools.query;

import com.fasterxml.jackson.core.type.TypeReference;
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
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ASSET_TEXT_SEARCH_DESCRIPTION;
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

    private final String ENTITY_KEY = "A list of Filter keys in canonic json format. " + FILTER_KEY;
    private final String KEY_FILTER = "A list of key filters in canonic json format. " + KEY_FILTERS;

    // Entity Data Query:
    @Tool(description = "Find Entity Data for single entity by its id. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataBySingleEntityFilter(
            @ToolParam(description = SINGLE_ENTITY) @Valid @NotNull String singleEntityFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String entityFields,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        SingleEntityFilter filter = JacksonUtil.fromString(singleEntityFilter, SingleEntityFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        List<EntityKey> entityFieldsList = JacksonUtil.fromString(entityFields, new TypeReference<>() {});
        List<EntityKey> latestValuesList = JacksonUtil.fromString(latestValues, new TypeReference<>() {});
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFieldsList, latestValuesList, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Find Entity Data for entities of the same type using the entity group type and id. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByEntityGroupFilter(
            @ToolParam(description = ENTITY_GROUP_FILTER) @Valid @NotNull String entityGroupFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String entityFields,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityGroupFilter filter = JacksonUtil.fromString(entityGroupFilter, EntityGroupFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        List<EntityKey> entityFieldsList = JacksonUtil.fromString(entityFields, new TypeReference<>() {});
        List<EntityKey> latestValuesList = JacksonUtil.fromString(latestValues, new TypeReference<>() {});
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFieldsList, latestValuesList, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Find Entity Data for entities of the same type using their ids. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByEntityListFilter(
            @ToolParam(description = ENTITY_LIST) @Valid @NotNull String entityListFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String entityFields,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityListFilter filter = JacksonUtil.fromString(entityListFilter, EntityListFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        List<EntityKey> entityFieldsList = JacksonUtil.fromString(entityFields, new TypeReference<>() {});
        List<EntityKey> latestValuesList = JacksonUtil.fromString(latestValues, new TypeReference<>() {});
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFieldsList, latestValuesList, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Find Entity Data for entities of the same type using the **'starts with'** expression over entity name. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByEntityNameFilter(
            @ToolParam(description = ENTITY_NAME) @Valid @NotNull String entityNameFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String entityFields,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityNameFilter filter = JacksonUtil.fromString(entityNameFilter, EntityNameFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        List<EntityKey> entityFieldsList = JacksonUtil.fromString(entityFields, new TypeReference<>() {});
        List<EntityKey> latestValuesList = JacksonUtil.fromString(latestValues, new TypeReference<>() {});
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFieldsList, latestValuesList, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Find Entity Data for entities based on their type (CUSTOMER, USER, DASHBOARD, ASSET, DEVICE, etc). All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByEntityTypeFilter(
            @ToolParam(description = ENTITY_TYPE_FILTER) @Valid @NotNull String entityTypeFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String entityFields,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityTypeFilter filter = JacksonUtil.fromString(entityTypeFilter, EntityTypeFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        List<EntityKey> entityFieldsList = JacksonUtil.fromString(entityFields, new TypeReference<>() {});
        List<EntityKey> latestValuesList = JacksonUtil.fromString(latestValues, new TypeReference<>() {});
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFieldsList, latestValuesList, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @PeOnly
    @Tool(description = "Find Entity Data for multiple groups of the same type using specified ids. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByEntityGroupListFilter(
            @ToolParam(description = ENTITY_GROUP_LIST_FILTER) @Valid @NotNull String entityGroupListFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String entityFields,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityGroupListFilter filter = JacksonUtil.fromString(entityGroupListFilter, EntityGroupListFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        List<EntityKey> entityFieldsList = JacksonUtil.fromString(entityFields, new TypeReference<>() {});
        List<EntityKey> latestValuesList = JacksonUtil.fromString(latestValues, new TypeReference<>() {});
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFieldsList, latestValuesList, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @PeOnly
    @Tool(description = "Find Entity Data for entity groups based on their type and the **'starts with'** expression over their name. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide()." + PE_ONLY_AVAILABLE)
    public String findEntityDataByEntityGroupNameFilter(
            @ToolParam(description = ENTITY_GROUP_NAME_FILTER) @Valid @NotNull String entityGroupNameFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String entityFields,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityGroupNameFilter filter = JacksonUtil.fromString(entityGroupNameFilter, EntityGroupNameFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        List<EntityKey> entityFieldsList = JacksonUtil.fromString(entityFields, new TypeReference<>() {});
        List<EntityKey> latestValuesList = JacksonUtil.fromString(latestValues, new TypeReference<>() {});
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFieldsList, latestValuesList, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @PeOnly
    @Tool(description = "Find Entity Data for entities that belong to group based on the entity type and the group name. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide()." + PE_ONLY_AVAILABLE)
    public String findEntityDataByEntitiesGroupNameFilter(
            @ToolParam(description = ENTITIES_BY_GROUP_NAME_FILTER) @Valid @NotNull String entitiesByGroupNameFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String entityFields,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntitiesByGroupNameFilter filter = JacksonUtil.fromString(entitiesByGroupNameFilter, EntitiesByGroupNameFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        List<EntityKey> entityFieldsList = JacksonUtil.fromString(entityFields, new TypeReference<>() {});
        List<EntityKey> latestValuesList = JacksonUtil.fromString(latestValues, new TypeReference<>() {});
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFieldsList, latestValuesList, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Find Entity Data for owner (Tenant or Customer) of the specified entity. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByStateEntityOwnerFilter(
            @ToolParam(description = ENTITY_OWNER_FILTER) @Valid @NotNull String stateEntityOwnerFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String entityFields,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        StateEntityOwnerFilter filter = JacksonUtil.fromString(stateEntityOwnerFilter, StateEntityOwnerFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        List<EntityKey> entityFieldsList = JacksonUtil.fromString(entityFields, new TypeReference<>() {});
        List<EntityKey> latestValuesList = JacksonUtil.fromString(latestValues, new TypeReference<>() {});
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFieldsList, latestValuesList, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Find Entity Data for assets based on their type and the **'starts with'** expression over their name. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByAssetTypeFilter(
            @ToolParam(description = ASSET_TYPE) @Valid @NotNull String assetTypeFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String entityFields,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        AssetTypeFilter filter = JacksonUtil.fromString(assetTypeFilter, AssetTypeFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        List<EntityKey> entityFieldsList = JacksonUtil.fromString(entityFields, new TypeReference<>() {});
        List<EntityKey> latestValuesList = JacksonUtil.fromString(latestValues, new TypeReference<>() {});
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFieldsList, latestValuesList, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Find Entity Data for devices based on their type and the **'starts with'** expression over their name. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByDeviceTypeFilter(
            @ToolParam(description = DEVICE_TYPE) @Valid @NotNull String deviceTypeFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String entityFields,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        DeviceTypeFilter filter = JacksonUtil.fromString(deviceTypeFilter, DeviceTypeFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        List<EntityKey> entityFieldsList = JacksonUtil.fromString(entityFields, new TypeReference<>() {});
        List<EntityKey> latestValuesList = JacksonUtil.fromString(latestValues, new TypeReference<>() {});
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFieldsList, latestValuesList, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Find Entity Data for edges based on their type and the **'starts with'** expression over their name. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByEdgeTypeFilter(
            @ToolParam(description = EDGE_TYPE) @Valid @NotNull String edgeTypeFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String entityFields,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EdgeTypeFilter filter = JacksonUtil.fromString(edgeTypeFilter, EdgeTypeFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        List<EntityKey> entityFieldsList = JacksonUtil.fromString(entityFields, new TypeReference<>() {});
        List<EntityKey> latestValuesList = JacksonUtil.fromString(latestValues, new TypeReference<>() {});
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFieldsList, latestValuesList, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Find Entity Data for entity views based on their type and the **'starts with'** expression over their name. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByEntityViewTypeFilter(
            @ToolParam(description = ENTITY_VIEW_TYPE) @Valid @NotNull String entityViewTypeFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String entityFields,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityViewTypeFilter filter = JacksonUtil.fromString(entityViewTypeFilter, EntityViewTypeFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        List<EntityKey> entityFieldsList = JacksonUtil.fromString(entityFields, new TypeReference<>() {});
        List<EntityKey> latestValuesList = JacksonUtil.fromString(latestValues, new TypeReference<>() {});
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFieldsList, latestValuesList, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Find Entity Data for api usage based on optional customer id. If the customer id is not set, returns current tenant API usage. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByApiUsageStateFilter(
            @ToolParam(description = API_USAGE) @Valid @NotNull String apiUsageStateFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String entityFields,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        ApiUsageStateFilter filter = JacksonUtil.fromString(apiUsageStateFilter, ApiUsageStateFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        List<EntityKey> entityFieldsList = JacksonUtil.fromString(entityFields, new TypeReference<>() {});
        List<EntityKey> latestValuesList = JacksonUtil.fromString(latestValues, new TypeReference<>() {});
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFieldsList, latestValuesList, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Find Entity Data for entities that are related to the provided root entity. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByRelationsQueryFilter(
            @ToolParam(description = RELATIONS_QUERY_FILTER) @Valid @NotNull String relationsQueryFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String entityFields,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        RelationsQueryFilter filter = JacksonUtil.fromString(relationsQueryFilter, RelationsQueryFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        List<EntityKey> entityFieldsList = JacksonUtil.fromString(entityFields, new TypeReference<>() {});
        List<EntityKey> latestValuesList = JacksonUtil.fromString(latestValues, new TypeReference<>() {});
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFieldsList, latestValuesList, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Find Entity Data for assets that are related to the provided root entity. All list/complex input MUST be canonic JSON. Filters related assets based on the relation type and set of asset types. Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByAssetSearchQueryFilter(
            @ToolParam(description = ASSET_QUERY_FILTER) @Valid @NotNull String assetSearchQueryFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String entityFields,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        AssetSearchQueryFilter filter = JacksonUtil.fromString(assetSearchQueryFilter, AssetSearchQueryFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        List<EntityKey> entityFieldsList = JacksonUtil.fromString(entityFields, new TypeReference<>() {});
        List<EntityKey> latestValuesList = JacksonUtil.fromString(latestValues, new TypeReference<>() {});
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFieldsList, latestValuesList, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Find Entity Data for devices that are related to the provided root entity. All list/complex input MUST be canonic JSON. Filters related assets based on the relation type and set of asset types. Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByDeviceSearchQueryFilter(
            @ToolParam(description = DEVICE_QUERY_FILTER) @Valid @NotNull String deviceSearchQueryFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String entityFields,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        DeviceSearchQueryFilter filter = JacksonUtil.fromString(deviceSearchQueryFilter, DeviceSearchQueryFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        List<EntityKey> entityFieldsList = JacksonUtil.fromString(entityFields, new TypeReference<>() {});
        List<EntityKey> latestValuesList = JacksonUtil.fromString(latestValues, new TypeReference<>() {});
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFieldsList, latestValuesList, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Find Entity Data for entity views that are related to the provided root entity. All list/complex input MUST be canonic JSON. Filters related assets based on the relation type and set of asset types. Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByEntityViewSearchQueryFilter(
            @ToolParam(description = EV_QUERY_FILTER) @Valid @NotNull String entityViewSearchQueryFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String entityFields,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityViewSearchQueryFilter filter = JacksonUtil.fromString(entityViewSearchQueryFilter, EntityViewSearchQueryFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        List<EntityKey> entityFieldsList = JacksonUtil.fromString(entityFields, new TypeReference<>() {});
        List<EntityKey> latestValuesList = JacksonUtil.fromString(latestValues, new TypeReference<>() {});
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFieldsList, latestValuesList, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = "Find Entity Data for edges that are related to the provided root entity. All list/complex input MUST be canonic JSON. Filters related assets based on the relation type and set of asset types. Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findEntityDataByEdgeQueryFilter(
            @ToolParam(description = EDGE_QUERY_FILTER) @Valid @NotNull String edgeSearchQueryFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String entityFields,
            @ToolParam(required = false, description = ENTITY_KEY) @Valid String latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EdgeSearchQueryFilter filter = JacksonUtil.fromString(edgeSearchQueryFilter, EdgeSearchQueryFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        List<EntityKey> entityFieldsList = JacksonUtil.fromString(entityFields, new TypeReference<>() {});
        List<EntityKey> latestValuesList = JacksonUtil.fromString(latestValues, new TypeReference<>() {});
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFieldsList, latestValuesList, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    // Entity Count Query:
    @Tool(description = "Find Entity Data count of single entity by its id. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqCountGuide() and getKeyFiltersGuide().")
    public String findEntityDataCountBySingleEntityFilter(
            @ToolParam(description = SINGLE_ENTITY) @Valid @NotNull String singleEntityFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters) {
        SingleEntityFilter filter = JacksonUtil.fromString(singleEntityFilter, SingleEntityFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        EntityCountQuery query = new EntityCountQuery(filter, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Find Entity Data count of entities of the same type using the entity group type and id. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqCountGuide() and getKeyFiltersGuide().")
    public String findEntityDataCountByEntityGroupFilter(
            @ToolParam(description = ENTITY_GROUP_FILTER) @Valid @NotNull String entityGroupFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters) {
        EntityGroupFilter filter = JacksonUtil.fromString(entityGroupFilter, EntityGroupFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        EntityCountQuery query = new EntityCountQuery(filter, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Find Entity Data count of entities of the same type using their ids. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqCountGuide() and getKeyFiltersGuide().")
    public String findEntityDataCountByEntityListFilter(
            @ToolParam(description = ENTITY_LIST) @Valid @NotNull String entityListFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters) {
        EntityListFilter filter = JacksonUtil.fromString(entityListFilter, EntityListFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        EntityCountQuery query = new EntityCountQuery(filter, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Find Entity Data count of entities of the same type using the **'starts with'** expression over entity name. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqCountGuide() and getKeyFiltersGuide().")
    public String findEntityDataCountByEntityNameFilter(
            @ToolParam(description = ENTITY_NAME) @Valid @NotNull String entityNameFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters) {
        EntityNameFilter filter = JacksonUtil.fromString(entityNameFilter, EntityNameFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        EntityCountQuery query = new EntityCountQuery(filter, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Find Entity Data count of entities based on their type (CUSTOMER, USER, DASHBOARD, ASSET, DEVICE, etc). All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqCountGuide() and getKeyFiltersGuide().")
    public String findEntityDataCountByEntityTypeFilter(
            @ToolParam(description = ENTITY_TYPE_FILTER) @Valid @NotNull String entityTypeFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters) {
        EntityTypeFilter filter = JacksonUtil.fromString(entityTypeFilter, EntityTypeFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        EntityCountQuery query = new EntityCountQuery(filter, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @PeOnly
    @Tool(description = "Find Entity Data count of multiple groups of the same type using specified ids. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqCountGuide() and getKeyFiltersGuide().")
    public String findEntityDataCountByEntityGroupListFilter(
            @ToolParam(description = ENTITY_GROUP_LIST_FILTER) @Valid @NotNull String entityGroupListFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters) {
        EntityGroupListFilter filter = JacksonUtil.fromString(entityGroupListFilter, EntityGroupListFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        EntityCountQuery query = new EntityCountQuery(filter, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @PeOnly
    @Tool(description = "Find Entity Data count of entity groups based on their type and the **'starts with'** expression over their name. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqCountGuide() and getKeyFiltersGuide()." + PE_ONLY_AVAILABLE)
    public String findEntityDataCountByEntityGroupNameFilter(
            @ToolParam(description = ENTITY_GROUP_NAME_FILTER) @Valid @NotNull String entityGroupNameFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters) {
        EntityGroupNameFilter filter = JacksonUtil.fromString(entityGroupNameFilter, EntityGroupNameFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        EntityCountQuery query = new EntityCountQuery(filter, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @PeOnly
    @Tool(description = "Find Entity Data count of entities that belong to group based on the entity type and the group name. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqCountGuide() and getKeyFiltersGuide()." + PE_ONLY_AVAILABLE)
    public String findEntityDataCountByEntitiesGroupNameFilter(
            @ToolParam(description = ENTITIES_BY_GROUP_NAME_FILTER) @Valid @NotNull String entitiesByGroupNameFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters) {
        EntitiesByGroupNameFilter filter = JacksonUtil.fromString(entitiesByGroupNameFilter, EntitiesByGroupNameFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        EntityCountQuery query = new EntityCountQuery(filter, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Find Entity Data count of owner (Tenant or Customer) of the specified entity. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqCountGuide() and getKeyFiltersGuide().")
    public String findEntityDataCountByStateEntityOwnerFilter(
            @ToolParam(description = ENTITY_OWNER_FILTER) @Valid @NotNull String stateEntityOwnerFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters) {
        StateEntityOwnerFilter filter = JacksonUtil.fromString(stateEntityOwnerFilter, StateEntityOwnerFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        EntityCountQuery query = new EntityCountQuery(filter, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Find Entity Data count of assets based on their type and the **'starts with'** expression over their name. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqCountGuide() and getKeyFiltersGuide().")
    public String findEntityDataCountByAssetTypeFilter(
            @ToolParam(description = ASSET_TYPE) @Valid @NotNull String assetTypeFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters) {
        AssetTypeFilter filter = JacksonUtil.fromString(assetTypeFilter, AssetTypeFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        EntityCountQuery query = new EntityCountQuery(filter, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Find Entity Data count of devices based on their type and the **'starts with'** expression over their name. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqCountGuide() and getKeyFiltersGuide().")
    public String findEntityDataCountByDeviceTypeFilter(
            @ToolParam(description = DEVICE_TYPE) @Valid @NotNull String deviceTypeFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters) {
        DeviceTypeFilter filter = JacksonUtil.fromString(deviceTypeFilter, DeviceTypeFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        EntityCountQuery query = new EntityCountQuery(filter, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Find Entity Data count of edges based on their type and the **'starts with'** expression over their name. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqCountGuide() and getKeyFiltersGuide().")
    public String findEntityDataCountByEdgeTypeFilter(
            @ToolParam(description = EDGE_TYPE) @Valid @NotNull String edgeTypeFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters) {
        EdgeTypeFilter filter = JacksonUtil.fromString(edgeTypeFilter, EdgeTypeFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        EntityCountQuery query = new EntityCountQuery(filter, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Find Entity Data count of entity views based on their type and the **'starts with'** expression over their name. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqCountGuide() and getKeyFiltersGuide().")
    public String findEntityDataCountByEntityViewTypeFilter(
            @ToolParam(description = ENTITY_VIEW_TYPE) @Valid @NotNull String entityViewTypeFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters) {
        EntityViewTypeFilter filter = JacksonUtil.fromString(entityViewTypeFilter, EntityViewTypeFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        EntityCountQuery query = new EntityCountQuery(filter, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Find Entity Data count of api usage based on optional customer id. If the customer id is not set, returns current tenant API usage. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqCountGuide() and getKeyFiltersGuide().")
    public String findEntityDataCountByApiUsageStateFilter(
            @ToolParam(description = API_USAGE) @Valid @NotNull String apiUsageStateFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters) {
        ApiUsageStateFilter filter = JacksonUtil.fromString(apiUsageStateFilter, ApiUsageStateFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        EntityCountQuery query = new EntityCountQuery(filter, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Find Entity Data count of entities that are related to the provided root entity. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqCountGuide() and getKeyFiltersGuide().")
    public String findEntityDataCountByRelationsQueryFilter(
            @ToolParam(description = RELATIONS_QUERY_FILTER) @Valid @NotNull String relationsQueryFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters) {
        RelationsQueryFilter filter = JacksonUtil.fromString(relationsQueryFilter, RelationsQueryFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        EntityCountQuery query = new EntityCountQuery(filter, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Find Entity Data count of assets that are related to the provided root entity. Filters related assets based on the relation type and set of asset types. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqCountGuide() and getKeyFiltersGuide().")
    public String findEntityDataCountByAssetSearchQueryFilter(
            @ToolParam(description = ASSET_QUERY_FILTER) @Valid @NotNull String assetSearchQueryFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters) {
        AssetSearchQueryFilter filter = JacksonUtil.fromString(assetSearchQueryFilter, AssetSearchQueryFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        EntityCountQuery query = new EntityCountQuery(filter, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Find Entity Data count of devices that are related to the provided root entity. Filters related assets based on the relation type and set of asset types. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqCountGuide() and getKeyFiltersGuide().")
    public String findEntityDataCountByDeviceSearchQueryFilter(
            @ToolParam(description = DEVICE_QUERY_FILTER) @Valid @NotNull String deviceSearchQueryFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters) {
        DeviceSearchQueryFilter filter = JacksonUtil.fromString(deviceSearchQueryFilter, DeviceSearchQueryFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        EntityCountQuery query = new EntityCountQuery(filter, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Find Entity Data count of entity views that are related to the provided root entity. Filters related assets based on the relation type and set of asset types. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqCountGuide() and getKeyFiltersGuide().")
    public String findEntityDataCountByEntityViewSearchQueryFilter(
            @ToolParam(description = EV_QUERY_FILTER) @Valid @NotNull String entityViewSearchQueryFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters) {
        EntityViewSearchQueryFilter filter = JacksonUtil.fromString(entityViewSearchQueryFilter, EntityViewSearchQueryFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        EntityCountQuery query = new EntityCountQuery(filter, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = "Find Entity Data count of edges that are related to the provided root entity. Filters related assets based on the relation type and set of asset types. All list/complex input MUST be canonic JSON. Tip: for structure & examples, call getEdqCountGuide() and getKeyFiltersGuide().")
    public String findEntityDataCountByEdgeQueryFilter(
            @ToolParam(description = EDGE_QUERY_FILTER) @Valid @NotNull String edgeSearchQueryFilter,
            @ToolParam(required = false, description = KEY_FILTER) @Valid String keyFilters) {
        EdgeSearchQueryFilter filter = JacksonUtil.fromString(edgeSearchQueryFilter, EdgeSearchQueryFilter.class, true);
        List<KeyFilter> keyFiltersList = JacksonUtil.fromString(keyFilters, new TypeReference<>() {});
        EntityCountQuery query = new EntityCountQuery(filter, keyFiltersList);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = ALARM_DATA_QUERY_DESCRIPTION + "Tip: for structure & examples, call getEdqGuide() and getKeyFiltersGuide().")
    public String findAlarmDataByQuery(
            @ToolParam(description = "A JSON value representing the alarm data query.") String strQuery) {
        AlarmDataQuery query = JacksonUtil.fromString(strQuery, AlarmDataQuery.class, true);
        return JacksonUtil.toString(clientService.getClient().findAlarmDataByQuery(query));
    }

    @Tool(description = "Count Alarms by Query. Allows to run complex queries to count the alarms based on the combination of alarm fields filter and multiple key filters. " +
            "Returns the number of alarms that match the query definition." +
            TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String countAlarmsByQuery(
            @ToolParam(description = "A JSON value representing the alarm count query.") String strQuery) {
        AlarmCountQuery query = JacksonUtil.fromString(strQuery, AlarmCountQuery.class, true);
        return JacksonUtil.toString(clientService.getClient().countAlarmsByQuery(query));
    }

}
