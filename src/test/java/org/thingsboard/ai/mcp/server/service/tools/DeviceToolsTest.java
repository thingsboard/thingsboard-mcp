package org.thingsboard.ai.mcp.server.service.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.security.DeviceCredentials;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Captor
    private ArgumentCaptor<PageLink> pageLinkCaptor;

    @BeforeEach
    void setup() {
        when(clientService.getClient()).thenReturn(restClient);
    }

    @Test
    void testFindDeviceById() {
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
        UUID deviceUuid = UUID.randomUUID();
        DeviceCredentials credentials = new DeviceCredentials();
        when(restClient.getDeviceCredentialsByDeviceId(any(DeviceId.class))).thenReturn(Optional.of(credentials));

        String result = tools.getDeviceCredentialsByDeviceId(deviceUuid.toString());

        ArgumentCaptor<DeviceId> idCap = ArgumentCaptor.forClass(DeviceId.class);
        verify(restClient).getDeviceCredentialsByDeviceId(idCap.capture());

        assertThat(idCap.getValue().getId()).isEqualTo(deviceUuid);
        assertThat(result).isEqualTo(JacksonUtil.toString(credentials));
    }

    @ParameterizedTest(name = "tenantDevices page={1} size={0} type={2} text={3} sort={4} {5}")
    @CsvSource({
            "40,1,sensor,temp,deviceProfileName,DESC",
            "10,0,,room,name,ASC"
    })
    void testFindTenantDevices(int pageSize, int page, String type, String text, String sortProp, String dir) throws Exception {
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Device d = new Device();
            d.setId(new DeviceId(UUID.randomUUID()));
            devices.add(d);
        }
        PageData<Device> pageData = new PageData<>(devices, 2, devices.size(), true);
        when(restClient.getTenantDevices(eq(type), any(PageLink.class))).thenReturn(pageData);

        String result = tools.getTenantDevices(Integer.toString(pageSize), Integer.toString(page), type, text, sortProp, dir);

        verify(restClient).getTenantDevices(eq(type), pageLinkCaptor.capture());
        PageLink pl = pageLinkCaptor.getValue();
        assertThat(pl.getPageSize()).isEqualTo(pageSize);
        assertThat(pl.getPage()).isEqualTo(page);
        assertThat(pl.getTextSearch()).isEqualTo(text);
        if (sortProp != null) {
            assertThat(pl.getSortOrder()).isNotNull();
            assertThat(pl.getSortOrder().getProperty()).isEqualTo(sortProp);
            assertThat(pl.getSortOrder().getDirection()).isEqualTo(SortOrder.Direction.valueOf(dir));
        }
        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @Test
    void testFindTenantDevice() {
        Device device = new Device();
        device.setId(new DeviceId(UUID.randomUUID()));
        when(restClient.getTenantDevice("Boiler-Device-01")).thenReturn(Optional.of(device));

        String result = tools.getTenantDevice("Boiler-Device-01");

        verify(restClient).getTenantDevice("Boiler-Device-01");
        assertThat(result).isEqualTo(JacksonUtil.toString(device));
    }

    @ParameterizedTest(name = "customerDevices page={1} size={0} type={3} text={4} sort={5} {6}")
    @CsvSource({
            "25,0,meter,plant,createdTime,ASC",
            "5,2,,heat,name,DESC"
    })
    void testFindCustomerDevices(int pageSize, int page, String type, String text, String sortProp, String dir) throws Exception {
        UUID customerUuid = UUID.randomUUID();
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Device d = new Device();
            d.setId(new DeviceId(UUID.randomUUID()));
            devices.add(d);
        }

        PageData<Device> pageData = new PageData<>(devices, 1, devices.size(), false);
        when(restClient.getCustomerDevices(any(CustomerId.class), eq(type), any(PageLink.class))).thenReturn(pageData);

        String result = tools.getCustomerDevices(customerUuid.toString(), Integer.toString(pageSize), Integer.toString(page), type, text, sortProp, dir);

        ArgumentCaptor<CustomerId> customerCap = ArgumentCaptor.forClass(CustomerId.class);
        verify(restClient).getCustomerDevices(customerCap.capture(), eq(type), pageLinkCaptor.capture());
        assertThat(customerCap.getValue().getId()).isEqualTo(customerUuid);

        PageLink pl = pageLinkCaptor.getValue();
        assertThat(pl.getPageSize()).isEqualTo(pageSize);
        assertThat(pl.getPage()).isEqualTo(page);
        assertThat(pl.getTextSearch()).isEqualTo(text);
        if (sortProp != null) {
            assertThat(pl.getSortOrder()).isNotNull();
            assertThat(pl.getSortOrder().getProperty()).isEqualTo(sortProp);
            assertThat(pl.getSortOrder().getDirection()).isEqualTo(SortOrder.Direction.valueOf(dir));
        }

        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @ParameterizedTest(name = "userDevices page={1} size={0} type={2} text={3} sort={4} {5}")
    @CsvSource({
            "15,2,pump,abc,name,DESC",
            "8,0,,temp,createdTime,ASC"
    })
    void testFindUserDevices(int pageSize, int page, String type, String text, String sortProp, String dir) throws Exception {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.PE);
        when(clientService.getClient()).thenReturn(restClient);

        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Device d = new Device();
            d.setId(new DeviceId(UUID.randomUUID()));
            devices.add(d);
        }

        PageData<Device> pageData = new PageData<>(devices, 1, devices.size(), false);
        when(restClient.getUserDevices(eq(type), any(PageLink.class))).thenReturn(pageData);

        String result = tools.getUserDevices(Integer.toString(pageSize), Integer.toString(page), type, text, sortProp, dir);

        verify(restClient).getUserDevices(eq(type), pageLinkCaptor.capture());
        PageLink pl = pageLinkCaptor.getValue();
        assertThat(pl.getPageSize()).isEqualTo(pageSize);
        assertThat(pl.getPage()).isEqualTo(page);
        assertThat(pl.getTextSearch()).isEqualTo(text);
        if (sortProp != null) {
            assertThat(pl.getSortOrder()).isNotNull();
            assertThat(pl.getSortOrder().getProperty()).isEqualTo(sortProp);
            assertThat(pl.getSortOrder().getDirection()).isEqualTo(SortOrder.Direction.valueOf(dir));
        }

        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @Test
    void testFindDevicesByIds() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        Device device1 = new Device(); device1.setId(new DeviceId(id1));
        Device device2 = new Device(); device2.setId(new DeviceId(id2));

        List<Device> devices = List.of(device1, device2);
        when(restClient.getDevicesByIds(anyList())).thenReturn(devices);

        String result = tools.getDevicesByIds(id1 + "," + id2);

        verify(restClient).getDevicesByIds(deviceIdsCaptor.capture());

        List<DeviceId> passedIds = deviceIdsCaptor.getValue();
        assertThat(passedIds).extracting(UUIDBased::getId).containsExactlyInAnyOrder(id1, id2);

        assertThat(result).isEqualTo(JacksonUtil.toString(devices));
    }

    @ParameterizedTest(name = "devicesByGroup page={1} size={0} sort={4} {5}")
    @CsvSource({
            "20,1,email,ASC,xyz",
            "5,0,firstName,DESC,"
    })
    void testFindDevicesByEntityGroupId(int pageSize, int page, String sortProp, String dir, String text) throws Exception {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.PE);
        when(clientService.getClient()).thenReturn(restClient);

        UUID groupUuid = UUID.randomUUID();
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Device d = new Device();
            d.setId(new DeviceId(UUID.randomUUID()));
            devices.add(d);
        }

        PageData<Device> pageData = new PageData<>(devices, 1, devices.size(), false);
        when(restClient.getDevicesByEntityGroupId(any(EntityGroupId.class), any(PageLink.class))).thenReturn(pageData);

        String result = tools.getDevicesByEntityGroupId(groupUuid.toString(), Integer.toString(pageSize), Integer.toString(page), text, sortProp, dir);

        ArgumentCaptor<EntityGroupId> groupCap = ArgumentCaptor.forClass(EntityGroupId.class);
        verify(restClient).getDevicesByEntityGroupId(groupCap.capture(), pageLinkCaptor.capture());

        assertThat(groupCap.getValue().getId()).isEqualTo(groupUuid);

        PageLink pl = pageLinkCaptor.getValue();
        assertThat(pl.getPageSize()).isEqualTo(pageSize);
        assertThat(pl.getPage()).isEqualTo(page);
        assertThat(pl.getTextSearch()).isEqualTo(text);
        assertThat(pl.getSortOrder()).isNotNull();
        assertThat(pl.getSortOrder().getProperty()).isEqualTo(sortProp);
        assertThat(pl.getSortOrder().getDirection()).isEqualTo(SortOrder.Direction.valueOf(dir));

        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @Nested
    @DisplayName("saveDevice variants")
    class SaveDeviceVariants {
        @Test
        void testSaveDevice_withoutGroups_noToken() {
            Device payload = new Device();
            payload.setName("A4B72CCDFF233");
            when(restClient.saveDevice(any(Device.class), eq((String) null))).thenAnswer(inv -> inv.getArgument(0));

            String res = tools.saveDevice(JacksonUtil.toString(payload), null, null, null);

            ArgumentCaptor<Device> cap = ArgumentCaptor.forClass(Device.class);
            verify(restClient).saveDevice(cap.capture(), eq((String) null));
            assertThat(cap.getValue().getName()).isEqualTo("A4B72CCDFF233");
            assertThat(res).isEqualTo(JacksonUtil.toString(payload));
        }

        @Test
        void testSaveDevice_withAccessToken_only() {
            Device payload = new Device();
            payload.setName("A1");
            when(restClient.saveDevice(any(Device.class), eq("tok"))).thenAnswer(inv -> inv.getArgument(0));

            String res = tools.saveDevice(JacksonUtil.toString(payload), "tok", null, null);

            verify(restClient).saveDevice(any(Device.class), eq("tok"));
            assertThat(res).isEqualTo(JacksonUtil.toString(payload));
        }

        @Test
        void testSaveDevice_withSingleGroup() {
            Device payload = new Device();
            payload.setName("A2");
            UUID group = UUID.randomUUID();

            when(restClient.saveDevice(any(Device.class), eq("tok"), any(EntityGroupId.class), eq(null)))
                    .thenAnswer(inv -> inv.getArgument(0));

            String res = tools.saveDevice(JacksonUtil.toString(payload), "tok", group.toString(), null);

            ArgumentCaptor<EntityGroupId> egCap = ArgumentCaptor.forClass(EntityGroupId.class);
            verify(restClient).saveDevice(any(Device.class), eq("tok"), egCap.capture(), eq(null));
            assertThat(egCap.getValue().getId()).isEqualTo(group);
            assertThat(res).isEqualTo(JacksonUtil.toString(payload));
        }

        @Test
        void testSaveDevice_withMultipleGroups() {
            Device payload = new Device();
            payload.setName("A3");
            String groupIds = UUID.randomUUID() + "," + UUID.randomUUID();

            when(restClient.saveDevice(any(Device.class), eq("tok"), eq(null), eq(groupIds)))
                    .thenAnswer(inv -> inv.getArgument(0));

            String res = tools.saveDevice(JacksonUtil.toString(payload), "tok", null, groupIds);

            verify(restClient).saveDevice(any(Device.class), eq("tok"), eq(null), eq(groupIds));
            assertThat(res).isEqualTo(JacksonUtil.toString(payload));
        }

    }

    @Nested
    @DisplayName("deleteDevice JSON contract")
    class DeleteDeviceContract {
        @Test
        void testDeleteDevice_ok() {
            UUID id = UUID.randomUUID();
            String res = tools.deleteDevice(id.toString());

            ArgumentCaptor<DeviceId> idCap = ArgumentCaptor.forClass(DeviceId.class);
            verify(restClient).deleteDevice(idCap.capture());
            assertThat(idCap.getValue().getId()).isEqualTo(id);

            assertThat(res).contains("\"status\":\"OK\"");
            assertThat(res).contains(id.toString());
        }

        @Test
        void testDeleteDevice_error() {
            UUID id = UUID.randomUUID();
            doThrow(new RuntimeException("boom")).when(restClient).deleteDevice(any(DeviceId.class));

            String res = tools.deleteDevice(id.toString());

            assertThat(res).contains("\"status\":\"ERROR\"");
            assertThat(res).contains(id.toString());
            assertThat(res).contains("boom");
        }

    }

}
