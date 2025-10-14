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
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PE_ONLY_AVAILABLE;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.RBAC_GROUP_READ_CHECK;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.ai.mcp.server.util.ToolUtils.createPageLink;

@Service
@RequiredArgsConstructor
public class AssetTools implements McpTools {

    private static final String ASSET_JSON_EXAMPLE = """
            ```json
            {
              "id": { "entityType": "ASSET", "id": "d3cfc080-a295-11f0-848c-93db0ade7d93" },
              "tenantId": { "entityType": "TENANT", "id": "d3cfc080-a295-11f0-848c-93db0ade7d93" },
              "customerID": { "entityType": "CUSTOMER", "id": "d3cfc080-a295-11f0-848c-93db0ade7d93" },
              "name": "Room-234",
              "label": "Room 234 Sensor",
              "type": "building-zone",
              "assetProfileId": { "entityType": "ASSET_PROFILE", "id": "716a92d0-9d36-11f0-a79c-e726b4e8048a" }
            }
            ```
            """;

    private final RestClientService clientService;

    @Tool(description =
            "Create or update an Asset. Remove 'id', 'tenantId' and optionally 'customerId' from the request body to create new Asset entity. " +
                    "If 'id' is provided, the existing Asset is updated. Asset names must be unique within a tenant \n" +

                    "### Asset Profile selection logic:\n" +
                    "- You can define the Asset’s profile either by providing **assetProfileId** or by specifying a **type**.\n" +
                    "- If both `assetProfileId` and `type` are provided, **assetProfileId takes precedence** — the platform will use the profile referenced by that ID.\n" +
                    "- If only `type` is provided, the platform will find or create a profile with that type name.\n" +
                    "- If neither is specified, the platform automatically assigns the **default Asset Profile** of the tenant.\n\n" +

                    "### Platform Edition:\n" +
                    "- In ThingsBoard PE, you can also attach the Asset to an entity group using the `entityGroupId` parameter or to multiple groups" +
                    "using `entityGroupIds` parameter.\n" +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String saveAsset(
            @ToolParam(description = "A JSON string representing the Asset entity. Omit 'id' to create a new Asset; include 'id' to update an existing one." + ASSET_JSON_EXAMPLE)
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

    @Tool(description = "Delete the asset. Deletes the asset and all the relations ('from' and 'to' the asset). " +
            "Referencing non-existing asset Id will cause an error. " + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
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

    @Tool(description = "Get the Asset object based on the provided Asset Id. " +
            "If the user has the authority of 'Tenant Administrator', the server checks that the asset is owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the asset is assigned to the same customer." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getAssetById(@ToolParam(description = ASSET_ID_PARAM_DESCRIPTION) @NotBlank String assetId) {
        return JacksonUtil.toString(clientService.getClient().getAssetById(new AssetId(UUID.fromString(assetId))));
    }

    @Tool(description = "Returns a page of assets owned by tenant. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
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

    @Tool(description = "Get tenant asset. Requested asset must be owned by tenant that the user belongs to. " +
            "Asset name is a unique property of asset. So it can be used to identify the asset." + TENANT_AUTHORITY_PARAGRAPH)
    public String getTenantAsset(@NotBlank @ToolParam(description = ASSET_NAME_DESCRIPTION) String assetName) {
        return JacksonUtil.toString(clientService.getClient().getTenantAsset(assetName));
    }

    @Tool(description = "Returns a page of assets objects assigned to customer. " + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
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
    @Tool(description = "Returns a page of assets objects available for the current user. " + PE_ONLY_AVAILABLE + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
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
    @Tool(description = "Returns a page of asset objects that belongs to specified Entity Group Id. " + PE_ONLY_AVAILABLE + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_GROUP_READ_CHECK)
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
