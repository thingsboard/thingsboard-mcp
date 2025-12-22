package org.thingsboard.ai.mcp.server.tools.ota;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.McpTools;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.page.PageLink;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.ai.mcp.server.util.ToolUtils.createPageLink;

@Service
@RequiredArgsConstructor
public class OtaTools implements McpTools {

    private static final String OTA_PACKAGE_JSON_EXAMPLE =
            """
                    ```json
                    {
                      \"id\": { \"entityType\": \"OTA_PACKAGE\", \"id\": \"d3cfc080-a295-11f0-848c-93db0ade7d93\" },
                      \"tenantId\": { \"entityType\": \"TENANT\", \"id\": \"d3cfc080-a295-11f0-848c-93db0ade7d93\" },
                      \"deviceProfileId\": { \"entityType\": \"DEVICE_PROFILE\", \"id\": \"716a92d0-9d36-11f0-a79c-e726b4e8048a\" },
                      \"type\": \"FIRMWARE\",
                      \"title\": \"tracker_lorawan_heltec\",
                      \"version\": \"1.0.32\",
                      \"tag\": \"tracker_lorawan_heltec 1.0.32\",
                      \"url\": null,
                      \"additionalInfo\": { \"description\": \"Heltec tracker OTA\" }
                    }
                    ```""";

    private final RestClientService clientService;

    @Tool(description = "Create or update OTA package info. Omit 'id' to create a new package; include 'id' to update existing package." +
            TENANT_AUTHORITY_PARAGRAPH)
    public String saveOtaPackageInfo(
            @ToolParam(description = "A JSON string representing the OTA package info. " + OTA_PACKAGE_JSON_EXAMPLE)
            @NotBlank @Valid String otaPackageInfoJson,
            @ToolParam(required = false, description = "If true, the OTA package uses a URL instead of uploaded binary data.")
            Boolean isUrl) {
        OtaPackageInfo otaPackageInfo = JacksonUtil.fromString(otaPackageInfoJson, OtaPackageInfo.class);
        boolean useUrl = isUrl != null && isUrl;
        return JacksonUtil.toString(clientService.getClient().saveOtaPackageInfo(otaPackageInfo, useUrl));
    }

    @Tool(description = "Upload OTA package binary data from a file path on the MCP host. If checksum is omitted, it is computed automatically." +
            TENANT_AUTHORITY_PARAGRAPH)
    public String saveOtaPackageData(
            @ToolParam(description = "A string value representing the OTA package id.") @NotBlank String otaPackageId,
            @ToolParam(description = "File path to OTA binary on the MCP host.") @NotBlank String filePath,
            @ToolParam(required = false, description = "Checksum algorithm: MD5, SHA256, SHA384, SHA512. Default: SHA256.") String checksumAlgorithm,
            @ToolParam(required = false, description = "Optional checksum (hex). If omitted, checksum is computed from file.") String checksum) throws Exception {
        if (!StringUtils.isBlank(checksum)) {
            return errorJson("Checksum input is not supported yet. Omit checksum to let ThingsBoard compute it.");
        }
        String normalizedPath = normalizePathForWindows(filePath);
        Path path = Paths.get(normalizedPath);
        if (!Files.exists(path)) {
            return errorJson("File not found: " + normalizedPath);
        }
        byte[] bytes = Files.readAllBytes(path);
        String fileName = path.getFileName().toString();
        ChecksumAlgorithm algo = parseChecksumAlgorithm(checksumAlgorithm);
        String digest = null;
        return JacksonUtil.toString(clientService.getClient().saveOtaPackageData(new OtaPackageId(UUID.fromString(otaPackageId)), digest, algo, fileName, bytes));
    }

    @Tool(description = "Download OTA package binary to a local file path on the MCP host." + TENANT_AUTHORITY_PARAGRAPH)
    public String downloadOtaPackage(
            @ToolParam(description = "A string value representing the OTA package id.") @NotBlank String otaPackageId,
            @ToolParam(description = "Destination file path (or directory) on the MCP host.") @NotBlank String destinationPath) throws Exception {
        ResponseEntity<Resource> response = clientService.getClient().downloadOtaPackage(new OtaPackageId(UUID.fromString(otaPackageId)));
        Resource resource = response.getBody();
        if (resource == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "No data returned for OTA package download");
            return JacksonUtil.toString(err);
        }
        String normalizedPath = normalizePathForWindows(destinationPath);
        Path target = Paths.get(normalizedPath);
        if (Files.exists(target) && Files.isDirectory(target)) {
            String name = resource.getFilename() != null ? resource.getFilename() : (otaPackageId + ".bin");
            target = target.resolve(name);
        }
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (InputStream in = resource.getInputStream()) {
            Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("status", "OK");
        result.put("path", target.toString());
        return JacksonUtil.toString(result);
    }

    @Tool(description = "Get OTA package info by id." + TENANT_AUTHORITY_PARAGRAPH)
    public String getOtaPackageInfoById(
            @ToolParam(description = "A string value representing the OTA package id.") @NotBlank String otaPackageId) {
        return JacksonUtil.toString(clientService.getClient().getOtaPackageInfoById(new OtaPackageId(UUID.fromString(otaPackageId))));
    }

    @Tool(description = "Get OTA package by id." + TENANT_AUTHORITY_PARAGRAPH)
    public String getOtaPackageById(
            @ToolParam(description = "A string value representing the OTA package id.") @NotBlank String otaPackageId) {
        return JacksonUtil.toString(clientService.getClient().getOtaPackageById(new OtaPackageId(UUID.fromString(otaPackageId))));
    }

    @Tool(description = "Get OTA packages (paged). " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    public String getOtaPackages(
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the OTA package title.") String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'title', 'version', 'tag', 'name'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return JacksonUtil.toString(clientService.getClient().getOtaPackages(pageLink));
    }

    @Tool(description = "Get OTA packages by device profile and type (paged). " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    public String getOtaPackagesByDeviceProfile(
            @ToolParam(description = "A string value representing the device profile id.") @NotBlank String deviceProfileId,
            @ToolParam(description = "OTA package type. Allowed values: FIRMWARE or SOFTWARE.") @NotBlank String otaPackageType,
            @ToolParam(required = false, description = "Filter only packages that have data (default: true).") Boolean hasData,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = "The case insensitive 'substring' filter based on the OTA package title.") String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'title', 'version', 'tag', 'name'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        boolean hasDataValue = hasData == null || hasData;
        OtaPackageType type = OtaPackageType.valueOf(otaPackageType.trim().toUpperCase());
        try {
            return JacksonUtil.toString(clientService.getClient().getOtaPackages(new DeviceProfileId(UUID.fromString(deviceProfileId)), type, hasDataValue, pageLink));
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return errorJson("Endpoint not available on this ThingsBoard version. Use getOtaPackages and filter client-side.");
            }
            throw e;
        }
    }

    @Tool(description = "Count devices in a profile that do not have assigned OTA package." + TENANT_AUTHORITY_PARAGRAPH)
    public String countByDeviceProfileAndEmptyOtaPackage(
            @ToolParam(description = "A string value representing the device profile id.") @NotBlank String deviceProfileId,
            @ToolParam(description = "OTA package type. Allowed values: FIRMWARE or SOFTWARE.") @NotBlank String otaPackageType) {
        OtaPackageType type = OtaPackageType.valueOf(otaPackageType.trim().toUpperCase());
        long count = clientService.getClient().countByDeviceProfileAndEmptyOtaPackage(type, new DeviceProfileId(UUID.fromString(deviceProfileId)));
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return JacksonUtil.toString(result);
    }

    @Tool(description = "Assign OTA package to a device (firmware/software). If clear is true, clears assignment." +
            TENANT_AUTHORITY_PARAGRAPH)
    public String assignOtaPackageToDevice(
            @ToolParam(description = "A string value representing the device id.") @NotBlank String deviceId,
            @ToolParam(required = false, description = "A string value representing the OTA package id.") String otaPackageId,
            @ToolParam(description = "OTA package type. Allowed values: FIRMWARE or SOFTWARE.") @NotBlank String otaPackageType,
            @ToolParam(required = false, description = "If true, clears the OTA package assignment.") Boolean clear) {
        boolean doClear = clear != null && clear;
        if (!doClear && StringUtils.isBlank(otaPackageId)) {
            return errorJson("otaPackageId is required unless clear=true");
        }
        OtaPackageType type = OtaPackageType.valueOf(otaPackageType.trim().toUpperCase());
        Optional<Device> deviceOpt = clientService.getClient().getDeviceById(new DeviceId(UUID.fromString(deviceId)));
        if (deviceOpt.isEmpty()) {
            return errorJson("Device not found: " + deviceId);
        }
        Device device = deviceOpt.get();
        OtaPackageId pkgId = doClear ? null : new OtaPackageId(UUID.fromString(otaPackageId));
        if (type == OtaPackageType.FIRMWARE) {
            device.setFirmwareId(pkgId);
        } else {
            device.setSoftwareId(pkgId);
        }
        return JacksonUtil.toString(clientService.getClient().saveDevice(device));
    }

    @Tool(description = "Assign OTA package to a device profile (firmware/software). If clear is true, clears assignment." +
            TENANT_AUTHORITY_PARAGRAPH)
    public String assignOtaPackageToDeviceProfile(
            @ToolParam(description = "A string value representing the device profile id.") @NotBlank String deviceProfileId,
            @ToolParam(required = false, description = "A string value representing the OTA package id.") String otaPackageId,
            @ToolParam(description = "OTA package type. Allowed values: FIRMWARE or SOFTWARE.") @NotBlank String otaPackageType,
            @ToolParam(required = false, description = "If true, clears the OTA package assignment.") Boolean clear) {
        boolean doClear = clear != null && clear;
        if (!doClear && StringUtils.isBlank(otaPackageId)) {
            return errorJson("otaPackageId is required unless clear=true");
        }
        OtaPackageType type = OtaPackageType.valueOf(otaPackageType.trim().toUpperCase());
        Optional<DeviceProfile> profileOpt = clientService.getClient().getDeviceProfileById(new DeviceProfileId(UUID.fromString(deviceProfileId)));
        if (profileOpt.isEmpty()) {
            return errorJson("Device profile not found: " + deviceProfileId);
        }
        DeviceProfile profile = profileOpt.get();
        OtaPackageId pkgId = doClear ? null : new OtaPackageId(UUID.fromString(otaPackageId));
        if (type == OtaPackageType.FIRMWARE) {
            profile.setFirmwareId(pkgId);
        } else {
            profile.setSoftwareId(pkgId);
        }
        return JacksonUtil.toString(clientService.getClient().saveDeviceProfile(profile));
    }

    @Tool(description = "Delete OTA package by id." + TENANT_AUTHORITY_PARAGRAPH)
    public String deleteOtaPackage(
            @ToolParam(description = "A string value representing the OTA package id.") @NotBlank String otaPackageId) {
        clientService.getClient().deleteOtaPackage(new OtaPackageId(UUID.fromString(otaPackageId)));
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
