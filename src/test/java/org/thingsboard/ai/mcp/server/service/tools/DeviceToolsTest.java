package org.thingsboard.ai.mcp.server.service.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.ai.mcp.server.data.ThingsBoardEdition;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.device.DeviceTools;
import org.thingsboard.ai.mcp.server.util.JacksonUtil;
import org.thingsboard.client.ThingsboardClient;
import org.thingsboard.client.model.Device;
import org.thingsboard.client.model.DeviceCredentials;
import org.thingsboard.client.model.DeviceId;
import org.thingsboard.client.model.PageDataDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
    private ThingsboardClient restClient;

    @BeforeEach
    void setup() {
        when(clientService.getClient()).thenReturn(restClient);
    }

    @Test
    void testFindDeviceById() {
        String deviceId = UUID.randomUUID().toString();
        Device device = new Device().name("test-device");
        when(restClient.getDeviceById(anyString())).thenReturn(device);

        String result = tools.getDeviceById(deviceId);

        verify(restClient).getDeviceById(eq(deviceId));
        assertThat(result).isEqualTo(JacksonUtil.toString(device));
    }

    @Test
    void testFindDeviceCredentialsByDeviceId() {
        String deviceId = UUID.randomUUID().toString();
        DeviceCredentials credentials = new DeviceCredentials();
        when(restClient.getDeviceCredentialsByDeviceId(anyString())).thenReturn(credentials);

        String result = tools.getDeviceCredentialsByDeviceId(deviceId);

        verify(restClient).getDeviceCredentialsByDeviceId(eq(deviceId));
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
            devices.add(new Device().name("device-" + i));
        }
        PageDataDevice pageData = new PageDataDevice(2, (long) devices.size(), true).data(devices);
        when(restClient.getTenantDevices(anyInt(), anyInt(), any(), any(), any(), any())).thenReturn(pageData);

        String result = tools.getTenantDevices(Integer.toString(pageSize), Integer.toString(page), type, text, sortProp, dir);

        verify(restClient).getTenantDevices(eq(pageSize), eq(page), eq(type), eq(text), eq(sortProp), eq(dir));
        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @Test
    void testFindTenantDevice() {
        Device device = new Device().name("Boiler-Device-01");
        when(restClient.getTenantDeviceByName("Boiler-Device-01")).thenReturn(device);

        String result = tools.getTenantDevice("Boiler-Device-01");

        verify(restClient).getTenantDeviceByName("Boiler-Device-01");
        assertThat(result).isEqualTo(JacksonUtil.toString(device));
    }

    @ParameterizedTest(name = "customerDevices page={1} size={0} type={3} text={4} sort={5} {6}")
    @CsvSource({
            "25,0,meter,plant,createdTime,ASC",
            "5,2,,heat,name,DESC"
    })
    void testFindCustomerDevices(int pageSize, int page, String type, String text, String sortProp, String dir) throws Exception {
        String customerId = UUID.randomUUID().toString();
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            devices.add(new Device().name("device-" + i));
        }

        PageDataDevice pageData = new PageDataDevice(1, (long) devices.size(), false).data(devices);
        when(restClient.getCustomerDevices(anyString(), anyInt(), anyInt(), any(), any(), any(), any())).thenReturn(pageData);

        String result = tools.getCustomerDevices(customerId, Integer.toString(pageSize), Integer.toString(page), type, text, sortProp, dir);

        verify(restClient).getCustomerDevices(eq(customerId), eq(pageSize), eq(page), eq(type), eq(text), eq(sortProp), eq(dir));
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
            devices.add(new Device().name("device-" + i));
        }

        PageDataDevice pageData = new PageDataDevice(1, (long) devices.size(), false).data(devices);
        when(restClient.getUserDevices(any(), any(), any(), any(), any(), any())).thenReturn(pageData);

        String result = tools.getUserDevices(Integer.toString(pageSize), Integer.toString(page), type, text, sortProp, dir);

        verify(restClient).getUserDevices(any(), any(), any(), any(), any(), any());
        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @Test
    void testFindDevicesByIds() {
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();

        Device device1 = new Device().name("d1");
        Device device2 = new Device().name("d2");

        List<Device> devices = List.of(device1, device2);
        when(restClient.getDevicesByIds(anyList())).thenReturn(devices);

        String result = tools.getDevicesByIds(id1 + "," + id2);

        verify(restClient).getDevicesByIds(eq(List.of(id1, id2)));
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

        String groupId = UUID.randomUUID().toString();
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            devices.add(new Device().name("device-" + i));
        }

        PageDataDevice pageData = new PageDataDevice(1, (long) devices.size(), false).data(devices);
        when(restClient.getDevicesByEntityGroupId(anyString(), any(), any(), any(), any(), any())).thenReturn(pageData);

        String result = tools.getDevicesByEntityGroupId(groupId, Integer.toString(pageSize), Integer.toString(page), text, sortProp, dir);

        verify(restClient).getDevicesByEntityGroupId(eq(groupId), any(), any(), any(), any(), any());
        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @Nested
    @DisplayName("saveDevice variants")
    class SaveDeviceVariants {
        @Test
        void testSaveDevice_withoutGroups_noToken() {
            Device payload = new Device().name("A4B72CCDFF233");
            when(restClient.saveDevice(any(Device.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(payload);

            String res = tools.saveDevice(JacksonUtil.toString(payload), null, null, null);

            verify(restClient).saveDevice(any(Device.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
            assertThat(res).isEqualTo(JacksonUtil.toString(payload));
        }

        @Test
        void testSaveDevice_withAccessToken_only() {
            Device payload = new Device().name("A1");
            when(restClient.saveDevice(any(Device.class), eq("tok"), isNull(), isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(payload);

            String res = tools.saveDevice(JacksonUtil.toString(payload), "tok", null, null);

            verify(restClient).saveDevice(any(Device.class), eq("tok"), isNull(), isNull(), isNull(), isNull(), isNull());
            assertThat(res).isEqualTo(JacksonUtil.toString(payload));
        }

        @Test
        void testSaveDevice_withSingleGroup() {
            Device payload = new Device().name("A2");
            String group = UUID.randomUUID().toString();

            when(restClient.saveDevice(any(Device.class), eq("tok"), eq(group), isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(payload);

            String res = tools.saveDevice(JacksonUtil.toString(payload), "tok", group, null);

            verify(restClient).saveDevice(any(Device.class), eq("tok"), eq(group), isNull(), isNull(), isNull(), isNull());
            assertThat(res).isEqualTo(JacksonUtil.toString(payload));
        }

        @Test
        void testSaveDevice_withMultipleGroups() {
            Device payload = new Device().name("A3");
            String id1 = UUID.randomUUID().toString();
            String id2 = UUID.randomUUID().toString();
            String groupIds = id1 + "," + id2;

            when(restClient.saveDevice(any(Device.class), eq("tok"), isNull(), eq(List.of(id1, id2)), isNull(), isNull(), isNull()))
                    .thenReturn(payload);

            String res = tools.saveDevice(JacksonUtil.toString(payload), "tok", null, groupIds);

            verify(restClient).saveDevice(any(Device.class), eq("tok"), isNull(), eq(List.of(id1, id2)), isNull(), isNull(), isNull());
            assertThat(res).isEqualTo(JacksonUtil.toString(payload));
        }

    }

    @Nested
    @DisplayName("deleteDevice JSON contract")
    class DeleteDeviceContract {
        @Test
        void testDeleteDevice_ok() {
            String id = UUID.randomUUID().toString();
            String res = tools.deleteDevice(id);

            verify(restClient).deleteDevice(eq(id));

            assertThat(res).contains("\"status\":\"OK\"");
            assertThat(res).contains(id);
        }

        @Test
        void testDeleteDevice_error() {
            String id = UUID.randomUUID().toString();
            doThrow(new RuntimeException("boom")).when(restClient).deleteDevice(anyString());

            String res = tools.deleteDevice(id);

            assertThat(res).contains("\"status\":\"ERROR\"");
            assertThat(res).contains(id);
            assertThat(res).contains("boom");
        }

    }

}
