package org.thingsboard.ai.mcp.server.tools.alarm;

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
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;

import java.util.UUID;

import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ALARM_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ALARM_INFO_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.ai.mcp.server.util.ToolUtils.createPageLink;
import static org.thingsboard.ai.mcp.server.util.ToolUtils.createTimePageLink;

@Service
@RequiredArgsConstructor
public class AlarmTools implements McpTools {

    private static final String ALARM_SECURITY_CHECK = "If the user has the authority of 'Tenant Administrator', the server checks that the originator of alarm is owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the originator of alarm belongs to the customer. ";
    private static final String ALARM_QUERY_SEARCH_STATUS_DESCRIPTION = "A string value representing one of the AlarmSearchStatus enumeration value. Allowed values: 'ANY', 'ACTIVE', 'CLEARED', 'ACK', 'UNACK'";
    private static final String ALARM_QUERY_STATUS_DESCRIPTION = "A string value representing one of the AlarmStatus enumeration value. Allowed values: 'ACTIVE_UNACK', 'ACTIVE_ACK', 'CLEARED_UNACK', 'CLEARED_ACK'";
    private static final String ALARM_QUERY_ASSIGNEE_DESCRIPTION = "A string value representing the assignee user id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    private static final String ALARM_QUERY_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on of next alarm fields: type, severity or status";
    private static final String ALARM_QUERY_START_TIME_DESCRIPTION = "The start timestamp in milliseconds of the search time range over the Alarm class field: 'createdTime'.";
    private static final String ALARM_QUERY_END_TIME_DESCRIPTION = "The end timestamp in milliseconds of the search time range over the Alarm class field: 'createdTime'.";
    private static final String ALARM_QUERY_FETCH_ORIGINATOR_DESCRIPTION = "A boolean value to specify if the alarm originator name will be " +
            "filled in the AlarmInfo object  field: 'originatorName' or will returns as null.";

    private final RestClientService clientService;

    @Tool(description = "Get the Alarm object based on the provided alarm id. " + ALARM_SECURITY_CHECK)
    public String getAlarmById(@ToolParam(description = ALARM_ID_PARAM_DESCRIPTION) @NotBlank String alarmId) {
        return JacksonUtil.toString(clientService.getClient().getAlarmById(new AlarmId(UUID.fromString(alarmId))));
    }

    @Tool(description = "Get the Alarm info object based on the provided alarm id. " + ALARM_SECURITY_CHECK + ALARM_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getAlarmInfoById(@ToolParam(description = ALARM_ID_PARAM_DESCRIPTION) @NotBlank String alarmId) {
        return JacksonUtil.toString(clientService.getClient().getAlarmInfoById(new AlarmId(UUID.fromString(alarmId))));
    }

    @Tool(description = "Get a page of alarms for the selected entity. Specifying both parameters 'searchStatus' and 'status' at the same time will cause an error. " + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getAlarms(
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String entityId,
            @ToolParam(required = false, description = ALARM_QUERY_SEARCH_STATUS_DESCRIPTION) String searchStatus,
            @ToolParam(required = false, description = ALARM_QUERY_STATUS_DESCRIPTION) String status,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive int pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero int page,
            @ToolParam(required = false, description = ALARM_QUERY_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'startTs', 'endTs', 'ackTs', 'clearTs', 'severity', 'status'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder,
            @ToolParam(required = false, description = ALARM_QUERY_START_TIME_DESCRIPTION) Long startTs,
            @ToolParam(required = false, description = ALARM_QUERY_END_TIME_DESCRIPTION) Long endTs,
            @ToolParam(required = false, description = ALARM_QUERY_FETCH_ORIGINATOR_DESCRIPTION) Boolean fetchOriginator) throws ThingsboardException {
        AlarmSearchStatus alarmSearchStatus = searchStatus != null ? AlarmSearchStatus.valueOf(searchStatus) : null;
        AlarmStatus alarmStatus = status != null ? AlarmStatus.valueOf(status) : null;
        TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTs, endTs);
        return JacksonUtil.toString(clientService.getClient().getAlarms(EntityIdFactory.getByTypeAndId(entityType, entityId), alarmSearchStatus, alarmStatus, pageLink, fetchOriginator));
    }

    @Tool(description = "Get a page of alarms that belongs to the current user owner. " +
            "If the user has the authority of 'Tenant Administrator', the server returns alarms that belongs to the tenant of current user. " +
            "If the user has the authority of 'Customer User', the server returns alarms that belongs to the customer of current user. " +
            "Specifying both parameters 'searchStatus' and 'status' at the same time will cause an error. " + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getAllAlarms(
            @ToolParam(required = false, description = ALARM_QUERY_SEARCH_STATUS_DESCRIPTION) String searchStatus,
            @ToolParam(required = false, description = ALARM_QUERY_STATUS_DESCRIPTION) String status,
            @ToolParam(required = false, description = ALARM_QUERY_ASSIGNEE_DESCRIPTION) String assigneeId,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive int pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero int page,
            @ToolParam(required = false, description = ALARM_QUERY_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'startTs', 'endTs', 'ackTs', 'clearTs', 'severity', 'status'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder,
            @ToolParam(required = false, description = ALARM_QUERY_START_TIME_DESCRIPTION) Long startTs,
            @ToolParam(required = false, description = ALARM_QUERY_END_TIME_DESCRIPTION) Long endTs,
            @ToolParam(required = false, description = ALARM_QUERY_FETCH_ORIGINATOR_DESCRIPTION) Boolean fetchOriginator) throws ThingsboardException {
        AlarmSearchStatus alarmSearchStatus = searchStatus != null ? AlarmSearchStatus.valueOf(searchStatus) : null;
        AlarmStatus alarmStatus = status != null ? AlarmStatus.valueOf(status) : null;
        TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTs, endTs);
        return JacksonUtil.toString(clientService.getClient().getAllAlarms(alarmSearchStatus, alarmStatus, assigneeId, pageLink, fetchOriginator));
    }

    @Tool(description = "Get highest alarm severity by originator ('entityType' and 'entityId') and optional 'status' and 'searchStatus' filters and returns the highest AlarmSeverity(CRITICAL, MAJOR, MINOR, WARNING or INDETERMINATE)." +
            "Specifying both parameters 'searchStatus' and 'status' at the same time will cause an error. " + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getHighestAlarmSeverity(
            @ToolParam(description = ENTITY_TYPE_PARAM_DESCRIPTION) @NotBlank String entityType,
            @ToolParam(description = ENTITY_ID_PARAM_DESCRIPTION) @NotBlank String entityId,
            @ToolParam(required = false, description = ALARM_QUERY_SEARCH_STATUS_DESCRIPTION) String searchStatus,
            @ToolParam(required = false, description = "A string value representing one of the AlarmStatus enumeration value. Allowed values: 'ACTIVE_UNACK', 'ACTIVE_ACK', 'CLEARED_UNACK', 'CLEARED_ACK'") String status) {
        AlarmSearchStatus alarmSearchStatus = searchStatus != null ? AlarmSearchStatus.valueOf(searchStatus) : null;
        AlarmStatus alarmStatus = status != null ? AlarmStatus.valueOf(status) : null;
        return JacksonUtil.toString(clientService.getClient().getHighestAlarmSeverity(EntityIdFactory.getByTypeAndId(entityType, entityId), alarmSearchStatus, alarmStatus));
    }

    @Tool(description = "Get a set of unique alarm types based on alarms that are either owned by tenant or assigned to the customer which user is performing the request. " + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getAlarmTypes(
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive int pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero int page,
            @ToolParam(required = false, description = ALARM_QUERY_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, "type", sortOrder);
        return JacksonUtil.toString(clientService.getClient().getAlarmTypes(pageLink));
    }

}
