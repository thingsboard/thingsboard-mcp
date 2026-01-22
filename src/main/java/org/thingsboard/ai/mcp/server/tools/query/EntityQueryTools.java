package org.thingsboard.ai.mcp.server.tools.query;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.annotation.PeOnly;
import org.thingsboard.ai.mcp.server.data.KeyFilterInput;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.McpTools;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.exception.ThingsboardException;
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
import static org.thingsboard.ai.mcp.server.util.ToolUtils.createPageLink;

@Service
@RequiredArgsConstructor
public class EntityQueryTools implements McpTools {

    private final RestClientService clientService;

    // Entity Data Query:
    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqGuide() - learn about EntityDataQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find Entity Data for single entity by its id. \
             Supports selectively fetching any combination of entity fields (e.g., name, type, label), attributes, and latest time-series (telemetry) values. \
             Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. \
            """)
    public String findEntityDataBySingleEntityFilter(
            @ToolParam(description = SINGLE_ENTITY) @NotBlank String singleEntityFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        SingleEntityFilter singleEntityFilter = JacksonUtil.fromString(singleEntityFilterJson, SingleEntityFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityDataQuery query = new EntityDataQuery(singleEntityFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @PeOnly
    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqGuide() - learn about EntityDataQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find Entity Data for entities of the same type using the entity group type and id. " \s
             Supports fetching entity fields (e.g., name, type), and latest values from attributes or timeseries (telemetry) with ability to define (or not)" \s
             operations complex logical expressions over entity field, attribute or latest time series value. " \s
            """ + PE_ONLY_AVAILABLE)
    public String findEntityDataByEntityGroupFilter(
            @ToolParam(description = ENTITY_GROUP_FILTER) @NotBlank String entityGroupFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityGroupFilter entityGroupFilter = JacksonUtil.fromString(entityGroupFilterJson, EntityGroupFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityDataQuery query = new EntityDataQuery(entityGroupFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqGuide() - learn about EntityDataQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find Entity Data for entities of the same type using their ids. " \s
             Supports fetching entity fields (e.g., name, type), and latest values from attributes or timeseries (telemetry) with ability to define (or not)" \s
             operations complex logical expressions over entity field, attribute or latest time series value. " \s
            """)
    public String findEntityDataByEntityListFilter(
            @ToolParam(description = ENTITY_LIST) @NotBlank String entityListFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityListFilter entityListFilter = JacksonUtil.fromString(entityListFilterJson, EntityListFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityDataQuery query = new EntityDataQuery(entityListFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
             - getEdqGuide() - learn about EntityDataQuery structure and example\s
             - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
            Find Entity Data for entities of the same type using the **'starts with'** expression over entity name. \s
            In case DEVICE, ASSET, ENTITY_VIEW, EDGE types are using and their profile/types are specified use corresponding method, like findEntityDataByDeviceTypeFilter or findEntityDataByAssetTypeFilter " \s
            Supports fetching entity fields (e.g., name, type), and latest values from attributes or timeseries (telemetry) with ability to define (or not)" \s
            operations complex logical expressions over entity field, attribute or latest time series value. " \s
            """)
    public String findEntityDataByEntityNameFilter(
            @ToolParam(description = ENTITY_NAME) @NotBlank String entityNameFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityNameFilter entityNameFilter = JacksonUtil.fromString(entityNameFilterJson, EntityNameFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityDataQuery query = new EntityDataQuery(entityNameFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqGuide() - learn about EntityDataQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find Entity Data for entities based on their type (CUSTOMER, USER, DASHBOARD, ASSET, DEVICE, ENTITY_VIEW, EDGE, TENANT)
              for Professional Edition (DATA_CONVERTER, INTEGRATION, SCHEDULER_EVENT, BLOB_ENTITY, REPORT, REPORT_TEMPLATE)" \s
             Supports fetching entity fields (e.g., name, type), and latest values from attributes or timeseries (telemetry) with ability to define (or not)" \s
             operations complex logical expressions over entity field, attribute or latest time series value. " \s
            """)
    public String findEntityDataByEntityTypeFilter(
            @ToolParam(description = ENTITY_TYPE_FILTER) @NotBlank String entityTypeFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityTypeFilter entityTypeFilter = JacksonUtil.fromString(entityTypeFilterJson, EntityTypeFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityDataQuery query = new EntityDataQuery(entityTypeFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @PeOnly
    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqGuide() - learn about EntityDataQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find Entity Data for multiple groups of the same type using specified ids." \s
             Supports fetching entity fields (e.g., name, type), and latest values from attributes or timeseries (telemetry) with ability to define (or not)" \s
             operations complex logical expressions over entity field, attribute or latest time series value. " \s
            """ + PE_ONLY_AVAILABLE)
    public String findEntityDataByEntityGroupListFilter(
            @ToolParam(description = ENTITY_GROUP_LIST_FILTER) @NotBlank String entityGroupListFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityGroupListFilter entityGroupListFilter = JacksonUtil.fromString(entityGroupListFilterJson, EntityGroupListFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityDataQuery query = new EntityDataQuery(entityGroupListFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @PeOnly
    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqGuide() - learn about EntityDataQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find Entity Data for entity groups based on their type and the **'starts with'** expression over their name." \s
             Supports fetching entity fields (e.g., name, type), and latest values from attributes or timeseries (telemetry) with ability to define (or not)" \s
             operations complex logical expressions over entity field, attribute or latest time series value. " \s
            """ + PE_ONLY_AVAILABLE)
    public String findEntityDataByEntityGroupNameFilter(
            @ToolParam(description = ENTITY_GROUP_NAME_FILTER) @NotBlank String entityGroupNameFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityGroupNameFilter entityGroupNameFilter = JacksonUtil.fromString(entityGroupNameFilterJson, EntityGroupNameFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityDataQuery query = new EntityDataQuery(entityGroupNameFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @PeOnly
    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqGuide() - learn about EntityDataQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find Entity Data for entities that belong to group based on the entity type and the group name." \s
             Supports fetching entity fields (e.g., name, type), and latest values from attributes or timeseries (telemetry) with ability to define (or not)" \s
             operations complex logical expressions over entity field, attribute or latest time series value. " \s
            """ + PE_ONLY_AVAILABLE)
    public String findEntityDataByEntitiesGroupNameFilter(
            @ToolParam(description = ENTITIES_BY_GROUP_NAME_FILTER) @NotBlank String entitiesByGroupNameFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntitiesByGroupNameFilter entitiesByGroupNameFilter = JacksonUtil.fromString(entitiesByGroupNameFilterJson, EntitiesByGroupNameFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityDataQuery query = new EntityDataQuery(entitiesByGroupNameFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqGuide() - learn about EntityDataQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find Entity Data for owner (Tenant or Customer) of the specified entity. " \s
             Supports fetching entity fields (e.g., name, type), and latest values from attributes or timeseries (telemetry) with ability to define (or not)" \s
             operations complex logical expressions over entity field, attribute or latest time series value. " \s
            """)
    public String findEntityDataByStateEntityOwnerFilter(
            @ToolParam(description = ENTITY_OWNER_FILTER) @NotBlank String stateEntityOwnerFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        StateEntityOwnerFilter stateEntityOwnerFilter = JacksonUtil.fromString(stateEntityOwnerFilterJson, StateEntityOwnerFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityDataQuery query = new EntityDataQuery(stateEntityOwnerFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqGuide() - learn about EntityDataQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find Entity Data for assets based on their type (asset profile name) and the **'starts with'** expression over their name. " \s
             Supports fetching entity fields (e.g., name, type), and latest values from attributes or timeseries (telemetry) with ability to define (or not)" \s
             operations complex logical expressions over entity field, attribute or latest time series value. " \s
            """)
    public String findEntityDataByAssetTypeFilter(
            @ToolParam(description = ASSET_TYPE) @NotBlank String assetTypeFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        AssetTypeFilter assetTypeFilter = JacksonUtil.fromString(assetTypeFilterJson, AssetTypeFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityDataQuery query = new EntityDataQuery(assetTypeFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqGuide() - learn about EntityDataQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find Entity Data for devices based on their type (device profile name) and the **'starts with'** expression over their name. " \s
             Supports fetching entity fields (e.g., name, type), and latest values from attributes or timeseries (telemetry) with ability to define (or not)" \s
             operations complex logical expressions over entity field, attribute or latest time series value. " \s
            """)
    public String findEntityDataByDeviceTypeFilter(
            @ToolParam(description = DEVICE_TYPE) @NotBlank String deviceTypeFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        DeviceTypeFilter deviceTypeFilter = JacksonUtil.fromString(deviceTypeFilterJson, DeviceTypeFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityDataQuery query = new EntityDataQuery(deviceTypeFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqGuide() - learn about EntityDataQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find Entity Data for edges based on their type and the **'starts with'** expression over their name. " \s
             Supports fetching entity fields (e.g., name, type), and latest values from attributes or timeseries (telemetry) with ability to define (or not)" \s
             operations complex logical expressions over entity field, attribute or latest time series value. " \s
            """)
    public String findEntityDataByEdgeTypeFilter(
            @ToolParam(description = EDGE_TYPE) @NotBlank String edgeTypeFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EdgeTypeFilter edgeTypeFilter = JacksonUtil.fromString(edgeTypeFilterJson, EdgeTypeFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityDataQuery query = new EntityDataQuery(edgeTypeFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqGuide() - learn about EntityDataQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find Entity Data for entity views based on their type and the **'starts with'** expression over their name. " \s
             Supports fetching entity fields (e.g., name, type), and latest values from attributes or timeseries (telemetry) with ability to define (or not)" \s
             operations complex logical expressions over entity field, attribute or latest time series value. " \s
            """)
    public String findEntityDataByEntityViewTypeFilter(
            @ToolParam(description = ENTITY_VIEW_TYPE) @NotBlank String entityViewTypeFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityViewTypeFilter entityViewTypeFilter = JacksonUtil.fromString(entityViewTypeFilterJson, EntityViewTypeFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityDataQuery query = new EntityDataQuery(entityViewTypeFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqGuide() - learn about EntityDataQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find Entity Data for for api usage based on optional customer id. If the customer id is not set, returns current tenant API usage. " \s
             Supports fetching entity fields (e.g., name, type), and latest values from attributes or timeseries (telemetry) with ability to define (or not)" \s
             operations complex logical expressions over entity field, attribute or latest time series value. " \s
            """)
    public String findEntityDataByApiUsageStateFilter(
            @ToolParam(description = API_USAGE) @NotBlank String apiUsageStateFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        ApiUsageStateFilter apiUsageStateFilter = JacksonUtil.fromString(apiUsageStateFilterJson, ApiUsageStateFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityDataQuery query = new EntityDataQuery(apiUsageStateFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqGuide() - learn about EntityDataQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find Entity Data for for entities that are related to the provided root entity. " \s
             Supports fetching entity fields (e.g., name, type), and latest values from attributes or timeseries (telemetry) with ability to define (or not)" \s
             operations complex logical expressions over entity field, attribute or latest time series value. " \s
            """)
    public String findEntityDataByRelationsQueryFilter(
            @ToolParam(description = RELATIONS_QUERY_FILTER) @NotBlank String relationsQueryFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        RelationsQueryFilter relationsQueryFilter = JacksonUtil.fromString(relationsQueryFilterJson, RelationsQueryFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityDataQuery query = new EntityDataQuery(relationsQueryFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqGuide() - learn about EntityDataQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find Entity Data for assets that are related to the provided root entity. Filters related assets based on the relation type and set of asset types. " \s
             Supports fetching entity fields (e.g., name, type), and latest values from attributes or timeseries (telemetry) with ability to define (or not)" \s
             operations complex logical expressions over entity field, attribute or latest time series value. " \s
            """)
    public String findEntityDataByAssetSearchQueryFilter(
            @ToolParam(description = ASSET_QUERY_FILTER) @NotBlank String assetSearchQueryFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        AssetSearchQueryFilter assetSearchQueryFilter = JacksonUtil.fromString(assetSearchQueryFilterJson, AssetSearchQueryFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityDataQuery query = new EntityDataQuery(assetSearchQueryFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqGuide() - learn about EntityDataQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find Entity Data for devices that are related to the provided root entity.  Filters related devices based on the relation type and set of devices types. " \s
             Supports fetching entity fields (e.g., name, type), and latest values from attributes or timeseries (telemetry) with ability to define (or not)" \s
             operations complex logical expressions over entity field, attribute or latest time series value. " \s
            """)
    public String findEntityDataByDeviceSearchQueryFilter(
            @ToolParam(description = DEVICE_QUERY_FILTER) @NotBlank String deviceSearchQueryFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        DeviceSearchQueryFilter deviceSearchQueryFilter = JacksonUtil.fromString(deviceSearchQueryFilterJson, DeviceSearchQueryFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityDataQuery query = new EntityDataQuery(deviceSearchQueryFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqGuide() - learn about EntityDataQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Tool is responsible for entity views that are related to the provided root entity. Filters related entity views based on the relation type and set of entity view types. " \s
             Supports fetching entity fields (e.g., name, type), and latest values from attributes or timeseries (telemetry) with ability to define (or not)" \s
             operations complex logical expressions over entity field, attribute or latest time series value. " \s
            """)
    public String findEntityDataByEntityViewSearchQueryFilter(
            @ToolParam(description = EV_QUERY_FILTER) @NotBlank String entityViewSearchQueryFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EntityViewSearchQueryFilter entityViewSearchQueryFilter = JacksonUtil.fromString(entityViewSearchQueryFilterJson, EntityViewSearchQueryFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityDataQuery query = new EntityDataQuery(entityViewSearchQueryFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqGuide() - learn about EntityDataQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Tool is responsible for edges that are related to the provided root entity. Filters related edges based on the relation type and set of edge types. " \s
             Supports fetching entity fields (e.g., name, type), and latest values from attributes or timeseries (telemetry) with ability to define (or not)" \s
             operations complex logical expressions over entity field, attribute or latest time series value. " \s
            """)
    public String findEntityDataByEdgeQueryFilter(
            @ToolParam(description = EDGE_QUERY_FILTER) @NotBlank String edgeSearchQueryFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> entityFields,
            @ToolParam(required = false, description = FILTER_KEY) @Valid List<EntityKey> latestValues,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the entity data.") String textSearch,
            @ToolParam(required = false, description = "Sort order key") String sortOrderKey,
            @ToolParam(required = false, description = "Sort order key type. Allowed values: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES, ENTITY_FIELD, ALARM_FIELD") String sortOrderType,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        EdgeSearchQueryFilter edgeSearchQueryFilter = JacksonUtil.fromString(edgeSearchQueryFilterJson, EdgeSearchQueryFilter.class);
        EntityDataPageLink pageLink = createPageLink(pageSize, page, textSearch, sortOrderKey, sortOrderType, sortOrder);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityDataQuery query = new EntityDataQuery(edgeSearchQueryFilter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    // Entity Count Query:
    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqCountGuide() - learn about EntityCountQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find the amount of entities by its ids that are matching keyFilters if it presents. \
            """)
    public String countBySingleEntityFilter(
            @ToolParam(description = SINGLE_ENTITY) @NotBlank String singleEntityFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs) {
        SingleEntityFilter singleEntityFilter = JacksonUtil.fromString(singleEntityFilterJson, SingleEntityFilter.class);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityCountQuery query = new EntityCountQuery(singleEntityFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @PeOnly
    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqCountGuide() - learn about EntityCountQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find the amount of entities of the same type using the entity group type and id that are matching keyFilters if it presents. \
            """ + PE_ONLY_AVAILABLE)
    public String countByEntityGroupFilter(
            @ToolParam(description = ENTITY_GROUP_FILTER) @NotBlank String entityGroupFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs) {
        EntityGroupFilter entityGroupFilter = JacksonUtil.fromString(entityGroupFilterJson, EntityGroupFilter.class);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityCountQuery query = new EntityCountQuery(entityGroupFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqCountGuide() - learn about EntityCountQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find the amount of entities of the same type using their ids that are matching keyFilters if it presents. \
            """)
    public String countByEntityListFilter(
            @ToolParam(description = ENTITY_LIST) @NotBlank String entityListFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs) {
        EntityListFilter entityListFilter = JacksonUtil.fromString(entityListFilterJson, EntityListFilter.class);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityCountQuery query = new EntityCountQuery(entityListFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqCountGuide() - learn about EntityCountQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find the amount of entities of the same type using the **'starts with'** expression over entity name that are matching keyFilters if it presents. \
            """)
    public String countByEntityNameFilter(
            @ToolParam(description = ENTITY_NAME) @NotBlank String entityNameFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs) {
        EntityNameFilter entityNameFilter = JacksonUtil.fromString(entityNameFilterJson, EntityNameFilter.class);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityCountQuery query = new EntityCountQuery(entityNameFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqCountGuide() - learn about EntityCountQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find the amount of entities based on their type (CUSTOMER, USER, DASHBOARD, ASSET, DEVICE, ENTITY_VIEW, EDGE, TENANT)
             for Professional Edition (DATA_CONVERTER, INTEGRATION, SCHEDULER_EVENT, BLOB_ENTITY, REPORT, REPORT_TEMPLATE) that are matching keyFilters if it presents. \
            """)
    public String countByEntityTypeFilter(
            @ToolParam(description = ENTITY_TYPE_FILTER) @NotBlank String entityTypeFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs) {
        EntityTypeFilter entityTypeFilter = JacksonUtil.fromString(entityTypeFilterJson, EntityTypeFilter.class);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityCountQuery query = new EntityCountQuery(entityTypeFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @PeOnly
    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqCountGuide() - learn about EntityCountQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find the amount of entities multiple groups of the same type using specified ids that are matching keyFilters if it presents. \
            """ + PE_ONLY_AVAILABLE)
    public String countByEntityGroupListFilter(
            @ToolParam(description = ENTITY_GROUP_LIST_FILTER) @NotBlank String entityGroupListFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs) {
        EntityGroupListFilter entityGroupListFilter = JacksonUtil.fromString(entityGroupListFilterJson, EntityGroupListFilter.class);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityCountQuery query = new EntityCountQuery(entityGroupListFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @PeOnly
    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqCountGuide() - learn about EntityCountQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find the amount of entity groups based on their type and the **'starts with'** expression over their name that are matching keyFilters if it presents. \
            """ + PE_ONLY_AVAILABLE)
    public String countByEntityGroupNameFilter(
            @ToolParam(description = ENTITY_GROUP_NAME_FILTER) @NotBlank String entityGroupNameFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs) {
        EntityGroupNameFilter entityGroupNameFilter = JacksonUtil.fromString(entityGroupNameFilterJson, EntityGroupNameFilter.class);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityCountQuery query = new EntityCountQuery(entityGroupNameFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @PeOnly
    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqCountGuide() - learn about EntityCountQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find the amount of  that belong to group based on the entity type and the group name that are matching keyFilters if it presents. \
            """ + PE_ONLY_AVAILABLE)
    public String countByEntitiesGroupNameFilter(
            @ToolParam(description = ENTITIES_BY_GROUP_NAME_FILTER) @NotBlank String entitiesByGroupNameFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs) {
        EntitiesByGroupNameFilter entitiesByGroupNameFilter = JacksonUtil.fromString(entitiesByGroupNameFilterJson, EntitiesByGroupNameFilter.class);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityCountQuery query = new EntityCountQuery(entitiesByGroupNameFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqCountGuide() - learn about EntityCountQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find the amount of assets based on their type (asset profile name) and the **'starts with'** expression over their name that are matching keyFilters if it presents. \
            """)
    public String countByAssetTypeFilter(
            @ToolParam(description = ASSET_TYPE) @NotBlank String assetTypeFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs) {
        AssetTypeFilter assetTypeFilter = JacksonUtil.fromString(assetTypeFilterJson, AssetTypeFilter.class);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityCountQuery query = new EntityCountQuery(assetTypeFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqCountGuide() - learn about EntityCountQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find the amount of devices based on their type (device profile name) and the **'starts with'** expression over their name that are matching keyFilters if it presents. \
            """)
    public String countByDeviceTypeFilter(
            @ToolParam(description = DEVICE_TYPE) @NotBlank String deviceTypeFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs) {
        DeviceTypeFilter deviceTypeFilter = JacksonUtil.fromString(deviceTypeFilterJson, DeviceTypeFilter.class);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityCountQuery query = new EntityCountQuery(deviceTypeFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqCountGuide() - learn about EntityCountQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find the amount of edges based on their type and the **'starts with'** expression over their name that are matching keyFilters if it presents. \
            """)
    public String countByEdgeTypeFilter(
            @ToolParam(description = EDGE_TYPE) @NotBlank String edgeTypeFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs) {
        EdgeTypeFilter edgeTypeFilter = JacksonUtil.fromString(edgeTypeFilterJson, EdgeTypeFilter.class);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityCountQuery query = new EntityCountQuery(edgeTypeFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqCountGuide() - learn about EntityCountQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find the amount of entity views based on their type and the **'starts with'** expression over their name that are matching keyFilters if it presents. \
            """)
    public String countByEntityViewTypeFilter(
            @ToolParam(description = ENTITY_VIEW_TYPE) @NotBlank String entityViewTypeFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs) {
        EntityViewTypeFilter entityViewTypeFilter = JacksonUtil.fromString(entityViewTypeFilterJson, EntityViewTypeFilter.class);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityCountQuery query = new EntityCountQuery(entityViewTypeFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqCountGuide() - learn about EntityCountQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find the api usage based on optional customer id. If the customer id is not set, returns current tenant API usage that are matching keyFilters if it presents. \
            """)
    public String countByApiUsageStateFilter(
            @ToolParam(description = API_USAGE) @NotBlank String apiUsageStateFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs) {
        ApiUsageStateFilter apiUsageStateFilter = JacksonUtil.fromString(apiUsageStateFilterJson, ApiUsageStateFilter.class);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityCountQuery query = new EntityCountQuery(apiUsageStateFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqCountGuide() - learn about EntityCountQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find the entities that are related to the provided root entity and matching keyFilters if it presents. \
            """)
    public String countByRelationsQueryFilter(
            @ToolParam(description = RELATIONS_QUERY_FILTER) @NotBlank String relationsQueryFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs) {
        RelationsQueryFilter relationsQueryFilter = JacksonUtil.fromString(relationsQueryFilterJson, RelationsQueryFilter.class);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityCountQuery query = new EntityCountQuery(relationsQueryFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqCountGuide() - learn about EntityCountQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find of assets that are related to the provided root entity. Filters related assets based on the relation type and set of asset types that are matching keyFilters if it presents. \
            """)
    public String countByAssetSearchQueryFilter(
            @ToolParam(description = ASSET_QUERY_FILTER) @NotBlank String assetSearchQueryFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs) {
        AssetSearchQueryFilter assetSearchQueryFilter = JacksonUtil.fromString(assetSearchQueryFilterJson, AssetSearchQueryFilter.class);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityCountQuery query = new EntityCountQuery(assetSearchQueryFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqCountGuide() - learn about EntityCountQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find of devices that are related to the provided root entity. Filters related devices based on the relation type and set of device types that are matching keyFilters if it presents. \
             Allows to define (if needed) complex logical expressions over entity field, attribute or latest time-series value. \
            """)
    public String countByDeviceSearchQueryFilter(
            @ToolParam(description = DEVICE_QUERY_FILTER) @NotBlank String deviceSearchQueryFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs) {
        DeviceSearchQueryFilter deviceSearchQueryFilter = JacksonUtil.fromString(deviceSearchQueryFilterJson, DeviceSearchQueryFilter.class);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityCountQuery query = new EntityCountQuery(deviceSearchQueryFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqCountGuide() - learn about EntityCountQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find of entity views that are related to the provided root entity. Filters related entity views based on the \
             relation type and set of entity views types that are matching keyFilters if it presents. \
            """)
    public String countByEntityViewSearchQueryFilter(
            @ToolParam(description = EV_QUERY_FILTER) @NotBlank String entityViewSearchQueryFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs) {
        EntityViewSearchQueryFilter entityViewSearchQueryFilter = JacksonUtil.fromString(entityViewSearchQueryFilterJson, EntityViewSearchQueryFilter.class);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityCountQuery query = new EntityCountQuery(entityViewSearchQueryFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

    @Tool(description = """
            IMPORTANT: use this tool when you need to make complex expression over entities field, time-series values or attributes, before using this tool, call these helpers: \s
              - getEdqCountGuide() - learn about EntityCountQuery structure and example\s
              - getKeyFiltersGuide() - learn how to build keyFilters and complex conditions.\s
             Find of edges that are related to the provided root entity. Filters related edges based on the relation type \
             and set of edges types that are matching keyFilters if it presents. \
            """)
    public String countByEdgeQueryFilter(
            @ToolParam(description = EDGE_QUERY_FILTER) @NotBlank String edgeSearchQueryFilterJson,
            @ToolParam(required = false, description = KEY_FILTERS) @Valid List<KeyFilterInput> keyFilterInputs) {
        EdgeSearchQueryFilter edgeSearchQueryFilter = JacksonUtil.fromString(edgeSearchQueryFilterJson, EdgeSearchQueryFilter.class);
        List<KeyFilter> keyFilters = null;
        if (keyFilterInputs != null && !keyFilterInputs.isEmpty()) {
            keyFilters = keyFilterInputs.stream().map(KeyFilterInput::toKeyFilter).toList();
        }
        EntityCountQuery query = new EntityCountQuery(edgeSearchQueryFilter, keyFilters);
        return JacksonUtil.toString(clientService.getClient().countEntitiesByQuery(query));
    }

}
