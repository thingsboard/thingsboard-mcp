package org.thingsboard.ai.mcp.server.service.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.thingsboard.ai.mcp.server.rest.RestClient;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.ota.OtaTools;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    private RestClient restClient;

    @Captor
    private ArgumentCaptor<PageLink> pageLinkCaptor;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        lenient().when(clientService.getClient()).thenReturn(restClient);
    }

    @Test
    void testSaveOtaPackageInfo() {
        OtaPackageInfo info = new OtaPackageInfo();
        info.setId(new OtaPackageId(UUID.randomUUID()));
        when(restClient.saveOtaPackageInfo(any(OtaPackageInfo.class), eq(true))).thenReturn(info);

        String result = tools.saveOtaPackageInfo(JacksonUtil.toString(info), true);

        ArgumentCaptor<OtaPackageInfo> infoCaptor = ArgumentCaptor.forClass(OtaPackageInfo.class);
        verify(restClient).saveOtaPackageInfo(infoCaptor.capture(), eq(true));
        assertThat(infoCaptor.getValue().getId()).isEqualTo(info.getId());
        assertThat(result).isEqualTo(JacksonUtil.toString(info));
    }

    @Test
    void testSaveOtaPackageData() throws Exception {
        UUID pkgUuid = UUID.randomUUID();
        Path file = Files.createTempFile(tempDir, "ota-", ".bin");
        Files.writeString(file, "ota-payload");
        OtaPackageInfo info = new OtaPackageInfo();
        info.setId(new OtaPackageId(pkgUuid));
        when(restClient.saveOtaPackageData(any(OtaPackageId.class), eq(null), any(), any(), any())).thenReturn(info);
        String result = tools.saveOtaPackageData(pkgUuid.toString(), file.toString(), "MD5", null);

        verify(restClient).saveOtaPackageData(any(), eq(null), eq(ChecksumAlgorithm.MD5), any(), any());
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
        Resource resource = new ByteArrayResource(payload) {
            @Override
            public String getFilename() {
                return "ota.bin";
            }
        };
        when(restClient.downloadOtaPackage(any(OtaPackageId.class))).thenReturn(ResponseEntity.ok(resource));

        String result = tools.downloadOtaPackage(pkgUuid.toString(), tempDir.toString());

        Path target = tempDir.resolve("ota.bin");
        assertThat(Files.exists(target)).isTrue();
        assertThat(Files.readAllBytes(target)).isEqualTo(payload);
        assertThat(result).contains("\"status\":\"OK\"");
    }

    @Test
    void testDownloadOtaPackageNoBody() throws Exception {
        UUID pkgUuid = UUID.randomUUID();
        when(restClient.downloadOtaPackage(any(OtaPackageId.class))).thenReturn(ResponseEntity.ok().build());

        String result = tools.downloadOtaPackage(pkgUuid.toString(), tempDir.toString());

        assertThat(result).contains("No data returned for OTA package download");
    }

    @Test
    void testGetOtaPackageInfoById() {
        UUID pkgUuid = UUID.randomUUID();
        OtaPackageInfo info = new OtaPackageInfo();
        info.setId(new OtaPackageId(pkgUuid));
        when(restClient.getOtaPackageInfoById(any(OtaPackageId.class))).thenReturn(info);

        String result = tools.getOtaPackageInfoById(pkgUuid.toString());

        ArgumentCaptor<OtaPackageId> idCaptor = ArgumentCaptor.forClass(OtaPackageId.class);
        verify(restClient).getOtaPackageInfoById(idCaptor.capture());
        assertThat(idCaptor.getValue().getId()).isEqualTo(pkgUuid);
        assertThat(result).isEqualTo(JacksonUtil.toString(info));
    }

    @Test
    void testGetOtaPackageById() {
        UUID pkgUuid = UUID.randomUUID();
        OtaPackage otaPackage = new OtaPackage();
        otaPackage.setId(new OtaPackageId(pkgUuid));
        when(restClient.getOtaPackageById(any(OtaPackageId.class))).thenReturn(otaPackage);

        String result = tools.getOtaPackageById(pkgUuid.toString());

        ArgumentCaptor<OtaPackageId> idCaptor = ArgumentCaptor.forClass(OtaPackageId.class);
        verify(restClient).getOtaPackageById(idCaptor.capture());
        assertThat(idCaptor.getValue().getId()).isEqualTo(pkgUuid);
        assertThat(result).isEqualTo(JacksonUtil.toString(otaPackage));
    }

    @Test
    void testGetOtaPackages() throws Exception {
        List<OtaPackageInfo> packages = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            OtaPackageInfo info = new OtaPackageInfo();
            info.setId(new OtaPackageId(UUID.randomUUID()));
            packages.add(info);
        }
        PageData<OtaPackageInfo> pageData = new PageData<>(packages, 1, packages.size(), false);
        when(restClient.getOtaPackages(any(PageLink.class))).thenReturn(pageData);

        String result = tools.getOtaPackages("10", "2", "firmware", "createdTime", "DESC");

        verify(restClient).getOtaPackages(pageLinkCaptor.capture());
        PageLink pageLink = pageLinkCaptor.getValue();
        assertThat(pageLink.getPageSize()).isEqualTo(10);
        assertThat(pageLink.getPage()).isEqualTo(2);
        assertThat(pageLink.getTextSearch()).isEqualTo("firmware");
        assertThat(pageLink.getSortOrder().getProperty()).isEqualTo("createdTime");
        assertThat(pageLink.getSortOrder().getDirection()).isEqualTo(SortOrder.Direction.DESC);

        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @Test
    void testGetOtaPackagesByDeviceProfile() throws Exception {
        UUID profileUuid = UUID.randomUUID();
        List<OtaPackageInfo> packages = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            OtaPackageInfo info = new OtaPackageInfo();
            info.setId(new OtaPackageId(UUID.randomUUID()));
            packages.add(info);
        }
        PageData<OtaPackageInfo> pageData = new PageData<>(packages, 1, packages.size(), true);
        when(restClient.getOtaPackages(any(DeviceProfileId.class), eq(OtaPackageType.FIRMWARE), eq(false), any(PageLink.class)))
                .thenReturn(pageData);

        String result = tools.getOtaPackagesByDeviceProfile(profileUuid.toString(), "FIRMWARE", false, "15", "0", "v1", "title", "ASC");

        ArgumentCaptor<DeviceProfileId> profileCaptor = ArgumentCaptor.forClass(DeviceProfileId.class);
        verify(restClient).getOtaPackages(profileCaptor.capture(), eq(OtaPackageType.FIRMWARE), eq(false), pageLinkCaptor.capture());
        assertThat(profileCaptor.getValue().getId()).isEqualTo(profileUuid);
        PageLink pageLink = pageLinkCaptor.getValue();
        assertThat(pageLink.getPageSize()).isEqualTo(15);
        assertThat(pageLink.getPage()).isEqualTo(0);
        assertThat(pageLink.getTextSearch()).isEqualTo("v1");
        assertThat(pageLink.getSortOrder().getProperty()).isEqualTo("title");
        assertThat(pageLink.getSortOrder().getDirection()).isEqualTo(SortOrder.Direction.ASC);

        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @Test
    void testCountByDeviceProfileAndEmptyOtaPackage() {
        UUID profileUuid = UUID.randomUUID();
        when(restClient.countByDeviceProfileAndEmptyOtaPackage(eq(OtaPackageType.SOFTWARE), any(DeviceProfileId.class)))
                .thenReturn(7L);

        String result = tools.countByDeviceProfileAndEmptyOtaPackage(profileUuid.toString(), "software");

        ArgumentCaptor<DeviceProfileId> profileCaptor = ArgumentCaptor.forClass(DeviceProfileId.class);
        verify(restClient).countByDeviceProfileAndEmptyOtaPackage(eq(OtaPackageType.SOFTWARE), profileCaptor.capture());
        assertThat(profileCaptor.getValue().getId()).isEqualTo(profileUuid);
        assertThat(result).isEqualTo(JacksonUtil.toString(Map.of("count", 7L)));
    }

    @Test
    void testAssignOtaPackageToDeviceFirmware() {
        UUID deviceUuid = UUID.randomUUID();
        UUID otaUuid = UUID.randomUUID();
        Device device = new Device();
        device.setId(new DeviceId(deviceUuid));
        when(restClient.getDeviceById(any(DeviceId.class))).thenReturn(Optional.of(device));
        when(restClient.saveDevice(any(Device.class))).thenReturn(device);

        String result = tools.assignOtaPackageToDevice(deviceUuid.toString(), otaUuid.toString(), "FIRMWARE", false);

        ArgumentCaptor<Device> deviceCaptor = ArgumentCaptor.forClass(Device.class);
        verify(restClient).saveDevice(deviceCaptor.capture());
        assertThat(deviceCaptor.getValue().getFirmwareId().getId()).isEqualTo(otaUuid);
        assertThat(result).isEqualTo(JacksonUtil.toString(device));
    }

    @Test
    void testAssignOtaPackageToDeviceProfileSoftware() {
        UUID profileUuid = UUID.randomUUID();
        UUID otaUuid = UUID.randomUUID();
        DeviceProfile profile = new DeviceProfile();
        profile.setId(new DeviceProfileId(profileUuid));
        when(restClient.getDeviceProfileById(any(DeviceProfileId.class))).thenReturn(Optional.of(profile));
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

        ArgumentCaptor<OtaPackageId> idCaptor = ArgumentCaptor.forClass(OtaPackageId.class);
        verify(restClient).deleteOtaPackage(idCaptor.capture());
        assertThat(idCaptor.getValue().getId()).isEqualTo(otaUuid);
        assertThat(result).isEqualTo("{\"status\":\"OK\",\"id\":\"" + otaUuid + "\"}");
    }

}
