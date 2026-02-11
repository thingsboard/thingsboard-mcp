package org.thingsboard.ai.mcp.server.tools.telemetry;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.annotation.ToolGroup;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.McpTools;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.IntervalType;
import org.thingsboard.server.common.data.page.SortOrder;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ATTRIBUTES_JSON_REQUEST_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ATTRIBUTES_KEYS_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ATTRIBUTES_SCOPE_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.DEVICE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.STRICT_DATA_TYPES_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.TELEMETRY_JSON_REQUEST_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.TELEMETRY_KEYS_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.util.ToolUtils.parseLong;

@Service
@RequiredArgsConstructor
@ToolGroup("telemetry")
public class TelemetryTools implements McpTools {

    private final RestClientService clientService;

    private static List<String> parseKeys(String keys) {
        if (keys == null || keys.isBlank()) {
            return List.of();
        }
        return List.of(keys.split(","));
    }

    @Tool(description = "Use this to get all attribute key names for an entity (merged across SERVER_SCOPE, CLIENT_SCOPE, SHARED_SCOPE).")
    public String getAttributeKeys(
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String entityIdStr) {
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
        return JacksonUtil.toString(clientService.getClient().getAttributeKeys(entityId));
    }

    @Tool(description = "Use this to get attribute key names for an entity filtered by scope (SERVER_SCOPE, CLIENT_SCOPE, SHARED_SCOPE).")
    public String getAttributeKeysByScope(
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String entityIdStr,
            @ToolParam(description = ATTRIBUTES_SCOPE_DESCRIPTION + " Allowable values: 'SERVER_SCOPE', 'SHARED_SCOPE', 'CLIENT_SCOPE'") @NotBlank String scope) {
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
        return JacksonUtil.toString(clientService.getClient().getAttributeKeysByScope(entityId, scope));
    }

    @Tool(description = "Use this to get all attributes for an entity. Use optional 'keys' to return specific attributes.")
    public String getAttributes(
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String entityIdStr,
            @ToolParam(required = false, description = ATTRIBUTES_KEYS_DESCRIPTION) String keys) {
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
        return JacksonUtil.toString(clientService.getClient().getAttributeKvEntries(entityId, parseKeys(keys)));
    }

    @Tool(description = "Use this to get attributes for an entity filtered by scope. Scopes: SERVER_SCOPE, SHARED_SCOPE, CLIENT_SCOPE (devices only). Use optional 'keys' to filter.")
    public String getAttributesByScope(
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String entityIdStr,
            @ToolParam(description = ATTRIBUTES_SCOPE_DESCRIPTION + " Allowable values: 'SERVER_SCOPE', 'SHARED_SCOPE', 'CLIENT_SCOPE'") @NotBlank String scope,
            @ToolParam(required = false, description = ATTRIBUTES_KEYS_DESCRIPTION) String keys) {
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
        return JacksonUtil.toString(clientService.getClient().getAttributesByScope(entityId, scope, parseKeys(keys)));
    }

    @Tool(description = "Use this to get all time series key names for an entity.")
    public String getTimeseriesKeys(
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String entityIdStr) {
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
        return JacksonUtil.toString(clientService.getClient().getTimeseriesKeys(entityId));
    }

    @Tool(description = "Use this to get the latest time series values for an entity. Use optional 'keys' to filter. Set 'useStrictDataTypes'=true to preserve original types.")
    public String getLatestTimeseries(
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String entityIdStr,
            @ToolParam(required = false, description = TELEMETRY_KEYS_DESCRIPTION) String keys,
            @ToolParam(required = false, description = STRICT_DATA_TYPES_DESCRIPTION) String useStrictDataTypes) {
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
        return JacksonUtil.toString(clientService.getClient().getLatestTimeseries(entityId, parseKeys(keys), Boolean.parseBoolean(useStrictDataTypes)));
    }

    @Tool(description = "Use this to get a range of time series values. Returns RAW data by default. " +
            "Aggregation: set agg=MIN|MAX|AVG|SUM|COUNT with interval (ms) or intervalType=MILLISECONDS|WEEK|WEEK_ISO|MONTH|QUARTER. " +
            "Global min/max: set interval=endTs-startTs+1, limit=1. " +
            "keys: comma-separated (e.g. temperature,humidity), aggregated per key. " +
            "limit: only for agg=NONE (raw). orderBy: ASC or DESC. " +
            "Example - global MAX for a day: agg=MAX, interval=86400001, limit=1. " +
            "Hourly AVG: agg=AVG, interval=3600000. " +
            "Raw latest 500: agg=NONE, limit=500, orderBy=DESC.")
    public String getTimeseries(
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String entityIdStr,
            @ToolParam(description = TELEMETRY_KEYS_DESCRIPTION) @NotBlank String keys,
            @ToolParam(required = false, description = "A long value representing the start timestamp of the time range in milliseconds, UTC. If not set 0 ts is used") @Positive String startTs,
            @ToolParam(required = false, description = "A long value representing the end timestamp of the time range in milliseconds, UTC. If not set, current ts is used") @Positive String endTs,
            @ToolParam(required = false, description = "A string value representing the type fo the interval. Allowed values: 'MILLISECONDS', 'WEEK', 'WEEK_ISO', 'MONTH', 'QUARTER'") String intervalType,
            @ToolParam(required = false, description = "A long value representing the aggregation interval range in milliseconds.") String interval,
            @ToolParam(required = false, description = "A string value representing the timezone that will be used to calculate exact timestamps for 'WEEK', 'WEEK_ISO', 'MONTH' and 'QUARTER' interval types.") String timeZone,
            @ToolParam(required = false, description = "Max number of data points to fetch. Only used when agg=NONE (raw mode).") String limit,
            @ToolParam(required = false, description = "A string value representing the aggregation function. If the interval is not specified, 'agg' parameter will use 'NONE' value. Allowed value: 'MIN', 'MAX', 'SUM', 'AVG', 'COUNT', 'NONE'") String agg,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String orderBy,
            @ToolParam(required = false, description = STRICT_DATA_TYPES_DESCRIPTION) String useStrictDataTypes) {
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
        Aggregation aggregation = agg != null ? Aggregation.valueOf(agg) : Aggregation.NONE;
        Long intervalInt = interval != null ? Long.parseLong(interval) : 0;
        Integer limitInt = limit != null ? Integer.parseInt(limit) : 100;
        IntervalType type = intervalType != null ? IntervalType.valueOf(intervalType) : null;
        return JacksonUtil.toString(clientService.getClient().getTimeseries(
                entityId,
                List.of(keys.split(",")),
                intervalInt,
                aggregation,
                type,
                timeZone,
                orderBy != null ? SortOrder.Direction.valueOf(orderBy) : SortOrder.Direction.ASC,
                parseLong(startTs, 0L),
                parseLong(endTs, System.currentTimeMillis()),
                limitInt,
                Boolean.parseBoolean(useStrictDataTypes)));
    }

    @Tool(description = "Use this to create or update device attributes. Provide device id, scope (SERVER_SCOPE or SHARED_SCOPE), and JSON key-value payload.")
    public String saveDeviceAttributes(
            @ToolParam(description = DEVICE_ID_PARAM_DESCRIPTION) @NotBlank String deviceId,
            @ToolParam(description = ATTRIBUTES_SCOPE_DESCRIPTION + " Allowable values: 'SERVER_SCOPE', 'SHARED_SCOPE'") @NotBlank String scope,
            @ToolParam(description = ATTRIBUTES_JSON_REQUEST_DESCRIPTION) @NotBlank String jsonBody) {
        boolean result = clientService.getClient().saveDeviceAttributes(new DeviceId(UUID.fromString(deviceId)), scope, JacksonUtil.toJsonNode(jsonBody));
        if (result) {
            return "{\"status\":\"Device attributes saved successfully\"}";
        }
        return "{\"status\":\"Failed to save device attributes\"}";
    }

    @Tool(description = "Use this to create or update attributes for any entity. Provide entity type/id, scope (SERVER_SCOPE or SHARED_SCOPE), and JSON key-value payload.")
    public String saveEntityAttributesV2(
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String entityIdStr,
            @ToolParam(description = ATTRIBUTES_SCOPE_DESCRIPTION + " Allowable values: 'SERVER_SCOPE', 'SHARED_SCOPE'") @NotBlank String scope,
            @ToolParam(description = ATTRIBUTES_JSON_REQUEST_DESCRIPTION) @NotBlank String jsonBody) {
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
        boolean result = clientService.getClient().saveEntityAttributesV2(entityId, scope, JacksonUtil.toJsonNode(jsonBody));
        if (result) {
            return "{\"status\":\"Entity attributes saved using V2 API\"}";
        }
        return "{\"status\":\"Failed to save attribute using V2 API\"}";
    }

    @Tool(description = "Use this to save time series data for an entity. Accepts JSON: simple {key:value}, with timestamp {ts:...,values:{...}}, or array format.")
    public String saveEntityTelemetry(
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String entityIdStr,
            @ToolParam(description = TELEMETRY_JSON_REQUEST_DESCRIPTION) @NotBlank String jsonBody) {
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
        boolean result = clientService.getClient().saveEntityTelemetry(entityId, "ANY", JacksonUtil.toJsonNode(jsonBody));
        if (result) {
            return "{\"status\":\"Telemetry submitted successfully\"}";
        }
        return "{\"status\":\"Failed to submit telemetry\"}";
    }

    @Tool(description = "Use this to save time series data with a TTL (Time to Live) for an entity. TTL only applies with Cassandra DB.")
    public String saveEntityTelemetryWithTTL(
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String entityIdStr,
            @ToolParam(description = "A  long value representing TTL (Time to Live) parameter.") @PositiveOrZero String ttl,
            @ToolParam(description = TELEMETRY_JSON_REQUEST_DESCRIPTION) @NotBlank String jsonBody) {
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
        boolean result = clientService.getClient().saveEntityTelemetryWithTTL(entityId, "ANY", parseLong(ttl, 0L), JacksonUtil.toJsonNode(jsonBody));
        if (result) {
            return "{\"status\":\"Telemetry with TTL submitted successfully\"}";
        }
        return "{\"status\":\"Failed to submit telemetry with TTL\"}";
    }

}
