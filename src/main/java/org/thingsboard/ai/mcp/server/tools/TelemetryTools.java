package org.thingsboard.ai.mcp.server.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.SortOrder;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.ATTRIBUTE_DATA_EXAMPLE;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.ENTITY_GET_ATTRIBUTE_SCOPES;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.ENTITY_SAVE_ATTRIBUTE_SCOPES;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.LATEST_TS_NON_STRICT_DATA_EXAMPLE;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.LATEST_TS_STRICT_DATA_EXAMPLE;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.MARKDOWN_CODE_BLOCK_END;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.MARKDOWN_CODE_BLOCK_START;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.SAVE_ATTRIBUTES_REQUEST_PAYLOAD;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.SAVE_TIMESERIES_REQUEST_PAYLOAD;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.TS_STRICT_DATA_EXAMPLE;

@Service
public class TelemetryTools {

    @Autowired
    private RestClientService clientService;

    @Tool(description = "Returns a set of unique attribute key names for the selected entity. " +
            "The response will include merged key names set for all attribute scopes:" +
            "\n\n * SERVER_SCOPE - supported for all entity types;" +
            "\n * CLIENT_SCOPE - supported for devices;" +
            "\n * SHARED_SCOPE - supported for devices. "
            + "\n\n" + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getAttributeKeys(String deviceId) {
        List<String> keys = clientService.getClient().getAttributeKeys(new DeviceId(UUID.fromString(deviceId)));
        return JacksonUtil.toString(keys);
    }

    @Tool(description = "Returns a set of unique attribute key names for the selected entity and attributes scope: " +
            "\n\n * SERVER_SCOPE - supported for all entity types;" +
            "\n * CLIENT_SCOPE - supported for devices;" +
            "\n * SHARED_SCOPE - supported for devices. "
            + "\n\n" + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getAttributeKeysByScope(String deviceId, String scope) {
        List<String> keys = clientService.getClient().getAttributeKeysByScope(new DeviceId(UUID.fromString(deviceId)), scope);
        return JacksonUtil.toString(keys);
    }

    @Tool(description = "Returns all attributes that belong to specified entity. Use optional 'keys' parameter to return specific attributes."
            + "\n Example of the result: \n\n"
            + MARKDOWN_CODE_BLOCK_START
            + ATTRIBUTE_DATA_EXAMPLE
            + MARKDOWN_CODE_BLOCK_END
            + "\n\n " + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getAttributes(String deviceId, String keys) {
        List<AttributeKvEntry> values = clientService.getClient().getAttributeKvEntries(
                new DeviceId(UUID.fromString(deviceId)),
                List.of(keys.split(",")));
        return JacksonUtil.toString(values);
    }

    @Tool(description = "Returns all attributes of a specified scope that belong to specified entity." +
            ENTITY_GET_ATTRIBUTE_SCOPES +
            "Use optional 'keys' parameter to return specific attributes."
            + "\n Example of the result: \n\n"
            + MARKDOWN_CODE_BLOCK_START
            + ATTRIBUTE_DATA_EXAMPLE
            + MARKDOWN_CODE_BLOCK_END
            + "\n\n " + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getAttributesByScope(String deviceId, String scope, String keys) {
        List<AttributeKvEntry> values = clientService.getClient().getAttributesByScope(
                new DeviceId(UUID.fromString(deviceId)),
                scope,
                List.of(keys.split(",")));
        return JacksonUtil.toString(values);
    }

    @Tool(description = "Returns a set of unique time series key names for the selected entity. " +
            "\n\n" + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getTimeseriesKeys(String deviceId) {
        List<String> keys = clientService.getClient().getTimeseriesKeys(new DeviceId(UUID.fromString(deviceId)));
        return JacksonUtil.toString(keys);
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
    public String getLatestTimeseries(String deviceId, String keys) {
        List<TsKvEntry> values = clientService.getClient().getLatestTimeseries(
                new DeviceId(UUID.fromString(deviceId)),
                List.of(keys.split(",")));
        return JacksonUtil.toString(values);
    }

    @Tool(description = "Returns a range of time series values for specified entity. " +
            "Returns not aggregated data by default. " +
            "Use aggregation function ('agg') and aggregation interval ('interval') to enable aggregation of the results on the database / server side. " +
            "The aggregation is generally more efficient then fetching all records. \n\n"
            + MARKDOWN_CODE_BLOCK_START
            + TS_STRICT_DATA_EXAMPLE
            + MARKDOWN_CODE_BLOCK_END
            + "\n\n" + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getTimeseries(String deviceId, String keys, long interval, String agg, String sortOrder, long startTs, long endTs, int limit, boolean useStrictDataTypes) {
        var result = clientService.getClient().getTimeseries(
                new DeviceId(UUID.fromString(deviceId)),
                List.of(keys.split(",")),
                interval,
                Aggregation.valueOf(agg),
                SortOrder.Direction.valueOf(sortOrder),
                startTs,
                endTs,
                limit,
                useStrictDataTypes);
        return JacksonUtil.toString(result);
    }

    @Tool(description = "Creates or updates the device attributes based on device id and specified attribute scope. " +
            SAVE_ATTRIBUTES_REQUEST_PAYLOAD
            + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String saveDeviceAttributes(String deviceId, String scope, String jsonBody) {
        clientService.getClient().saveDeviceAttributes(new DeviceId(UUID.fromString(deviceId)), scope, JacksonUtil.toJsonNode(jsonBody));
        return "{\"status\":\"Device attributes saved successfully\"}";
    }

    @Tool(description = "Creates or updates the entity attributes based on Entity Id and the specified attribute scope. " +
            ENTITY_SAVE_ATTRIBUTE_SCOPES +
            SAVE_ATTRIBUTES_REQUEST_PAYLOAD
            + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String saveEntityAttributesV1(String entityType, String entityId, String scope, String jsonBody) {
        EntityId entity = EntityIdFactory.getByTypeAndId(entityType, entityId);
        clientService.getClient().saveEntityAttributesV1(entity, scope, JacksonUtil.toJsonNode(jsonBody));
        return "{\"status\":\"Entity attributes saved using V1 API\"}";
    }

    @Tool(description = "Creates or updates the entity attributes based on Entity Id and the specified attribute scope. " +
            ENTITY_SAVE_ATTRIBUTE_SCOPES +
            SAVE_ATTRIBUTES_REQUEST_PAYLOAD
            + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String saveEntityAttributesV2(String entityType, String entityId, String scope, String jsonBody) {
        EntityId entity = EntityIdFactory.getByTypeAndId(entityType, entityId);
        clientService.getClient().saveEntityAttributesV2(entity, scope, JacksonUtil.toJsonNode(jsonBody));
        return "{\"status\":\"Entity attributes saved using V2 API\"}";
    }

    @Tool(description = "Creates or updates the entity time series data based on the Entity Id and request payload." +
            SAVE_TIMESERIES_REQUEST_PAYLOAD +
            "\n\n The scope parameter is not used in the API call implementation but should be specified whatever value because it is used as a path variable. "
            + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String saveEntityTelemetry(String entityType, String entityId, String jsonBody) {
        EntityId entity = EntityIdFactory.getByTypeAndId(entityType, entityId);
        clientService.getClient().saveEntityTelemetry(entity, "ANY", JacksonUtil.toJsonNode(jsonBody));
        return "{\"status\":\"Telemetry submitted successfully\"}";
    }

    @Tool(description = "Creates or updates the entity time series data based on the Entity Id and request payload." +
            SAVE_TIMESERIES_REQUEST_PAYLOAD +
            "\n\n The scope parameter is not used in the API call implementation but should be specified whatever value because it is used as a path variable. "
            + "\n\nThe ttl parameter takes affect only in case of Cassandra DB."
            + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String saveEntityTelemetryWithTTL(String entityType, String entityId, long ttl, String jsonBody) {
        EntityId entity = EntityIdFactory.getByTypeAndId(entityType, entityId);
        clientService.getClient().saveEntityTelemetryWithTTL(entity, "ANY", ttl, JacksonUtil.toJsonNode(jsonBody));
        return "{\"status\":\"Telemetry with TTL submitted successfully\"}";
    }

}
