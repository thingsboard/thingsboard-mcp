package org.thingsboard.ai.mcp.server.service.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.ai.mcp.server.data.ThingsBoardEdition;
import org.thingsboard.ai.mcp.server.rest.RestClient;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.device.DeviceTools;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.DeviceCredentials;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PE_ONLY_AVAILABLE;

@ExtendWith(MockitoExtension.class)
public class DeviceToolsTest {

    @InjectMocks
    private DeviceTools tools;

    @Mock
    private RestClientService clientService;

    @Mock
    private RestClient restClient;

    @Captor
    private ArgumentCaptor<List<DeviceId>> deviceIdsCaptor;

    @Test
    void testFindDeviceById() {
        when(clientService.getClient()).thenReturn(restClient);

        UUID deviceUuid = UUID.randomUUID();
        Device device = new Device();
        device.setId(new DeviceId(deviceUuid));
        when(restClient.getDeviceById(any(DeviceId.class))).thenReturn(Optional.of(device));

        String result = tools.getDeviceById(deviceUuid.toString());

        ArgumentCaptor<DeviceId> idCap = ArgumentCaptor.forClass(DeviceId.class);
        verify(restClient).getDeviceById(idCap.capture());

        assertThat(idCap.getValue().getId()).isEqualTo(deviceUuid);
        assertThat(result).isEqualTo(JacksonUtil.toString(device));
    }

    @Test
    void testFindDeviceCredentialsByDeviceId() {
        when(clientService.getClient()).thenReturn(restClient);

        UUID deviceUuid = UUID.randomUUID();
        DeviceCredentials credentials = new DeviceCredentials();
        when(restClient.getDeviceCredentialsByDeviceId(any(DeviceId.class))).thenReturn(Optional.of(credentials));

        String result = tools.getDeviceCredentialsByDeviceId(deviceUuid.toString());

        ArgumentCaptor<DeviceId> idCap = ArgumentCaptor.forClass(DeviceId.class);
        verify(restClient).getDeviceCredentialsByDeviceId(idCap.capture());

        assertThat(idCap.getValue().getId()).isEqualTo(deviceUuid);
        assertThat(result).isEqualTo(JacksonUtil.toString(credentials));
    }

    @Test
    void testFindTenantDevices() throws Exception {
        when(clientService.getClient()).thenReturn(restClient);

        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Device device = new Device();
            device.setId(new DeviceId(UUID.randomUUID()));
            devices.add(device);
        }

        PageData<Device> page = new PageData<>(devices, 2, devices.size(), true);
        when(restClient.getTenantDevices(eq("sensor"), any(PageLink.class))).thenReturn(page);

        String result = tools.getTenantDevices(40, 1, "sensor", "temp", "deviceProfileName", "DESC");

        ArgumentCaptor<PageLink> pageCap = ArgumentCaptor.forClass(PageLink.class);
        verify(restClient).getTenantDevices(eq("sensor"), pageCap.capture());

        PageLink pageLink = pageCap.getValue();
        assertThat(pageLink.getPageSize()).isEqualTo(40);
        assertThat(pageLink.getPage()).isEqualTo(1);
        assertThat(pageLink.getTextSearch()).isEqualTo("temp");
        assertThat(pageLink.getSortOrder().getProperty()).isEqualTo("deviceProfileName");
        assertThat(pageLink.getSortOrder().getDirection().name()).isEqualTo("DESC");

        assertThat(result).isEqualTo(JacksonUtil.toString(page));
    }

    @Test
    void testFindTenantDevice() {
        when(clientService.getClient()).thenReturn(restClient);

        Device device = new Device();
        device.setId(new DeviceId(UUID.randomUUID()));
        when(restClient.getTenantDevice("Boiler-Device-01")).thenReturn(Optional.of(device));

        String result = tools.getTenantDevice("Boiler-Device-01");

        verify(restClient).getTenantDevice("Boiler-Device-01");
        assertThat(result).isEqualTo(JacksonUtil.toString(device));
    }

    @Test
    void testFindCustomerDevices() throws Exception {
        when(clientService.getClient()).thenReturn(restClient);

        UUID customerUuid = UUID.randomUUID();
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Device device = new Device();
            device.setId(new DeviceId(UUID.randomUUID()));
            devices.add(device);
        }

        PageData<Device> page = new PageData<>(devices, 1, devices.size(), false);
        when(restClient.getCustomerDevices(any(CustomerId.class), eq("meter"), any(PageLink.class))).thenReturn(page);

        String result = tools.getCustomerDevices(customerUuid.toString(), 25, 0, "meter", "plant", "createdTime", "ASC");

        ArgumentCaptor<CustomerId> customerCap = ArgumentCaptor.forClass(CustomerId.class);
        ArgumentCaptor<PageLink> pageCap = ArgumentCaptor.forClass(PageLink.class);
        verify(restClient).getCustomerDevices(customerCap.capture(), eq("meter"), pageCap.capture());

        assertThat(customerCap.getValue().getId()).isEqualTo(customerUuid);

        PageLink pageLink = pageCap.getValue();
        assertThat(pageLink.getPageSize()).isEqualTo(25);
        assertThat(pageLink.getPage()).isEqualTo(0);
        assertThat(pageLink.getTextSearch()).isEqualTo("plant");
        assertThat(pageLink.getSortOrder().getDirection().name()).isEqualTo("ASC");

        assertThat(result).isEqualTo(JacksonUtil.toString(page));
    }

    @Test
    void testFindUserDevices_ceEdition() throws Exception {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.CE);

        String result = tools.getUserDevices(10, 0, null, null, null, null);

        verify(clientService, never()).getClient();
        assertThat(result).isEqualTo(PE_ONLY_AVAILABLE);
    }

    @Test
    void testFindUserDevices_peEdition() throws Exception {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.PE);
        when(clientService.getClient()).thenReturn(restClient);

        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Device device = new Device();
            device.setId(new DeviceId(UUID.randomUUID()));
            devices.add(device);
        }

        PageData<Device> page = new PageData<>(devices, 1, devices.size(), false);
        when(restClient.getUserDevices(eq("pump"), any(PageLink.class))).thenReturn(page);

        String result = tools.getUserDevices(15, 2, "pump", "abc", "name", "DESC");

        ArgumentCaptor<PageLink> pageCap = ArgumentCaptor.forClass(PageLink.class);
        verify(restClient).getUserDevices(eq("pump"), pageCap.capture());

        PageLink pageLink = pageCap.getValue();
        assertThat(pageLink.getPageSize()).isEqualTo(15);
        assertThat(pageLink.getPage()).isEqualTo(2);
        assertThat(pageLink.getTextSearch()).isEqualTo("abc");
        assertThat(pageLink.getSortOrder().getProperty()).isEqualTo("name");
        assertThat(pageLink.getSortOrder().getDirection().name()).isEqualTo("DESC");

        assertThat(result).isEqualTo(JacksonUtil.toString(page));
    }

    @Test
    void testFindDevicesByIds() {
        when(clientService.getClient()).thenReturn(restClient);

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        Device device1 = new Device(); device1.setId(new DeviceId(id1));
        Device device2 = new Device(); device2.setId(new DeviceId(id2));

        List<Device> devices = List.of(device1, device2);
        when(restClient.getDevicesByIds(anyList())).thenReturn(devices);

        String result = tools.getDevicesByIds(id1.toString(), id2.toString());

        verify(restClient).getDevicesByIds(deviceIdsCaptor.capture());

        List<DeviceId> passedIds = deviceIdsCaptor.getValue();
        assertThat(passedIds).extracting(UUIDBased::getId).containsExactlyInAnyOrder(id1, id2);

        assertThat(result).isEqualTo(JacksonUtil.toString(devices));
    }

    @Test
    void testFindDevicesByEntityGroupId_ceEdition() throws Exception {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.CE);

        String result = tools.getDevicesByEntityGroupId(UUID.randomUUID().toString(), 10, 0, null, null, null);

        verify(clientService, never()).getClient();
        assertThat(result).isEqualTo(PE_ONLY_AVAILABLE);
    }

    @Test
    void testFindDevicesByEntityGroupId_peEdition() throws Exception {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.PE);
        when(clientService.getClient()).thenReturn(restClient);

        UUID groupUuid = UUID.randomUUID();
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Device device = new Device();
            device.setId(new DeviceId(UUID.randomUUID()));
            devices.add(device);
        }

        PageData<Device> page = new PageData<>(devices, 1, devices.size(), false);
        when(restClient.getDevicesByEntityGroupId(any(EntityGroupId.class), any(PageLink.class))).thenReturn(page);

        String result = tools.getDevicesByEntityGroupId(groupUuid.toString(), 20, 1, "xyz", "email", "ASC");

        ArgumentCaptor<EntityGroupId> groupCap = ArgumentCaptor.forClass(EntityGroupId.class);
        ArgumentCaptor<PageLink> pageCap = ArgumentCaptor.forClass(PageLink.class);
        verify(restClient).getDevicesByEntityGroupId(groupCap.capture(), pageCap.capture());

        assertThat(groupCap.getValue().getId()).isEqualTo(groupUuid);

        PageLink pageLink = pageCap.getValue();
        assertThat(pageLink.getPageSize()).isEqualTo(20);
        assertThat(pageLink.getPage()).isEqualTo(1);
        assertThat(pageLink.getTextSearch()).isEqualTo("xyz");
        assertThat(pageLink.getSortOrder().getProperty()).isEqualTo("email");
        assertThat(pageLink.getSortOrder().getDirection().name()).isEqualTo("ASC");

        assertThat(result).isEqualTo(JacksonUtil.toString(page));
    }

}
