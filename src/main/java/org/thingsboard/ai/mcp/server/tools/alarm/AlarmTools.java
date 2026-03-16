package org.thingsboard.ai.mcp.server.tools.alarm;

import jakarta.validation.Valid;
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
import org.thingsboard.ai.mcp.server.util.JacksonUtil;
import org.thingsboard.client.model.Alarm;

import java.util.HashMap;
import java.util.Map;

import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ALARM_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.util.ToolUtils.parseIntOrDefault;
import static org.thingsboard.ai.mcp.server.util.ToolUtils.parseLong;
import static org.thingsboard.ai.mcp.server.util.ToolUtils.sanitizeStringParam;

@Service
@RequiredArgsConstructor
@ToolGroup("alarm")
public class AlarmTools implements McpTools {

    private static final String ALARM_QUERY_SEARCH_STATUS_DESCRIPTION = "A string value representing one of the AlarmSearchStatus enumeration value. Allowed values: 'ANY', 'ACTIVE', 'CLEARED', 'ACK', 'UNACK'";
    private static final String ALARM_QUERY_STATUS_DESCRIPTION = "A string value representing one of the AlarmStatus enumeration value. Allowed values: 'ACTIVE_UNACK', 'ACTIVE_ACK', 'CLEARED_UNACK', 'CLEARED_ACK'";
    private static final String ALARM_QUERY_ASSIGNEE_DESCRIPTION = "A string value representing the assignee user id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    private static final String ALARM_QUERY_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on of next alarm fields: type, severity or status";
    private static final String ALARM_QUERY_START_TIME_DESCRIPTION = "Start of time range (epoch ms) over 'createdTime'.";
    private static final String ALARM_QUERY_END_TIME_DESCRIPTION = "End of time range (epoch ms) over 'createdTime'.";
    private static final String ALARM_QUERY_FETCH_ORIGINATOR_DESCRIPTION = "If true, includes originator name in response.";

    private static final String ALARM_JSON_EXAMPLE = """
            {
              "originator": {"id": "<deviceId>", "entityType": "DEVICE"},
              "type": "High Temperature Alarm",
              "severity": "CRITICAL",
              "propagate": true,
              "details": {"message": "Temperature exceeded threshold"}
            }""";

    private final RestClientService clientService;

    @Tool(description = "Use this to create or update an alarm. " +
            "To create: provide 'originator', 'type', 'severity'. Omit 'id'. " +
            "To update: include 'id'. " +
            "Alarms are deduplicated by originator + type: duplicates update the existing active alarm. " +
            "After clearing (clearAlarm), a new one with the same type can be created.")
    public String saveAlarm(
            @ToolParam(description = "JSON alarm object. Required for create: 'originator' ({id, entityType}), 'type', 'severity' (CRITICAL|MAJOR|MINOR|WARNING|INDETERMINATE). " +
                    "Optional: 'propagate', 'details', 'assigneeId'. Include 'id' to update. Example: " + ALARM_JSON_EXAMPLE)
            @NotBlank @Valid String alarmJson) {
        Alarm alarm = JacksonUtil.fromString(alarmJson, Alarm.class);
        return JacksonUtil.toString(clientService.getClient().saveAlarm(alarm));
    }

    @Tool(description = "Use this to permanently delete an alarm by its id.")
    public String deleteAlarm(@ToolParam(description = ALARM_ID_PARAM_DESCRIPTION) @NotBlank String alarmIdStr) {
        try {
            clientService.getClient().deleteAlarm(alarmIdStr);
            return "{\"status\":\"OK\",\"id\":\"" + alarmIdStr + "\"}";
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("id", alarmIdStr);
            err.put("message", e.getMessage());
            return JacksonUtil.toString(err);
        }
    }

    @Tool(description = "Use this to acknowledge an alarm. Sets 'ack_ts' and triggers ALARM_ACK rule chain event.")
    public String ackAlarm(
            @ToolParam(description = ALARM_ID_PARAM_DESCRIPTION) @NotBlank String alarmIdStr) {
        try {
            return JacksonUtil.toString(clientService.getClient().ackAlarm(alarmIdStr));
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("id", alarmIdStr);
            err.put("message", e.getMessage());
            return JacksonUtil.toString(err);
        }
    }

    @Tool(description = "Use this to clear an alarm. Sets 'clear_ts' and triggers ALARM_CLEAR rule chain event.")
    public String clearAlarm(
            @ToolParam(description = ALARM_ID_PARAM_DESCRIPTION) @NotBlank String alarmIdStr) {
        try {
            return JacksonUtil.toString(clientService.getClient().clearAlarm(alarmIdStr));
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("id", alarmIdStr);
            err.put("message", e.getMessage());
            return JacksonUtil.toString(err);
        }
    }

    @Tool(description = "Use this to get alarm details by id. Returns AlarmInfo including originator name.")
    public String getAlarmInfoById(@ToolParam(description = ALARM_ID_PARAM_DESCRIPTION) @NotBlank String alarmId) {
        return JacksonUtil.toString(clientService.getClient().getAlarmInfoById(alarmId));
    }

    @Tool(description = "Use this to get a paginated list of alarms for a specific entity. Filter by searchStatus or status (not both). Returns PageData of AlarmInfo.")
    public String getAlarms(
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String entityId,
            @ToolParam(required = false, description = ALARM_QUERY_SEARCH_STATUS_DESCRIPTION) String searchStatus,
            @ToolParam(required = false, description = ALARM_QUERY_STATUS_DESCRIPTION) String status,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = ALARM_QUERY_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'startTs', 'endTs', 'ackTs', 'clearTs', 'severity', 'status'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder,
            @ToolParam(required = false, description = ALARM_QUERY_START_TIME_DESCRIPTION) String startTs,
            @ToolParam(required = false, description = ALARM_QUERY_END_TIME_DESCRIPTION) String endTs,
            @ToolParam(required = false, description = ALARM_QUERY_FETCH_ORIGINATOR_DESCRIPTION) Boolean fetchOriginator) {
        return JacksonUtil.toString(clientService.getClient().getAlarmsByEntity(
                entityType,
                entityId,
                parseIntOrDefault(pageSize, 10),
                parseIntOrDefault(page, 0),
                sanitizeStringParam(searchStatus),
                sanitizeStringParam(status),
                null,
                sanitizeStringParam(textSearch),
                sanitizeStringParam(sortProperty),
                sanitizeStringParam(sortOrder),
                parseLong(startTs),
                parseLong(endTs),
                fetchOriginator));
    }

    @Tool(description = "Use this to get all alarms visible to the current user (tenant-scoped or customer-scoped). Filter by searchStatus or status (not both). Returns PageData of AlarmInfo.")
    public String getAllAlarms(
            @ToolParam(required = false, description = ALARM_QUERY_SEARCH_STATUS_DESCRIPTION) String searchStatus,
            @ToolParam(required = false, description = ALARM_QUERY_STATUS_DESCRIPTION) String status,
            @ToolParam(required = false, description = ALARM_QUERY_ASSIGNEE_DESCRIPTION) String assigneeId,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = ALARM_QUERY_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'startTs', 'endTs', 'ackTs', 'clearTs', 'severity', 'status'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder,
            @ToolParam(required = false, description = ALARM_QUERY_START_TIME_DESCRIPTION) String startTs,
            @ToolParam(required = false, description = ALARM_QUERY_END_TIME_DESCRIPTION) String endTs,
            @ToolParam(required = false, description = ALARM_QUERY_FETCH_ORIGINATOR_DESCRIPTION) Boolean fetchOriginator) {
        return JacksonUtil.toString(clientService.getClient().getAllAlarms(
                parseIntOrDefault(pageSize, 10),
                parseIntOrDefault(page, 0),
                sanitizeStringParam(searchStatus),
                sanitizeStringParam(status),
                sanitizeStringParam(assigneeId),
                sanitizeStringParam(textSearch),
                sanitizeStringParam(sortProperty),
                sanitizeStringParam(sortOrder),
                parseLong(startTs),
                parseLong(endTs),
                fetchOriginator));
    }

    @Tool(description = "Use this to get the highest alarm severity for an entity. Returns: CRITICAL, MAJOR, MINOR, WARNING, or INDETERMINATE. Filter by searchStatus or status (not both).")
    public String getHighestAlarmSeverity(
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String entityId,
            @ToolParam(required = false, description = ALARM_QUERY_SEARCH_STATUS_DESCRIPTION) String searchStatus,
            @ToolParam(required = false, description = "A string value representing one of the AlarmStatus enumeration value. Allowed values: 'ACTIVE_UNACK', 'ACTIVE_ACK', 'CLEARED_UNACK', 'CLEARED_ACK'") String status) {
        return JacksonUtil.toString(clientService.getClient().getHighestAlarmSeverity(
                entityType,
                entityId,
                sanitizeStringParam(searchStatus),
                sanitizeStringParam(status),
                null));
    }

    @Tool(description = "Use this to list unique alarm type names visible to the current user.")
    public String getAlarmTypes(
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = ALARM_QUERY_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        return JacksonUtil.toString(clientService.getClient().getAlarmTypes(
                parseIntOrDefault(pageSize, 10),
                parseIntOrDefault(page, 0),
                sanitizeStringParam(textSearch),
                sanitizeStringParam(sortOrder)));
    }

}
