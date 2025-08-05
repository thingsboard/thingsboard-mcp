package org.thingsboard.ai.mcp.server.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;

import java.util.UUID;

@Service
public class AlarmTools {

    @Autowired
    private RestClientService clientService;

    /**
     * Retrieves an alarm by its ID.
     *
     * @param alarmId the unique identifier of the alarm
     * @return JSON string representing the alarm object
     */
    @Tool(description = "Get alarm by ID")
    public String getAlarmById(String alarmId) {
        return JacksonUtil.toString(clientService.getClient().getAlarmById(new AlarmId(UUID.fromString(alarmId))));
    }

    /**
     * Retrieves alarms associated with a given entity.
     *
     * @param entityType      the type of the entity (e.g., DEVICE, ASSET)
     * @param entityId        the unique identifier of the entity
     * @param status          optional filter for alarm status (e.g., ACTIVE_ACK, CLEARED_UNACK)
     * @param searchStatus    optional filter for alarm search status (e.g., ANY, ACTIVE, CLEARED)
     * @param pageSize        number of alarms to return per page
     * @param page            page number for pagination
     * @param startTs         start timestamp for filtering alarms
     * @param endTs           end timestamp for filtering alarms
     * @param sortOrder       optional sort order (e.g., ASC, DESC)
     * @param fetchOriginator flag indicating whether to include originator details
     * @return JSON string containing a paginated list of alarms
     */
    @Tool(description = "Get alarms")
    public String getAlarms(String entityType, String entityId, String status, String searchStatus, int pageSize, int page, long startTs, long endTs, String sortOrder, boolean fetchOriginator) {
        AlarmSearchStatus alarmSearchStatus = searchStatus != null ? AlarmSearchStatus.valueOf(searchStatus) : null;
        AlarmStatus alarmStatus = status != null ? AlarmStatus.valueOf(status) : null;
        TimePageLink pageLink = new TimePageLink(pageSize, page, null, sortOrder != null ? new SortOrder(sortOrder) : null, startTs, endTs);
        return JacksonUtil.toString(clientService.getClient().getAlarms(EntityIdFactory.getByTypeAndId(entityType, entityId), alarmSearchStatus, alarmStatus, pageLink, fetchOriginator));
    }

    /**
     * Retrieves the highest alarm severity for the specified entity.
     *
     * @param entityType   the type of the entity (e.g., DEVICE, ASSET)
     * @param entityId     the unique identifier of the entity
     * @param searchStatus optional filter for alarm search status
     * @param status       optional filter for alarm status
     * @return JSON string representing the highest severity level or null if none exist
     */
    @Tool(description = "Get highest alarm severity")
    public String getHighestAlarmSeverity(String entityType, String entityId, String searchStatus, String status) {
        AlarmSearchStatus alarmSearchStatus = searchStatus != null ? AlarmSearchStatus.valueOf(searchStatus) : null;
        AlarmStatus alarmStatus = status != null ? AlarmStatus.valueOf(status) : null;
        return JacksonUtil.toString(clientService.getClient().getHighestAlarmSeverity(EntityIdFactory.getByTypeAndId(entityType, entityId), alarmSearchStatus, alarmStatus));
    }

    /**
     * Retrieves detailed alarm information by alarm ID.
     *
     * @param alarmId the unique identifier of the alarm
     * @return JSON string representing extended alarm information
     */
    @Tool(description = "Get alarm info by ID")
    public String getAlarmInfoById(String alarmId) {
        return JacksonUtil.toString(clientService.getClient().getAlarmInfoById(new AlarmId(UUID.fromString(alarmId))));
    }

    /**
     * Retrieves all available alarm types for the current tenant.
     *
     * @param pageSize number of types to return per page
     * @param page     page number for pagination
     * @return JSON string representing a list of alarm types
     */
    @Tool(description = "Get alarm types")
    public String getAlarmTypes(int pageSize, int page) {
        PageLink pageLink = new PageLink(pageSize, page);
        return JacksonUtil.toString(clientService.getClient().getAlarmTypes(pageLink));
    }
}