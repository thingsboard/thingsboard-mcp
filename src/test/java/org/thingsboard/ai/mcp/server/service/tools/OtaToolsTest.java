package org.thingsboard.ai.mcp.server.service.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.ota.OtaTools;
import org.thingsboard.ai.mcp.server.util.JacksonUtil;
import org.thingsboard.client.ThingsboardClient;
import org.thingsboard.client.model.Device;
import org.thingsboard.client.model.DeviceProfile;
import org.thingsboard.client.model.EntityType;
import org.thingsboard.client.model.OtaPackage;
import org.thingsboard.client.model.OtaPackageId;
import org.thingsboard.client.model.OtaPackageInfo;
import org.thingsboard.client.model.PageDataOtaPackageInfo;
import org.thingsboard.client.model.SaveOtaPackageInfoRequest;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OtaToolsTest {

    @InjectMocks
    private OtaTools tools;

    @Mock
    private RestClientService clientService;

    @Mock
    private ThingsboardClient restClient;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        lenient().when(clientService.getClient()).thenReturn(restClient);
    }

    @Test
    void testSaveOtaPackageInfo() {
        OtaPackageInfo info = new OtaPackageInfo();
        info.setId(new OtaPackageId().id(UUID.randomUUID()).entityType(EntityType.OTA_PACKAGE));
        when(restClient.saveOtaPackageInfo(any(SaveOtaPackageInfoRequest.class))).thenReturn(info);

        String result = tools.saveOtaPackageInfo(JacksonUtil.toString(info), true);

        verify(restClient).saveOtaPackageInfo(any(SaveOtaPackageInfoRequest.class));
        assertThat(result).isEqualTo(JacksonUtil.toString(info));
    }

    @Test
    void testSaveOtaPackageData() throws Exception {
        UUID pkgUuid = UUID.randomUUID();
        Path file = Files.createTempFile(tempDir, "ota-", ".bin");
        Files.writeString(file, "ota-payload");
        OtaPackageInfo info = new OtaPackageInfo();
        info.setId(new OtaPackageId().id(pkgUuid).entityType(EntityType.OTA_PACKAGE));
        when(restClient.saveOtaPackageData(anyString(), anyString(), any(File.class), any())).thenReturn(info);

        String result = tools.saveOtaPackageData(pkgUuid.toString(), file.toString(), "MD5", null);

        verify(restClient).saveOtaPackageData(eq(pkgUuid.toString()), eq("MD5"), any(File.class), any());
        assertThat(result).contains(pkgUuid.toString());
        assertThat(result).isEqualTo(JacksonUtil.toString(info));
    }

    @Test
    void testSaveOtaPackageDataFileNotFound() throws Exception {
        UUID pkgUuid = UUID.randomUUID();
        Path missing = tempDir.resolve("missing.bin");

        String result = tools.saveOtaPackageData(pkgUuid.toString(), missing.toString(), null, null);

        assertThat(result).contains("File not found");
    }

    @Test
    void testDownloadOtaPackageToDirectory() throws Exception {
        UUID pkgUuid = UUID.randomUUID();
        byte[] payload = "ota-download".getBytes(StandardCharsets.UTF_8);
        Path tempFile = Files.createTempFile(tempDir, "download-", ".bin");
        Files.write(tempFile, payload);
        Path otaFile = tempDir.resolve("ota-source.bin");
        Files.move(tempFile, otaFile);

        when(restClient.downloadOtaPackage(anyString())).thenReturn(otaFile.toFile());

        String result = tools.downloadOtaPackage(pkgUuid.toString(), tempDir.toString());

        assertThat(result).contains("\"status\":\"OK\"");
    }

    @Test
    void testDownloadOtaPackageNoBody() throws Exception {
        UUID pkgUuid = UUID.randomUUID();
        when(restClient.downloadOtaPackage(anyString())).thenReturn(null);

        String result = tools.downloadOtaPackage(pkgUuid.toString(), tempDir.toString());

        assertThat(result).contains("No data returned for OTA package download");
    }

    @Test
    void testGetOtaPackageInfoById() {
        UUID pkgUuid = UUID.randomUUID();
        OtaPackageInfo info = new OtaPackageInfo();
        info.setId(new OtaPackageId().id(pkgUuid).entityType(EntityType.OTA_PACKAGE));
        when(restClient.getOtaPackageInfoById(anyString())).thenReturn(info);

        String result = tools.getOtaPackageInfoById(pkgUuid.toString());

        verify(restClient).getOtaPackageInfoById(eq(pkgUuid.toString()));
        assertThat(result).isEqualTo(JacksonUtil.toString(info));
    }

    @Test
    void testGetOtaPackageById() {
        UUID pkgUuid = UUID.randomUUID();
        OtaPackage otaPackage = new OtaPackage();
        otaPackage.setId(new OtaPackageId().id(pkgUuid).entityType(EntityType.OTA_PACKAGE));
        when(restClient.getOtaPackageById(anyString())).thenReturn(otaPackage);

        String result = tools.getOtaPackageById(pkgUuid.toString());

        verify(restClient).getOtaPackageById(eq(pkgUuid.toString()));
        assertThat(result).isEqualTo(JacksonUtil.toString(otaPackage));
    }

    @Test
    void testGetOtaPackages() throws Exception {
        List<OtaPackageInfo> packages = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            OtaPackageInfo info = new OtaPackageInfo();
            info.setId(new OtaPackageId().id(UUID.randomUUID()).entityType(EntityType.OTA_PACKAGE));
            packages.add(info);
        }
        PageDataOtaPackageInfo pageData = new PageDataOtaPackageInfo(1, (long) packages.size(), false).data(packages);
        when(restClient.getOtaPackages(any(), any(), any(), any(), any())).thenReturn(pageData);

        String result = tools.getOtaPackages("10", "2", "firmware", "createdTime", "DESC");

        verify(restClient).getOtaPackages(eq(10), eq(2), eq("firmware"), eq("createdTime"), eq("DESC"));
        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @Test
    void testGetOtaPackagesByDeviceProfile() throws Exception {
        UUID profileUuid = UUID.randomUUID();
        List<OtaPackageInfo> packages = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            OtaPackageInfo info = new OtaPackageInfo();
            info.setId(new OtaPackageId().id(UUID.randomUUID()).entityType(EntityType.OTA_PACKAGE));
            packages.add(info);
        }
        PageDataOtaPackageInfo pageData = new PageDataOtaPackageInfo(1, (long) packages.size(), true).data(packages);
        when(restClient.getOtaPackagesByDeviceProfileIdAndType(anyString(), anyString(), any(), any(), any(), any(), any()))
                .thenReturn(pageData);

        String result = tools.getOtaPackagesByDeviceProfile(profileUuid.toString(), "FIRMWARE", false, "15", "0", "v1", "title", "ASC");

        verify(restClient).getOtaPackagesByDeviceProfileIdAndType(
                eq(profileUuid.toString()), eq("FIRMWARE"), eq(15), eq(0), eq("v1"), eq("title"), eq("ASC"));
        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @Test
    void testCountByDeviceProfileAndEmptyOtaPackage() {
        UUID profileUuid = UUID.randomUUID();
        when(restClient.countByDeviceProfileAndEmptyOtaPackage(eq("SOFTWARE"), anyString()))
                .thenReturn(7L);

        String result = tools.countByDeviceProfileAndEmptyOtaPackage(profileUuid.toString(), "software");

        verify(restClient).countByDeviceProfileAndEmptyOtaPackage(eq("SOFTWARE"), eq(profileUuid.toString()));
        assertThat(result).isEqualTo(JacksonUtil.toString(Map.of("count", 7L)));
    }

    @Test
    void testAssignOtaPackageToDeviceFirmware() {
        UUID deviceUuid = UUID.randomUUID();
        UUID otaUuid = UUID.randomUUID();
        Device device = new Device();
        device.setId(new org.thingsboard.client.model.DeviceId().id(deviceUuid).entityType(EntityType.DEVICE));
        when(restClient.getDeviceById(anyString())).thenReturn(device);
        when(restClient.saveDevice(any(Device.class), any(), any(), any(), any(), any(), any())).thenReturn(device);

        String result = tools.assignOtaPackageToDevice(deviceUuid.toString(), otaUuid.toString(), "FIRMWARE", false);

        ArgumentCaptor<Device> deviceCaptor = ArgumentCaptor.forClass(Device.class);
        verify(restClient).saveDevice(deviceCaptor.capture(), any(), any(), any(), any(), any(), any());
        assertThat(deviceCaptor.getValue().getFirmwareId().getId()).isEqualTo(otaUuid);
        assertThat(result).isEqualTo(JacksonUtil.toString(device));
    }

    @Test
    void testAssignOtaPackageToDeviceProfileSoftware() {
        UUID profileUuid = UUID.randomUUID();
        UUID otaUuid = UUID.randomUUID();
        DeviceProfile profile = new DeviceProfile();
        profile.setId(new org.thingsboard.client.model.DeviceProfileId().id(profileUuid).entityType(EntityType.DEVICE_PROFILE));
        when(restClient.getDeviceProfileById(anyString(), eq(false))).thenReturn(profile);
        when(restClient.saveDeviceProfile(any(DeviceProfile.class))).thenReturn(profile);

        String result = tools.assignOtaPackageToDeviceProfile(profileUuid.toString(), otaUuid.toString(), "software", false);

        ArgumentCaptor<DeviceProfile> profileCaptor = ArgumentCaptor.forClass(DeviceProfile.class);
        verify(restClient).saveDeviceProfile(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getSoftwareId().getId()).isEqualTo(otaUuid);
        assertThat(result).isEqualTo(JacksonUtil.toString(profile));
    }

    @Test
    void testDeleteOtaPackage() {
        UUID otaUuid = UUID.randomUUID();

        String result = tools.deleteOtaPackage(otaUuid.toString());

        verify(restClient).deleteOtaPackage(eq(otaUuid.toString()));
        assertThat(result).isEqualTo("{\"status\":\"OK\",\"id\":\"" + otaUuid + "\"}");
    }

}
