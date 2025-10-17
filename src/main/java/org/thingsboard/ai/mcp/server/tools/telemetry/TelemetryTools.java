package org.thingsboard.ai.mcp.server.tools.telemetry;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
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
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ATTRIBUTE_DATA_EXAMPLE;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.DEVICE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_GET_ATTRIBUTE_SCOPES;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_SAVE_ATTRIBUTE_SCOPES;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.LATEST_TS_NON_STRICT_DATA_EXAMPLE;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.LATEST_TS_STRICT_DATA_EXAMPLE;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.MARKDOWN_CODE_BLOCK_END;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.MARKDOWN_CODE_BLOCK_START;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.SAVE_ATTRIBUTES_REQUEST_PAYLOAD;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.SAVE_TIMESERIES_REQUEST_PAYLOAD;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.STRICT_DATA_TYPES_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.TELEMETRY_JSON_REQUEST_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.TELEMETRY_KEYS_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.ai.mcp.server.util.ToolUtils.parseLong;

@Service
@RequiredArgsConstructor
public class TelemetryTools implements McpTools {

    private final RestClientService clientService;

    @Tool(description = "Returns a set of unique attribute key names for the selected entity. " +
            "The response will include merged key names set for all attribute scopes:" +
            "\n\n * SERVER_SCOPE - supported for all entity types;" +
            "\n * CLIENT_SCOPE - supported for devices;" +
            "\n * SHARED_SCOPE - supported for devices. "
            + "\n\n" + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getAttributeKeys(
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String entityIdStr) {
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
        return JacksonUtil.toString(clientService.getClient().getAttributeKeys(entityId));
    }

    @Tool(description = "Returns a set of unique attribute key names for the selected entity and attributes scope: " +
            "\n\n * SERVER_SCOPE - supported for all entity types;" +
            "\n * CLIENT_SCOPE - supported for devices;" +
            "\n * SHARED_SCOPE - supported for devices. "
            + "\n\n" + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getAttributeKeysByScope(
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String entityIdStr,
            @ToolParam(description = ATTRIBUTES_SCOPE_DESCRIPTION + " Allowable values: 'SERVER_SCOPE', 'SHARED_SCOPE', 'CLIENT_SCOPE'") @NotBlank String scope) {
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
        return JacksonUtil.toString(clientService.getClient().getAttributeKeysByScope(entityId, scope));
    }

    @Tool(description = "Returns all attributes that belong to specified entity. Use optional 'keys' parameter to return specific attributes."
            + "\n Example of the result: \n\n"
            + MARKDOWN_CODE_BLOCK_START
            + ATTRIBUTE_DATA_EXAMPLE
            + MARKDOWN_CODE_BLOCK_END
            + "\n\n " + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getAttributes(
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String entityIdStr,
            @ToolParam(required = false, description = ATTRIBUTES_KEYS_DESCRIPTION) String keys) {
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
        return JacksonUtil.toString(clientService.getClient().getAttributeKvEntries(entityId, List.of(keys.split(","))));
    }

    @Tool(description = "Returns all attributes of a specified scope that belong to specified entity." +
            ENTITY_GET_ATTRIBUTE_SCOPES +
            "Use optional 'keys' parameter to return specific attributes."
            + "\n Example of the result: \n\n"
            + MARKDOWN_CODE_BLOCK_START
            + ATTRIBUTE_DATA_EXAMPLE
            + MARKDOWN_CODE_BLOCK_END
            + "\n\n " + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getAttributesByScope(
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String entityIdStr,
            @ToolParam(description = ATTRIBUTES_SCOPE_DESCRIPTION + " Allowable values: 'SERVER_SCOPE', 'SHARED_SCOPE', 'CLIENT_SCOPE'") @NotBlank String scope,
            @ToolParam(required = false, description = ATTRIBUTES_KEYS_DESCRIPTION) String keys) {
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
        return JacksonUtil.toString(clientService.getClient().getAttributesByScope(entityId, scope, List.of(keys.split(","))));
    }

    @Tool(description = "Returns a set of unique time series key names for the selected entity. " +
            "\n\n" + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getTimeseriesKeys(
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String entityIdStr) {
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
        return JacksonUtil.toString(clientService.getClient().getTimeseriesKeys(entityId));
    }

    @Tool(description = "Returns all time series that belong to specified entity. Use optional 'keys' parameter to return specific time series." +
            " The result is a JSON object. The format of the values depends on the 'useStrictDataTypes' parameter." +
            " By default, all time series values are converted to strings: \n\n"
            + MARKDOWN_CODE_BLOCK_START
            + LATEST_TS_NON_STRICT_DATA_EXAMPLE
            + MARKDOWN_CODE_BLOCK_END
            + "\n\n However, it is possible to request the values without conversion ('useStrictDataTypes'=true): \n\n"
            + MARKDOWN_CODE_BLOCK_START
            + LATEST_TS_STRICT_DATA_EXAMPLE
            + MARKDOWN_CODE_BLOCK_END
            + "\n\n " + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getLatestTimeseries(
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String entityIdStr,
            @ToolParam(required = false, description = TELEMETRY_KEYS_DESCRIPTION) String keys,
            @ToolParam(required = false, description = STRICT_DATA_TYPES_DESCRIPTION) String useStrictDataTypes) {
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
        return JacksonUtil.toString(clientService.getClient().getLatestTimeseries(entityId, List.of(keys.split(",")), Boolean.parseBoolean(useStrictDataTypes)));
    }

    @Tool(description =
            "Returns a range of time series values for specified entity.  By default returns RAW data points (no aggregation)." +

                    "### Aggregation: how to get MIN / MAX / AVG / SUM / COUNT\n" +
                    "- Set `agg` to one of: `MIN`, `MAX`, `AVG`, `SUM`, `COUNT`.\n" +
                    "- Provide an aggregation **interval** to enable aggregation:\n" +
                    "  - Use `interval` (milliseconds), or\n" +
                    "  - Use `intervalType` = `MILLISECONDS | WEEK | WEEK_ISO | MONTH | QUARTER` (optional `timeZone`).\n" +
                    "- **Global min/max over the whole range**: make a **single bucket** that covers the entire `[startTs, endTs]` window:\n" +
                    "  - Set `agg=MIN` (or `MAX`) and `interval = endTs - startTs + 1`.\n" +
                    "  - Set `limit=1` (optional).\n\n" +

                    "### Keys & ordering\n" +
                    "- `keys` = comma-separated telemetry keys (e.g., `temperature,humidity`).\n" +
                    "- Aggregation runs **per key** independently.\n" +
                    "- `orderBy` controls sort order of returned datapoints (use `ASC` or `DESC`).\n\n" +

                    "### Limits\n" +
                    "- `limit` is used **only when `agg=NONE`** (raw mode). Ignored for aggregated queries.\n\n" +

                    "### Examples\n" +
                    "1) **Global MAX temperature** for a day (one value):\n" +
                    "   - entityType=`DEVICE`\n" +
                    "   - entityId=`<deviceUUID>`\n" +
                    "   - keys=`temperature`\n" +
                    "   - startTs=`1719878400000`, endTs=`1719964800000`\n" +
                    "   - agg=`MAX`, interval=`86400001`, limit=`1`\n" +
                    "\n" +
                    "2) **Hourly AVG temperature**:\n" +
                    "   - agg=`AVG`, interval=`3600000`\n" +
                    "\n" +
                    "3) **Raw values** (no aggregation), latest 500 points descending:\n" +
                    "   - agg=`NONE` (or omit), limit=`500`, orderBy=`DESC`\n\n" +

                    INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getTimeseries(
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String entityIdStr,
            @ToolParam(description = TELEMETRY_KEYS_DESCRIPTION) @NotBlank String keys,
            @ToolParam(required = false, description = "A long value representing the start timestamp of the time range in milliseconds, UTC. If not set 0 ts is used") @Positive String startTs,
            @ToolParam(required = false, description = "A long value representing the end timestamp of the time range in milliseconds, UTC. If not set, current ts is used") @Positive String endTs,
            @ToolParam(required = false, description = "A string value representing the type fo the interval. Allowed values: 'MILLISECONDS', 'WEEK', 'WEEK_ISO', 'MONTH', 'QUARTER'") String intervalType,
            @ToolParam(required = false, description = "A long value representing the aggregation interval range in milliseconds.") String interval,
            @ToolParam(required = false, description = "A string value representing the timezone that will be used to calculate exact timestamps for 'WEEK', 'WEEK_ISO', 'MONTH' and 'QUARTER' interval types.") String timeZone,
            @ToolParam(required = false, description = "An integer value that represents a max number of time series data points to fetch. This parameter is used only in the case if 'agg' parameter is set to 'NONE'. ") String limit,
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

    @Tool(description = "Creates or updates the device attributes based on device id and specified attribute scope. " +
            SAVE_ATTRIBUTES_REQUEST_PAYLOAD
            + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
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

    @Tool(description = "Creates or updates the entity attributes based on Entity Id and the specified attribute scope. " +
            ENTITY_SAVE_ATTRIBUTE_SCOPES +
            SAVE_ATTRIBUTES_REQUEST_PAYLOAD
            + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String saveEntityAttributesV1(
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String entityIdStr,
            @ToolParam(description = ATTRIBUTES_SCOPE_DESCRIPTION + " Allowable values: 'SERVER_SCOPE', 'SHARED_SCOPE'") @NotBlank String scope,
            @ToolParam(description = ATTRIBUTES_JSON_REQUEST_DESCRIPTION) @NotBlank String jsonBody) {
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
        boolean result = clientService.getClient().saveEntityAttributesV1(entityId, scope, JacksonUtil.toJsonNode(jsonBody));
        if (result) {
            return "{\"status\":\"Entity attributes saved using V1 API\"}";
        }
        return "{\"status\":\"Failed to save attribute using V1 API\"}";
    }

    @Tool(description = "Creates or updates the entity attributes based on Entity Id and the specified attribute scope. " +
            ENTITY_SAVE_ATTRIBUTE_SCOPES +
            SAVE_ATTRIBUTES_REQUEST_PAYLOAD
            + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
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

    @Tool(description = "Creates or updates the entity time series data based on the Entity Id and request payload." +
            SAVE_TIMESERIES_REQUEST_PAYLOAD +
            "\n\n The scope parameter is not used in the API call implementation but should be specified whatever value because it is used as a path variable. "
            + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
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

    @Tool(description = "Creates or updates the entity time series data based on the Entity Id and request payload." +
            SAVE_TIMESERIES_REQUEST_PAYLOAD +
            "\n\n The scope parameter is not used in the API call implementation but should be specified whatever value because it is used as a path variable. "
            + "\n\nThe ttl parameter takes affect only in case of Cassandra DB."
            + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
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
