package org.thingsboard.ai.mcp.server.service.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.alarm.AlarmTools;
import org.thingsboard.ai.mcp.server.util.JacksonUtil;
import org.thingsboard.client.ThingsboardClient;
import org.thingsboard.client.model.Alarm;
import org.thingsboard.client.model.AlarmInfo;
import org.thingsboard.client.model.AlarmSeverity;
import org.thingsboard.client.model.DeviceId;
import org.thingsboard.client.model.EntityType;
import org.thingsboard.client.model.PageDataAlarmInfo;
import org.thingsboard.client.model.PageDataEntitySubtype;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AlarmToolsTest {

    @InjectMocks
    private AlarmTools tools;

    @Mock
    private RestClientService clientService;

    @Mock
    private ThingsboardClient restClient;

    @BeforeEach
    void setup() {
        when(clientService.getClient()).thenReturn(restClient);
    }

    @Test
    void testFindInfoAlarmById() {
        String alarmId = UUID.randomUUID().toString();
        AlarmInfo alarmInfo = new AlarmInfo().type("default").originatorName("device-1");
        when(restClient.getAlarmInfoById(anyString())).thenReturn(alarmInfo);

        String result = tools.getAlarmInfoById(alarmId);

        verify(restClient).getAlarmInfoById(eq(alarmId));
        assertThat(result).isEqualTo(JacksonUtil.toString(alarmInfo));
    }

    @Test
    void testSaveAlarm_createNew() {
        String originatorId = UUID.randomUUID().toString();
        DeviceId originator = new DeviceId().id(UUID.fromString(originatorId)).entityType(EntityType.DEVICE);
        Alarm payload = new Alarm().type("Overheat").originator(originator);

        Alarm saved = JacksonUtil.fromString(
                "{\"id\":{\"id\":\"" + UUID.randomUUID() + "\",\"entityType\":\"ALARM\"},\"type\":\"Overheat\"}",
                Alarm.class);
        saved.originator(originator);

        when(restClient.saveAlarm(any(Alarm.class))).thenReturn(saved);

        String result = tools.saveAlarm(JacksonUtil.toString(payload));

        verify(restClient).saveAlarm(any(Alarm.class));
        assertThat(result).isEqualTo(JacksonUtil.toString(saved));
    }

    @Test
    void testSaveAlarm_updateExisting() {
        String alarmUuid = UUID.randomUUID().toString();
        Alarm payload = JacksonUtil.fromString(
                "{\"id\":{\"id\":\"" + alarmUuid + "\",\"entityType\":\"ALARM\"},\"type\":\"Overheat\"}",
                Alarm.class);

        when(restClient.saveAlarm(any(Alarm.class))).thenAnswer(inv -> inv.getArgument(0));

        String result = tools.saveAlarm(JacksonUtil.toString(payload));

        verify(restClient).saveAlarm(any(Alarm.class));
        assertThat(result).isEqualTo(JacksonUtil.toString(payload));
    }

    @Test
    void testDeleteAlarm_ok() {
        String id = UUID.randomUUID().toString();
        String res = tools.deleteAlarm(id);

        verify(restClient).deleteAlarm(eq(id));

        assertThat(res).contains("\"status\":\"OK\"");
        assertThat(res).contains(id);
    }

    @Test
    void testDeleteAlarm_error() {
        String id = UUID.randomUUID().toString();
        doThrow(new RuntimeException("boom")).when(restClient).deleteAlarm(anyString());

        String res = tools.deleteAlarm(id);

        assertThat(res).contains("\"status\":\"ERROR\"");
        assertThat(res).contains(id);
        assertThat(res).contains("boom");
    }

    @Test
    void testAckAlarm_ok() {
        String id = UUID.randomUUID().toString();
        AlarmInfo acked = new AlarmInfo().type("default");
        when(restClient.ackAlarm(anyString())).thenReturn(acked);

        String res = tools.ackAlarm(id);

        verify(restClient).ackAlarm(eq(id));
        assertThat(res).isEqualTo(JacksonUtil.toString(acked));
    }

    @Test
    void testAckAlarm_error() {
        String id = UUID.randomUUID().toString();
        doThrow(new RuntimeException("not found")).when(restClient).ackAlarm(anyString());
        String res = tools.ackAlarm(id);
        assertThat(res).contains("ERROR");
        assertThat(res).contains("not found");
        assertThat(res).contains(id);
    }

    @Test
    void testClearAlarm_ok() {
        String id = UUID.randomUUID().toString();
        AlarmInfo cleared = new AlarmInfo().type("default");
        when(restClient.clearAlarm(anyString())).thenReturn(cleared);

        String res = tools.clearAlarm(id);

        verify(restClient).clearAlarm(eq(id));
        assertThat(res).isEqualTo(JacksonUtil.toString(cleared));
    }

    @Test
    void testClearAlarm_error() {
        String id = UUID.randomUUID().toString();
        doThrow(new RuntimeException("already cleared")).when(restClient).clearAlarm(anyString());
        String res = tools.clearAlarm(id);
        assertThat(res).contains("ERROR");
        assertThat(res).contains("already cleared");
        assertThat(res).contains(id);
    }

    @Test
    void testFindAlarms_defaultPaging() throws Exception {
        String entityType = "DEVICE";
        String entityId = UUID.randomUUID().toString();
        List<AlarmInfo> alarms = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            alarms.add(new AlarmInfo().type("alarm-" + i));
        }
        PageDataAlarmInfo page = new PageDataAlarmInfo(1, (long) alarms.size(), false).data(alarms);
        when(restClient.getAlarmsByEntity(anyString(), anyString(), anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        String result = tools.getAlarms(entityType, entityId, null, null,
                "100", "0", null, null, null, "0", "0", false);

        verify(restClient).getAlarmsByEntity(
                eq(entityType), eq(entityId), eq(100), eq(0),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(0L), eq(0L), eq(false));
        assertThat(result).isEqualTo(JacksonUtil.toString(page));
    }

    @Test
    void testFindAlarms_withFiltersAndSorting() throws Exception {
        String entityType = "DEVICE";
        String entityId = UUID.randomUUID().toString();
        PageDataAlarmInfo emptyPage = new PageDataAlarmInfo(0, 0L, false).data(List.of());
        when(restClient.getAlarmsByEntity(anyString(), anyString(), anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(emptyPage);

        String result = tools.getAlarms(
                entityType,
                entityId,
                "ACTIVE",
                "ACTIVE_ACK",
                "25",
                "2",
                "temp",
                "createdTime",
                "ASC",
                "1000",
                "2000",
                true
        );

        verify(restClient).getAlarmsByEntity(
                eq(entityType), eq(entityId), eq(25), eq(2),
                eq("ACTIVE"), eq("ACTIVE_ACK"), isNull(), eq("temp"),
                eq("createdTime"), eq("ASC"), eq(1000L), eq(2000L), eq(true));
        assertThat(result).isEqualTo(JacksonUtil.toString(emptyPage));
    }

    @Test
    void testFindAllAlarms_defaults() throws Exception {
        PageDataAlarmInfo page = new PageDataAlarmInfo(1, 0L, false).data(List.of());
        when(restClient.getAllAlarms(anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        String result = tools.getAllAlarms(null, null, null, "100", "0", null, null, null, "0", "0", false);

        verify(restClient).getAllAlarms(
                eq(100), eq(0),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(0L), eq(0L), eq(false));
        assertThat(result).isEqualTo(JacksonUtil.toString(page));
    }

    @Test
    void testFindAllAlarms_withFilters() throws Exception {
        PageDataAlarmInfo page = new PageDataAlarmInfo(1, 0L, false).data(List.of());
        when(restClient.getAllAlarms(anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        String result = tools.getAllAlarms("CLEARED", "CLEARED_UNACK", "user-1",
                "10", "1", "temp", "endTs", "DESC", "10", "20", true);

        verify(restClient).getAllAlarms(
                eq(10), eq(1),
                eq("CLEARED"), eq("CLEARED_UNACK"), eq("user-1"), eq("temp"),
                eq("endTs"), eq("DESC"), eq(10L), eq(20L), eq(true));
        assertThat(result).isEqualTo(JacksonUtil.toString(page));
    }

    @Test
    void testFindHighestAlarmSeverity() {
        String entityType = "DEVICE";
        String entityId = UUID.randomUUID().toString();
        when(restClient.getHighestAlarmSeverity(anyString(), anyString(), any(), any(), any()))
                .thenReturn(AlarmSeverity.MINOR);

        String result = tools.getHighestAlarmSeverity(entityType, entityId, "CLEARED", "CLEARED_ACK");

        verify(restClient).getHighestAlarmSeverity(
                eq(entityType), eq(entityId), eq("CLEARED"), eq("CLEARED_ACK"), isNull());
        assertThat(result).isEqualTo(JacksonUtil.toString(AlarmSeverity.MINOR));
    }

    @Test
    void testHighestAlarmSeverity_emptyReturnsNullJson() {
        String entityType = "DEVICE";
        String entityId = UUID.randomUUID().toString();
        when(restClient.getHighestAlarmSeverity(anyString(), anyString(), any(), any(), any())).thenReturn(null);

        String result = tools.getHighestAlarmSeverity(entityType, entityId, null, null);
        assertThat(result).isEqualTo("null");
    }

    @Test
    void testFindAlarmTypes() throws Exception {
        PageDataEntitySubtype page = new PageDataEntitySubtype(1, 0L, false).data(List.of());
        when(restClient.getAlarmTypes(anyInt(), anyInt(), any(), any())).thenReturn(page);

        String result = tools.getAlarmTypes("50", "3", "abc", "DESC");

        verify(restClient).getAlarmTypes(eq(50), eq(3), eq("abc"), eq("DESC"));
        assertThat(result).isEqualTo(JacksonUtil.toString(page));
    }

}
