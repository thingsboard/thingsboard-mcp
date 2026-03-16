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
import org.thingsboard.ai.mcp.server.util.JsonUtils;

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

    private static String keysToCommaString(String keys) {
        if (keys == null || keys.isBlank()) {
            return null;
        }
        return keys.trim();
    }

    @Tool(description = "Use this to get all attribute key names for an entity (merged across SERVER_SCOPE, CLIENT_SCOPE, SHARED_SCOPE).")
    public String getAttributeKeys(
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String entityIdStr) {
        return JsonUtils.toString(clientService.getClient().getAttributeKeys(entityType, entityIdStr));
    }

    @Tool(description = "Use this to get attribute key names for an entity filtered by scope (SERVER_SCOPE, CLIENT_SCOPE, SHARED_SCOPE).")
    public String getAttributeKeysByScope(
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String entityIdStr,
            @ToolParam(description = ATTRIBUTES_SCOPE_DESCRIPTION + " Allowable values: 'SERVER_SCOPE', 'SHARED_SCOPE', 'CLIENT_SCOPE'") @NotBlank String scope) {
        return JsonUtils.toString(clientService.getClient().getAttributeKeysByScope(entityType, entityIdStr, scope));
    }

    @Tool(description = "Use this to get all attributes for an entity. Use optional 'keys' to return specific attributes.")
    public String getAttributes(
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String entityIdStr,
            @ToolParam(required = false, description = ATTRIBUTES_KEYS_DESCRIPTION) String keys) {
        return JsonUtils.toString(clientService.getClient().getAttributes(entityType, entityIdStr, keysToCommaString(keys), null));
    }

    @Tool(description = "Use this to get attributes for an entity filtered by scope. Scopes: SERVER_SCOPE, SHARED_SCOPE, CLIENT_SCOPE (devices only). Use optional 'keys' to filter.")
    public String getAttributesByScope(
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String entityIdStr,
            @ToolParam(description = ATTRIBUTES_SCOPE_DESCRIPTION + " Allowable values: 'SERVER_SCOPE', 'SHARED_SCOPE', 'CLIENT_SCOPE'") @NotBlank String scope,
            @ToolParam(required = false, description = ATTRIBUTES_KEYS_DESCRIPTION) String keys) {
        return JsonUtils.toString(clientService.getClient().getAttributesByScope(entityType, entityIdStr, scope, keysToCommaString(keys), null));
    }

    @Tool(description = "Use this to get all time series key names for an entity.")
    public String getTimeseriesKeys(
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String entityIdStr) {
        return JsonUtils.toString(clientService.getClient().getTimeseriesKeys(entityType, entityIdStr));
    }

    @Tool(description = "Use this to get the latest time series values for an entity. Use optional 'keys' to filter. Set 'useStrictDataTypes'=true to preserve original types.")
    public String getLatestTimeseries(
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String entityIdStr,
            @ToolParam(required = false, description = TELEMETRY_KEYS_DESCRIPTION) String keys,
            @ToolParam(required = false, description = STRICT_DATA_TYPES_DESCRIPTION) String useStrictDataTypes) {
        return JsonUtils.toString(clientService.getClient().getLatestTimeseries(
                entityType, entityIdStr, keysToCommaString(keys), Boolean.parseBoolean(useStrictDataTypes), null));
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
        String aggregation = agg != null ? agg.trim().toUpperCase() : "NONE";
        Long intervalLong = interval != null ? Long.parseLong(interval) : 0L;
        String limitStr = limit != null ? limit.trim() : "100";
        String order = orderBy != null ? orderBy.trim().toUpperCase() : "ASC";
        return JsonUtils.toString(clientService.getClient().getTimeseriesHistory(
                entityType,
                entityIdStr,
                parseLong(startTs, 0L),
                parseLong(endTs, System.currentTimeMillis()),
                keys.trim(),
                intervalType,
                intervalLong,
                timeZone,
                limitStr,
                aggregation,
                order,
                Boolean.parseBoolean(useStrictDataTypes),
                null));
    }

    @Tool(description = "Use this to create or update device attributes. Provide device id, scope (SERVER_SCOPE or SHARED_SCOPE), and JSON key-value payload.")
    public String saveDeviceAttributes(
            @ToolParam(description = DEVICE_ID_PARAM_DESCRIPTION) @NotBlank String deviceId,
            @ToolParam(description = ATTRIBUTES_SCOPE_DESCRIPTION + " Allowable values: 'SERVER_SCOPE', 'SHARED_SCOPE'") @NotBlank String scope,
            @ToolParam(description = ATTRIBUTES_JSON_REQUEST_DESCRIPTION) @NotBlank String jsonBody) {
        String result = clientService.getClient().saveDeviceAttributes(deviceId, scope, jsonBody);
        if (result != null) {
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
        String result = clientService.getClient().saveEntityAttributesV2(entityType, entityIdStr, scope, jsonBody);
        if (result != null) {
            return "{\"status\":\"Entity attributes saved using V2 API\"}";
        }
        return "{\"status\":\"Failed to save attribute using V2 API\"}";
    }

    @Tool(description = "Use this to save time series data for an entity. Accepts JSON: simple {key:value}, with timestamp {ts:...,values:{...}}, or array format.")
    public String saveEntityTelemetry(
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String entityIdStr,
            @ToolParam(description = TELEMETRY_JSON_REQUEST_DESCRIPTION) @NotBlank String jsonBody) {
        String result = clientService.getClient().saveEntityTelemetry(entityType, entityIdStr, "ANY", jsonBody);
        if (result != null) {
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
        String result = clientService.getClient().saveEntityTelemetryWithTTL(entityType, entityIdStr, "ANY", parseLong(ttl, 0L), jsonBody);
        if (result != null) {
            return "{\"status\":\"Telemetry with TTL submitted successfully\"}";
        }
        return "{\"status\":\"Failed to submit telemetry with TTL\"}";
    }

}
