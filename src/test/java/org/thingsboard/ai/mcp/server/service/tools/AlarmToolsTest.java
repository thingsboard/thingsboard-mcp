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
import org.thingsboard.server.common.data.page.TimePageLink;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.any;
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
    void testFindAlarms() throws Exception {
        when(clientService.getClient()).thenReturn(restClient);

        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        List<AlarmInfo> alarms = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            alarms.add(createAlarmInfo(UUID.randomUUID(), deviceId));
        }

        PageData<AlarmInfo> page = new PageData<>(alarms, 1, alarms.size(), false);

        when(restClient.getAlarms(any(EntityId.class), isNull(), isNull(), any(TimePageLink.class), eq(false))).thenReturn(page);

        String result = tools.getAlarms(deviceId.getEntityType().name(), deviceId.getId().toString(), null, null, 100, 0, null, null, null, 0L, 0L, false);

        ArgumentCaptor<TimePageLink> pageCap = ArgumentCaptor.forClass(TimePageLink.class);

        verify(restClient).getAlarms(any(EntityId.class), isNull(), isNull(), pageCap.capture(), eq(false));

        TimePageLink passedPage = pageCap.getValue();
        assertThat(passedPage.getPageSize()).isEqualTo(100);
        assertThat(passedPage.getPage()).isEqualTo(0);
        assertThat(passedPage.getTextSearch()).isNull();
        assertThat(passedPage.getSortOrder()).isNull();
        assertThat(passedPage.getStartTime()).isEqualTo(0L);
        assertThat(passedPage.getEndTime()).isEqualTo(0L);

        assertThat(result).isEqualTo(JacksonUtil.toString(page));
    }

    @Test
    void testFindAllAlarms() throws Exception {
        when(clientService.getClient()).thenReturn(restClient);

        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        List<AlarmInfo> alarms = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            alarms.add(createAlarmInfo(UUID.randomUUID(), deviceId));
        }

        PageData<AlarmInfo> page = new PageData<>(alarms, 1, alarms.size(), false);
        when(restClient.getAllAlarms(isNull(), isNull(), isNull(), any(TimePageLink.class), eq(false))).thenReturn(page);

        String result = tools.getAllAlarms(null, null, null, 100, 0, null, null, null, 0L, 0L, false);

        ArgumentCaptor<TimePageLink> pageCap = ArgumentCaptor.forClass(TimePageLink.class);
        verify(restClient).getAllAlarms(isNull(), isNull(), isNull(), pageCap.capture(), eq(false));

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
    void testFindHighestAlarmSeverity() {
        when(clientService.getClient()).thenReturn(restClient);

        String entityType = "DEVICE";
        String entityId = UUID.randomUUID().toString();
        when(restClient.getHighestAlarmSeverity(any(EntityId.class), eq(AlarmSearchStatus.CLEARED), eq(AlarmStatus.CLEARED_ACK))).thenReturn(Optional.of(AlarmSeverity.MINOR));

        String result = tools.getHighestAlarmSeverity(entityType, entityId, "CLEARED", "CLEARED_ACK");

        ArgumentCaptor<EntityId> entityCap = ArgumentCaptor.forClass(EntityId.class);
        verify(restClient).getHighestAlarmSeverity(entityCap.capture(), eq(AlarmSearchStatus.CLEARED), eq(AlarmStatus.CLEARED_ACK));

        assertThat(entityCap.getValue().getEntityType().name()).isEqualTo(entityType);
        assertThat(entityCap.getValue().getId().toString()).isEqualTo(entityId);
        assertThat(result).isEqualTo(JacksonUtil.toString(AlarmSeverity.MINOR));
    }

    @Test
    void testFindAlarmTypes() throws Exception {
        when(clientService.getClient()).thenReturn(restClient);

        PageData<EntitySubtype> page = new PageData<>(List.of(), 1, 0, false);
        when(restClient.getAlarmTypes(any(PageLink.class))).thenReturn(page);

        String result = tools.getAlarmTypes(50, 3, "abc", "DESC");

        ArgumentCaptor<PageLink> pageCap = ArgumentCaptor.forClass(PageLink.class);
        verify(restClient).getAlarmTypes(pageCap.capture());

        PageLink pl = pageCap.getValue();
        assertThat(pl.getPageSize()).isEqualTo(50);
        assertThat(pl.getPage()).isEqualTo(3);
        assertThat(pl.getTextSearch()).isEqualTo("abc");
        assertThat(pl.getSortOrder().getDirection().name()).isEqualTo("DESC");

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
