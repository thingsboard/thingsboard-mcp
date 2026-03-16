package org.thingsboard.ai.mcp.server.tools.ota;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.annotation.ToolGroup;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.McpTools;
import org.thingsboard.ai.mcp.server.util.JacksonUtil;
import org.thingsboard.client.ApiException;
import org.thingsboard.client.model.ChecksumAlgorithm;
import org.thingsboard.client.model.Device;
import org.thingsboard.client.model.DeviceProfile;
import org.thingsboard.client.model.EntityType;
import org.thingsboard.client.model.OtaPackageId;
import org.thingsboard.client.model.OtaPackageType;
import org.thingsboard.client.model.SaveOtaPackageInfoRequest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.util.ToolUtils.parseIntOrDefault;
import static org.thingsboard.ai.mcp.server.util.ToolUtils.sanitizeStringParam;

@Service
@RequiredArgsConstructor
@ToolGroup("ota")
public class OtaTools implements McpTools {

    private static final String OTA_PACKAGE_JSON_EXAMPLE = """
            {
              "deviceProfileId": {"entityType": "DEVICE_PROFILE", "id": "<profileId>"},
              "type": "FIRMWARE",
              "title": "tracker_lorawan_heltec",
              "version": "1.0.32"
            }""";

    private final RestClientService clientService;

    @Tool(description = "Use this to create or update OTA package info. Omit 'id' to create; include 'id' to update.")
    public String saveOtaPackageInfo(
            @ToolParam(description = "JSON OTA package info object. " + OTA_PACKAGE_JSON_EXAMPLE)
            @NotBlank @Valid String otaPackageInfoJson,
            @ToolParam(required = false, description = "If true, the OTA package uses a URL instead of uploaded binary data.")
            Boolean isUrl) {
        SaveOtaPackageInfoRequest request = JacksonUtil.fromString(otaPackageInfoJson, SaveOtaPackageInfoRequest.class);
        return JacksonUtil.toString(clientService.getClient().saveOtaPackageInfo(request));
    }

    @Tool(description = "Use this to upload OTA package binary data from a file path on the MCP host.")
    public String saveOtaPackageData(
            @ToolParam(description = "A string value representing the OTA package id.") @NotBlank String otaPackageId,
            @ToolParam(description = "File path to OTA binary on the MCP host.") @NotBlank String filePath,
            @ToolParam(required = false, description = "Checksum algorithm: MD5, SHA256, SHA384, SHA512. Default: SHA256.") String checksumAlgorithm,
            @ToolParam(required = false, description = "Optional checksum (hex). If omitted, checksum is computed from file.") String checksum) throws Exception {
        if (StringUtils.isNotBlank(checksum)) {
            return errorJson("Checksum input is not supported yet. Omit checksum to let ThingsBoard compute it.");
        }
        String normalizedPath = normalizePathForWindows(filePath);
        Path path = Paths.get(normalizedPath);
        if (!Files.exists(path)) {
            return errorJson("File not found: " + normalizedPath);
        }
        File file = path.toFile();
        ChecksumAlgorithm algo = parseChecksumAlgorithm(checksumAlgorithm);
        return JacksonUtil.toString(clientService.getClient().saveOtaPackageData(otaPackageId, algo.name(), file, null));
    }

    @Tool(description = "Use this to download OTA package binary to a local file path on the MCP host.")
    public String downloadOtaPackage(
            @ToolParam(description = "A string value representing the OTA package id.") @NotBlank String otaPackageId,
            @ToolParam(description = "Destination file path (or directory) on the MCP host.") @NotBlank String destinationPath) throws Exception {
        File downloadedFile = clientService.getClient().downloadOtaPackage(otaPackageId);
        if (downloadedFile == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "No data returned for OTA package download");
            return JacksonUtil.toString(err);
        }
        String normalizedPath = normalizePathForWindows(destinationPath);
        Path target = Paths.get(normalizedPath);
        if (Files.exists(target) && Files.isDirectory(target)) {
            String name = downloadedFile.getName() != null ? downloadedFile.getName() : (otaPackageId + ".bin");
            target = target.resolve(name);
        }
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.copy(downloadedFile.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        Map<String, Object> result = new HashMap<>();
        result.put("status", "OK");
        result.put("path", target.toString());
        return JacksonUtil.toString(result);
    }

    @Tool(description = "Use this to get OTA package info by id.")
    public String getOtaPackageInfoById(
            @ToolParam(description = "A string value representing the OTA package id.") @NotBlank String otaPackageId) {
        return JacksonUtil.toString(clientService.getClient().getOtaPackageInfoById(otaPackageId));
    }

    @Tool(description = "Use this to get the full OTA package object by id.")
    public String getOtaPackageById(
            @ToolParam(description = "A string value representing the OTA package id.") @NotBlank String otaPackageId) {
        return JacksonUtil.toString(clientService.getClient().getOtaPackageById(otaPackageId));
    }

    @Tool(description = "Use this to get a paginated list of OTA packages.")
    public String getOtaPackages(
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the OTA package title.") String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'title', 'version', 'tag', 'name'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        return JacksonUtil.toString(clientService.getClient().getOtaPackages(
                parseIntOrDefault(pageSize, 10),
                parseIntOrDefault(page, 0),
                sanitizeStringParam(textSearch),
                sanitizeStringParam(sortProperty),
                sanitizeStringParam(sortOrder)));
    }

    @Tool(description = "Use this to get a paginated list of OTA packages filtered by device profile and type (FIRMWARE/SOFTWARE).")
    public String getOtaPackagesByDeviceProfile(
            @ToolParam(description = "A string value representing the device profile id.") @NotBlank String deviceProfileId,
            @ToolParam(description = "OTA package type. Allowed values: FIRMWARE or SOFTWARE.") @NotBlank String otaPackageType,
            @ToolParam(required = false, description = "Filter only packages that have data (default: true).") Boolean hasData,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the OTA package title.") String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'title', 'version', 'tag', 'name'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) {
        String type = otaPackageType.trim().toUpperCase();
        try {
            return JacksonUtil.toString(clientService.getClient().getOtaPackagesByDeviceProfileIdAndType(
                    deviceProfileId,
                    type,
                    parseIntOrDefault(pageSize, 10),
                    parseIntOrDefault(page, 0),
                    sanitizeStringParam(textSearch),
                    sanitizeStringParam(sortProperty),
                    sanitizeStringParam(sortOrder)));
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                return errorJson("Endpoint not available on this ThingsBoard version. Use getOtaPackages and filter client-side.");
            }
            throw e;
        }
    }

    @Tool(description = "Use this to count devices in a profile that have no assigned OTA package.")
    public String countByDeviceProfileAndEmptyOtaPackage(
            @ToolParam(description = "A string value representing the device profile id.") @NotBlank String deviceProfileId,
            @ToolParam(description = "OTA package type. Allowed values: FIRMWARE or SOFTWARE.") @NotBlank String otaPackageType) {
        String type = otaPackageType.trim().toUpperCase();
        long count = clientService.getClient().countByDeviceProfileAndEmptyOtaPackage(type, deviceProfileId);
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return JacksonUtil.toString(result);
    }

    @Tool(description = "Use this to assign or clear an OTA package (FIRMWARE/SOFTWARE) on a specific device.")
    public String assignOtaPackageToDevice(
            @ToolParam(description = "A string value representing the device id.") @NotBlank String deviceId,
            @ToolParam(required = false, description = "A string value representing the OTA package id.") String otaPackageId,
            @ToolParam(description = "OTA package type. Allowed values: FIRMWARE or SOFTWARE.") @NotBlank String otaPackageType,
            @ToolParam(required = false, description = "If true, clears the OTA package assignment.") Boolean clear) {
        boolean doClear = clear != null && clear;
        if (!doClear && StringUtils.isBlank(otaPackageId)) {
            return errorJson("otaPackageId is required unless clear=true");
        }
        String type = otaPackageType.trim().toUpperCase();
        Device device;
        try {
            device = clientService.getClient().getDeviceById(deviceId);
        } catch (ApiException e) {
            return errorJson("Device not found: " + deviceId);
        }
        OtaPackageId pkgId = doClear ? null : new OtaPackageId().id(UUID.fromString(otaPackageId)).entityType(EntityType.OTA_PACKAGE);
        if ("FIRMWARE".equals(type)) {
            device.firmwareId(pkgId);
        } else {
            device.softwareId(pkgId);
        }
        return JacksonUtil.toString(clientService.getClient().saveDevice(device, null, null, null, null, null, null));
    }

    @Tool(description = "Use this to assign or clear an OTA package (FIRMWARE/SOFTWARE) on a device profile.")
    public String assignOtaPackageToDeviceProfile(
            @ToolParam(description = "A string value representing the device profile id.") @NotBlank String deviceProfileId,
            @ToolParam(required = false, description = "A string value representing the OTA package id.") String otaPackageId,
            @ToolParam(description = "OTA package type. Allowed values: FIRMWARE or SOFTWARE.") @NotBlank String otaPackageType,
            @ToolParam(required = false, description = "If true, clears the OTA package assignment.") Boolean clear) {
        boolean doClear = clear != null && clear;
        if (!doClear && StringUtils.isBlank(otaPackageId)) {
            return errorJson("otaPackageId is required unless clear=true");
        }
        String type = otaPackageType.trim().toUpperCase();
        DeviceProfile profile;
        try {
            profile = clientService.getClient().getDeviceProfileById(deviceProfileId, false);
        } catch (ApiException e) {
            return errorJson("Device profile not found: " + deviceProfileId);
        }
        OtaPackageId pkgId = doClear ? null : new OtaPackageId().id(UUID.fromString(otaPackageId)).entityType(EntityType.OTA_PACKAGE);
        if ("FIRMWARE".equals(type)) {
            profile.firmwareId(pkgId);
        } else {
            profile.softwareId(pkgId);
        }
        return JacksonUtil.toString(clientService.getClient().saveDeviceProfile(profile));
    }

    @Tool(description = "Use this to delete an OTA package by id.")
    public String deleteOtaPackage(
            @ToolParam(description = "A string value representing the OTA package id.") @NotBlank String otaPackageId) {
        clientService.getClient().deleteOtaPackage(otaPackageId);
        return "{\"status\":\"OK\",\"id\":\"" + otaPackageId + "\"}";
    }

    private static ChecksumAlgorithm parseChecksumAlgorithm(String checksumAlgorithm) {
        if (StringUtils.isBlank(checksumAlgorithm)) {
            return ChecksumAlgorithm.SHA256;
        }
        return ChecksumAlgorithm.valueOf(checksumAlgorithm.trim().toUpperCase());
    }

    private static String normalizePathForWindows(String path) {
        if (path == null) {
            return null;
        }
        if (!isWindows()) {
            return path;
        }
        if (path.startsWith("/mnt/") && path.length() > 6) {
            char drive = path.charAt(5);
            if (path.charAt(6) == '/') {
                String rest = path.substring(7).replace("/", "\\\\");
                return Character.toUpperCase(drive) + ":\\" + rest;
            }
        }
        return path;
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name", "");
        return os.toLowerCase().contains("win");
    }

    private static String errorJson(String message) {
        Map<String, Object> err = new HashMap<>();
        err.put("status", "ERROR");
        err.put("message", message);
        return JacksonUtil.toString(err);
    }

}
