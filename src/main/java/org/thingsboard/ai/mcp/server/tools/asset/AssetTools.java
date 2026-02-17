package org.thingsboard.ai.mcp.server.tools.asset;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.annotation.PeOnly;
import org.thingsboard.ai.mcp.server.annotation.ToolGroup;
import org.thingsboard.ai.mcp.server.data.ThingsBoardEdition;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.McpTools;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ASSET_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ASSET_NAME_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ASSET_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ASSET_TYPE_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.CUSTOMER_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_GROUP_IDS_CREATE_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_GROUP_ID_CREATE_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.ENTITY_GROUP_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PE_ONLY_AVAILABLE;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.util.ToolUtils.createPageLink;

@Service
@RequiredArgsConstructor
@ToolGroup("asset")
public class AssetTools implements McpTools {

    private static final String ASSET_JSON_EXAMPLE = """
            {
              "name": "Room-234",
              "label": "Room 234 Sensor",
              "type": "building-zone",
              "assetProfileId": {"entityType": "ASSET_PROFILE", "id": "<profileId>"}
            }""";

    private final RestClientService clientService;

    @Tool(description = "Use this to create or update an asset. Omit 'id' to create; include 'id' to update. Asset names are unique per tenant. " +
            "Profile selection: 'assetProfileId' takes precedence over 'type'. If neither set, default profile is used. " +
            "PE: use 'entityGroupId'/'entityGroupIds' to attach to groups.")
    public String saveAsset(
            @ToolParam(description = "JSON asset object. Omit 'id' to create; include 'id' to update. " + ASSET_JSON_EXAMPLE)
            @NotBlank @Valid String assetJson,
            @ToolParam(required = false, description = "(PE only) " + ENTITY_GROUP_ID_CREATE_PARAM_DESCRIPTION)
            String entityGroupId,
            @ToolParam(required = false, description = "(PE only) " + ENTITY_GROUP_IDS_CREATE_PARAM_DESCRIPTION)
            String entityGroupIds) {
        Asset asset = JacksonUtil.fromString(assetJson, Asset.class);
        if (StringUtils.isNotBlank(entityGroupId)) {
            return JacksonUtil.toString(clientService.getClient().saveAsset(asset, new EntityGroupId(UUID.fromString(entityGroupId)), null));
        } else if (StringUtils.isNotBlank(entityGroupIds)) {
            return JacksonUtil.toString(clientService.getClient().saveAsset(asset, null, entityGroupIds));
        } else {
            return JacksonUtil.toString(clientService.getClient().saveAsset(asset));
        }
    }

    @Tool(description = "Use this to permanently delete an asset and all its relations by id.")
    public String deleteAsset(@ToolParam(description = ASSET_ID_PARAM_DESCRIPTION) @NotBlank @Valid String assetIdStr) {
        try {
            AssetId assetId = new AssetId(UUID.fromString(assetIdStr));
            clientService.getClient().deleteAsset(assetId);
            return "{\"status\":\"OK\",\"id\":\"" + assetId + "\"}";
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("id", assetIdStr);
            err.put("message", e.getMessage());
            return JacksonUtil.toString(err);
        }
    }

    @Tool(description = "Use this to get an asset by its id.")
    public String getAssetById(@ToolParam(description = ASSET_ID_PARAM_DESCRIPTION) @NotBlank String assetId) {
        return JacksonUtil.toString(clientService.getClient().getAssetById(new AssetId(UUID.fromString(assetId))));
    }

    @Tool(description = "Use this to get a paginated list of assets owned by the tenant. Filter by type.")
    public String getTenantAssets(
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = ASSET_TYPE_DESCRIPTION) String type,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'name', 'type', 'label', 'customerTitle'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return JacksonUtil.toString(clientService.getClient().getTenantAssets(pageLink, type));
    }

    @Tool(description = "Use this to get an asset by its unique name within the tenant.")
    public String getTenantAsset(@NotBlank @ToolParam(description = ASSET_NAME_DESCRIPTION) String assetName) {
        return JacksonUtil.toString(clientService.getClient().getTenantAsset(assetName));
    }

    @Tool(description = "Use this to get a paginated list of assets assigned to a specific customer. Filter by type.")
    public String getCustomerAssets(
            @ToolParam(description = CUSTOMER_ID_PARAM_DESCRIPTION) @NotBlank String customerId,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = ASSET_TYPE_DESCRIPTION) String type,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'name', 'type', 'label', 'customerTitle'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return JacksonUtil.toString(clientService.getClient().getCustomerAssets(new CustomerId(UUID.fromString(customerId)), pageLink, type));
    }

    @PeOnly
    @Tool(description = "Use this to get a paginated list of assets available to the current user. PE only.")
    public String getUserAssets(
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = ASSET_TYPE_DESCRIPTION) String type,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'name', 'type', 'label', 'customerTitle'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        if (ThingsBoardEdition.CE == clientService.getEdition()) {
            return PE_ONLY_AVAILABLE;
        }
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return JacksonUtil.toString(clientService.getClient().getUserAssets(type, pageLink));
    }

    @PeOnly
    @Tool(description = "Use this to get a paginated list of assets in a specific entity group. PE only.")
    public String getAssetsByEntityGroupId(
            @ToolParam(description = ENTITY_GROUP_ID_PARAM_DESCRIPTION) @NotBlank String entityGroupId,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) @Positive String pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) @PositiveOrZero String page,
            @ToolParam(required = false, description = CUSTOMER_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'firstName', 'lastName', 'email'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        if (ThingsBoardEdition.CE == clientService.getEdition()) {
            return PE_ONLY_AVAILABLE;
        }
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return JacksonUtil.toString(clientService.getClient().getAssetsByEntityGroupId(new EntityGroupId(UUID.fromString(entityGroupId)), pageLink));
    }

}
