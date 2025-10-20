package org.thingsboard.ai.mcp.server.service.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.ai.mcp.server.rest.RestClient;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.alarm.AlarmTools;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    private RestClient restClient;

    protected TenantId tenantId;

    @BeforeEach
    void setup() {
        when(clientService.getClient()).thenReturn(restClient);
        tenantId = TenantId.fromUUID(UUID.randomUUID());
    }

    @Test
    void testFindAlarmById() {
        UUID alarmId = UUID.randomUUID();
        Alarm alarm = createAlarm(alarmId, new DeviceId(UUID.randomUUID()));
        when(restClient.getAlarmById(any(AlarmId.class))).thenReturn(Optional.of(alarm));

        String result = tools.getAlarmById(alarmId.toString());

        ArgumentCaptor<AlarmId> idCap = ArgumentCaptor.forClass(AlarmId.class);
        verify(restClient).getAlarmById(idCap.capture());
        assertThat(idCap.getValue().getId()).isEqualTo(alarmId);

        assertThat(result).isEqualTo(JacksonUtil.toString(alarm));
    }

    @Test
    void testFindInfoAlarmById() {
        UUID alarmId = UUID.randomUUID();
        AlarmInfo alarmInfo = createAlarmInfo(alarmId, new DeviceId(UUID.randomUUID()));
        when(restClient.getAlarmInfoById(any(AlarmId.class))).thenReturn(Optional.of(alarmInfo));

        String result = tools.getAlarmInfoById(alarmId.toString());

        ArgumentCaptor<AlarmId> idCap = ArgumentCaptor.forClass(AlarmId.class);
        verify(restClient).getAlarmInfoById(idCap.capture());
        assertThat(idCap.getValue().getId()).isEqualTo(alarmId);

        assertThat(result).isEqualTo(JacksonUtil.toString(alarmInfo));
    }

    @Test
    void testSaveAlarm_createNew() {
        Alarm payload = new Alarm();
        payload.setTenantId(tenantId);
        payload.setType("Overheat");
        DeviceId originator = new DeviceId(UUID.randomUUID());
        payload.setOriginator(originator);

        Alarm saved = new Alarm();
        saved.setId(new AlarmId(UUID.randomUUID()));
        saved.setTenantId(tenantId);
        saved.setType(payload.getType());
        saved.setOriginator(originator);

        when(restClient.saveAlarm(any(Alarm.class))).thenReturn(saved);

        String json = JacksonUtil.toString(payload);
        String result = tools.saveAlarm(json);

        ArgumentCaptor<Alarm> alarmCap = ArgumentCaptor.forClass(Alarm.class);
        verify(restClient).saveAlarm(alarmCap.capture());
        assertThat(alarmCap.getValue().getId()).isNull();
        assertThat(alarmCap.getValue().getType()).isEqualTo("Overheat");
        assertThat(alarmCap.getValue().getOriginator()).isEqualTo(originator);

        assertThat(result).isEqualTo(JacksonUtil.toString(saved));
    }

    @Test
    void testSaveAlarm_updateExisting() {
        UUID alarmId = UUID.randomUUID();
        Alarm payload = new Alarm();
        payload.setId(new AlarmId(alarmId));
        payload.setTenantId(tenantId);
        payload.setType("Overheat");
        DeviceId originator = new DeviceId(UUID.randomUUID());
        payload.setOriginator(originator);

        when(restClient.saveAlarm(any(Alarm.class))).thenAnswer(inv -> inv.getArgument(0));

        String result = tools.saveAlarm(JacksonUtil.toString(payload));

        ArgumentCaptor<Alarm> alarmCap = ArgumentCaptor.forClass(Alarm.class);
        verify(restClient).saveAlarm(alarmCap.capture());
        assertThat(alarmCap.getValue().getId().getId()).isEqualTo(alarmId);
        assertThat(result).isEqualTo(JacksonUtil.toString(payload));
    }

    @Test
    void testDeleteAlarm_ok() {
        UUID id = UUID.randomUUID();
        String res = tools.deleteAlarm(id.toString());

        ArgumentCaptor<AlarmId> idCap = ArgumentCaptor.forClass(AlarmId.class);
        verify(restClient).deleteAlarm(idCap.capture());
        assertThat(idCap.getValue().getId()).isEqualTo(id);

        assertThat(res).contains("\"status\":\"OK\"");
        assertThat(res).contains(id.toString());
    }

    @Test
    void testDeleteAlarm_error() {
        UUID id = UUID.randomUUID();
        doThrow(new RuntimeException("boom")).when(restClient).deleteAlarm(any(AlarmId.class));

        String res = tools.deleteAlarm(id.toString());

        assertThat(res).contains("\"status\":\"ERROR\"");
        assertThat(res).contains(id.toString());
        assertThat(res).contains("boom");
    }

    @Test
    void testAckAlarm_ok() {
        UUID id = UUID.randomUUID();
        String res = tools.ackAlarm(id.toString());
        ArgumentCaptor<AlarmId> idCap = ArgumentCaptor.forClass(AlarmId.class);
        verify(restClient).ackAlarm(idCap.capture());
        assertThat(idCap.getValue().getId()).isEqualTo(id);
        assertThat(res).contains("\"status\":\"OK\"");
    }

    @Test
    void testAckAlarm_error() {
        UUID id = UUID.randomUUID();
        doThrow(new RuntimeException("not found")).when(restClient).ackAlarm(any(AlarmId.class));
        String res = tools.ackAlarm(id.toString());
        assertThat(res).contains("ERROR");
        assertThat(res).contains("not found");
        assertThat(res).contains(id.toString());
    }

    @Test
    void testClearAlarm_ok() {
        UUID id = UUID.randomUUID();
        String res = tools.clearAlarm(id.toString());
        ArgumentCaptor<AlarmId> idCap = ArgumentCaptor.forClass(AlarmId.class);
        verify(restClient).clearAlarm(idCap.capture());
        assertThat(idCap.getValue().getId()).isEqualTo(id);
        assertThat(res).contains("OK");
    }

    @Test
    void testClearAlarm_error() {
        UUID id = UUID.randomUUID();
        doThrow(new RuntimeException("already cleared")).when(restClient).clearAlarm(any(AlarmId.class));
        String res = tools.clearAlarm(id.toString());
        assertThat(res).contains("ERROR");
        assertThat(res).contains("already cleared");
        assertThat(res).contains(id.toString());
    }

    @Test
    void testFindAlarms_defaultPaging() throws Exception {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        List<AlarmInfo> alarms = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            alarms.add(createAlarmInfo(UUID.randomUUID(), deviceId));
        }
        PageData<AlarmInfo> page = new PageData<>(alarms, 1, alarms.size(), false);
        when(restClient.getAlarms(any(EntityId.class), isNull(), isNull(), any(TimePageLink.class), eq(false))).thenReturn(page);

        String result = tools.getAlarms(deviceId.getEntityType().name(), deviceId.getId().toString(), null, null,
                "100", "0", null, null, null, "0", "0", false);

        ArgumentCaptor<EntityId> entityCap = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<TimePageLink> pageCap = ArgumentCaptor.forClass(TimePageLink.class);
        verify(restClient).getAlarms(entityCap.capture(), isNull(), isNull(), pageCap.capture(), eq(false));

        assertThat(entityCap.getValue().getEntityType()).isEqualTo(deviceId.getEntityType());
        assertThat(entityCap.getValue().getId()).isEqualTo(deviceId.getId());

        TimePageLink pl = pageCap.getValue();
        assertThat(pl.getPageSize()).isEqualTo(100);
        assertThat(pl.getPage()).isEqualTo(0);
        assertThat(pl.getTextSearch()).isNull();
        assertThat(pl.getSortOrder()).isNull();
        assertThat(pl.getStartTime()).isEqualTo(0L);
        assertThat(pl.getEndTime()).isEqualTo(0L);

        assertThat(result).isEqualTo(JacksonUtil.toString(page));
    }

    @Test
    void testFindAlarms_withFiltersAndSorting() throws Exception {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        when(restClient.getAlarms(any(EntityId.class), eq(AlarmSearchStatus.ACTIVE), eq(AlarmStatus.ACTIVE_ACK), any(TimePageLink.class), eq(true)))
                .thenReturn(new PageData<>(List.of(), 0, 0, false));

        String result = tools.getAlarms(
                deviceId.getEntityType().name(),
                deviceId.getId().toString(),
                "ACTIVE",
                "ACTIVE_ACK",
                "25",
                "2",
                "temp",
                "createdTime",
                "1000",
                "ASC",
                "2000",
                true
        );

        ArgumentCaptor<TimePageLink> pageCap = ArgumentCaptor.forClass(TimePageLink.class);
        verify(restClient).getAlarms(any(EntityId.class), eq(AlarmSearchStatus.ACTIVE), eq(AlarmStatus.ACTIVE_ACK), pageCap.capture(), eq(true));

        TimePageLink pl = pageCap.getValue();
        assertThat(pl.getPageSize()).isEqualTo(25);
        assertThat(pl.getPage()).isEqualTo(2);
        assertThat(pl.getTextSearch()).isEqualTo("temp");
        SortOrder so = pl.getSortOrder();
        assertThat(so).isNotNull();
        assertThat(so.getDirection()).isEqualTo(SortOrder.Direction.ASC);
        assertThat(so.getProperty()).isEqualTo("createdTime");
        assertThat(pl.getStartTime()).isEqualTo(1000L);
        assertThat(pl.getEndTime()).isEqualTo(2000L);

        assertThat(result).isEqualTo(JacksonUtil.toString(new PageData<>(List.of(), 0, 0, false)));
    }

    @Test
    void testFindAllAlarms_defaults() throws Exception {
        PageData<AlarmInfo> page = new PageData<>(List.of(), 1, 0, false);
        when(restClient.getAllAlarms(isNull(), isNull(), isNull(), any(TimePageLink.class), eq(false))).thenReturn(page);

        String result = tools.getAllAlarms(null, null, null, "100", "0", null, null, null, "0", "0", false);

        ArgumentCaptor<TimePageLink> cap = ArgumentCaptor.forClass(TimePageLink.class);
        verify(restClient).getAllAlarms(isNull(), isNull(), isNull(), cap.capture(), eq(false));
        TimePageLink pl = cap.getValue();
        assertThat(pl.getPageSize()).isEqualTo(100);
        assertThat(pl.getPage()).isEqualTo(0);
        assertThat(pl.getTextSearch()).isNull();
        assertThat(pl.getSortOrder()).isNull();
        assertThat(pl.getStartTime()).isEqualTo(0L);
        assertThat(pl.getEndTime()).isEqualTo(0L);

        assertThat(result).isEqualTo(JacksonUtil.toString(page));
    }

    @Test
    void testFindAllAlarms_withFilters() throws Exception {
        when(restClient.getAllAlarms(eq(AlarmSearchStatus.CLEARED), eq(AlarmStatus.CLEARED_UNACK), eq("user-1"), any(TimePageLink.class), eq(true)))
                .thenReturn(new PageData<>(List.of(), 1, 0, false));

        String result = tools.getAllAlarms("CLEARED", "CLEARED_UNACK", "user-1",
                "10", "1", "temp", "endTs", "DESC", "10", "20", true);

        ArgumentCaptor<TimePageLink> cap = ArgumentCaptor.forClass(TimePageLink.class);
        verify(restClient).getAllAlarms(eq(AlarmSearchStatus.CLEARED), eq(AlarmStatus.CLEARED_UNACK), eq("user-1"), cap.capture(), eq(true));

        TimePageLink pl = cap.getValue();
        assertThat(pl.getPageSize()).isEqualTo(10);
        assertThat(pl.getPage()).isEqualTo(1);
        assertThat(pl.getTextSearch()).isEqualTo("temp");
        assertThat(pl.getSortOrder()).isNotNull();
        assertThat(pl.getSortOrder().getProperty()).isEqualTo("endTs");
        assertThat(pl.getSortOrder().getDirection()).isEqualTo(SortOrder.Direction.DESC);
        assertThat(pl.getStartTime()).isEqualTo(10L);
        assertThat(pl.getEndTime()).isEqualTo(20L);

        assertThat(result).isEqualTo(JacksonUtil.toString(new PageData<>(List.of(), 1, 0, false)));
    }

    @Test
    void testFindHighestAlarmSeverity() {
        String entityType = "DEVICE";
        String entityId = UUID.randomUUID().toString();
        when(restClient.getHighestAlarmSeverity(any(EntityId.class), eq(AlarmSearchStatus.CLEARED), eq(AlarmStatus.CLEARED_ACK)))
                .thenReturn(Optional.of(AlarmSeverity.MINOR));

        String result = tools.getHighestAlarmSeverity(entityType, entityId, "CLEARED", "CLEARED_ACK");

        ArgumentCaptor<EntityId> entityCap = ArgumentCaptor.forClass(EntityId.class);
        verify(restClient).getHighestAlarmSeverity(entityCap.capture(), eq(AlarmSearchStatus.CLEARED), eq(AlarmStatus.CLEARED_ACK));

        assertThat(entityCap.getValue().getEntityType().name()).isEqualTo(entityType);
        assertThat(entityCap.getValue().getId().toString()).isEqualTo(entityId);
        assertThat(result).isEqualTo(JacksonUtil.toString(AlarmSeverity.MINOR));
    }

    @Test
    void testHighestAlarmSeverity_emptyReturnsNullJson() {
        String entityType = "DEVICE";
        String entityId = UUID.randomUUID().toString();
        when(restClient.getHighestAlarmSeverity(any(EntityId.class), any(), any())).thenReturn(Optional.empty());

        String result = tools.getHighestAlarmSeverity(entityType, entityId, null, null);
        assertThat(result).isEqualTo("null");
    }

    @Test
    void testFindAlarmTypes() throws Exception {
        PageData<EntitySubtype> page = new PageData<>(List.of(), 1, 0, false);
        when(restClient.getAlarmTypes(any(PageLink.class))).thenReturn(page);

        String result = tools.getAlarmTypes("50", "3", "abc", "DESC");

        ArgumentCaptor<PageLink> pageCap = ArgumentCaptor.forClass(PageLink.class);
        verify(restClient).getAlarmTypes(pageCap.capture());

        PageLink pl = pageCap.getValue();
        assertThat(pl.getPageSize()).isEqualTo(50);
        assertThat(pl.getPage()).isEqualTo(3);
        assertThat(pl.getTextSearch()).isEqualTo("abc");
        assertThat(pl.getSortOrder().getDirection().name()).isEqualTo("DESC");
        assertThat(pl.getSortOrder().getProperty()).isEqualTo("type");

        assertThat(result).isEqualTo(JacksonUtil.toString(page));
    }

    private Alarm createAlarm(UUID id, EntityId originatorId) {
        Alarm alarm = new Alarm();
        alarm.setId(new AlarmId(id));
        alarm.setTenantId(tenantId);
        alarm.setType("default");
        alarm.setOriginator(originatorId);
        return alarm;
    }

    private AlarmInfo createAlarmInfo(UUID id, EntityId originatorId) {
        AlarmInfo alarmInfo = new AlarmInfo();
        alarmInfo.setId(new AlarmId(id));
        alarmInfo.setTenantId(tenantId);
        alarmInfo.setType("default");
        alarmInfo.setOriginator(originatorId);
        alarmInfo.setOriginatorName(StringUtils.randomAlphabetic(15));
        alarmInfo.setOriginatorLabel(StringUtils.randomAlphabetic(15));
        return alarmInfo;
    }

}
