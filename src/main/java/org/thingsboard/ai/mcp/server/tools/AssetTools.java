package org.thingsboard.ai.mcp.server.tools;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;

import java.util.Arrays;
import java.util.UUID;

import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.ASSET_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.ASSET_INFO_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.ASSET_NAME_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.ASSET_PROFILE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.ASSET_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.ASSET_TYPE_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.EDGE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.ai.mcp.server.tools.ToolUtils.createPageLink;
import static org.thingsboard.ai.mcp.server.tools.ToolUtils.createTimePageLink;

@Service
@RequiredArgsConstructor
public class AssetTools {

    private final RestClientService clientService;

    @Tool(description = "Get the Asset object based on the provided Asset Id. " +
            "If the user has the authority of 'Tenant Administrator', the server checks that the asset is owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the asset is assigned to the same customer." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getAssetById(@ToolParam(description = ASSET_ID_PARAM_DESCRIPTION) String assetId) {
        return JacksonUtil.toString(clientService.getClient().getAssetById(new AssetId(UUID.fromString(assetId))));
    }

    @Tool(description = "Fetch the Asset Info object based on the provided Asset Id. " +
            "If the user has the authority of 'Tenant Administrator', the server checks that the asset is owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the asset is assigned to the same customer. "
            + ASSET_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getAssetInfoById(@ToolParam(description = ASSET_ID_PARAM_DESCRIPTION) String assetId) {
        return JacksonUtil.toString(clientService.getClient().getAssetInfoById(new AssetId(UUID.fromString(assetId))));
    }

    @Tool(description = "Returns a page of assets owned by tenant. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    public String getTenantAssets(
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) int pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) int page,
            @ToolParam(required = false, description = ASSET_TYPE_DESCRIPTION) String type,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'name', 'type', 'label', 'customerTitle'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return JacksonUtil.toString(clientService.getClient().getTenantAssets(pageLink, type).getData());
    }

    @Tool(description = "Returns a page of assets info objects owned by tenant. " + PAGE_DATA_PARAMETERS + ASSET_INFO_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH)
    public String getTenantAssetInfos(
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) int pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) int page,
            @ToolParam(required = false, description = ASSET_TYPE_DESCRIPTION) String type,
            @ToolParam(required = false, description = ASSET_PROFILE_ID_PARAM_DESCRIPTION) String assetProfileId,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'name', 'type', 'label', 'clearTs', 'customerTitle'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        AssetProfileId profileId = assetProfileId != null ? new AssetProfileId(UUID.fromString(assetProfileId)) : null;
        return JacksonUtil.toString(clientService.getClient().getTenantAssetInfos(type, profileId, pageLink).getData());
    }

    @Tool(description = "Get tenant asset. Requested asset must be owned by tenant that the user belongs to. " +
            "Asset name is a unique property of asset. So it can be used to identify the asset." + TENANT_AUTHORITY_PARAGRAPH)
    public String getTenantAsset(@ToolParam(description = ASSET_NAME_DESCRIPTION) String assetName) {
        return JacksonUtil.toString(clientService.getClient().getTenantAsset(assetName));
    }

    @Tool(description = "Returns a page of assets objects assigned to customer. " + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getCustomerAssets(
            @ToolParam(description = CUSTOMER_ID_PARAM_DESCRIPTION) String customerId,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) int pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) int page,
            @ToolParam(required = false, description = ASSET_TYPE_DESCRIPTION) String type,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'name', 'type', 'label', 'clearTs', 'customerTitle'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return JacksonUtil.toString(clientService.getClient().getCustomerAssets(new CustomerId(UUID.fromString(customerId)), pageLink, type).getData());
    }

    @Tool(description = "Returns a page of assets info objects assigned to customer. " + PAGE_DATA_PARAMETERS + ASSET_INFO_DESCRIPTION)
    public String getCustomerAssetInfos(
            @ToolParam(description = CUSTOMER_ID_PARAM_DESCRIPTION) String customerId,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) int pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) int page,
            @ToolParam(required = false, description = ASSET_TYPE_DESCRIPTION) String type,
            @ToolParam(required = false, description = ASSET_PROFILE_ID_PARAM_DESCRIPTION) String assetProfileId,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'name', 'type', 'label', 'clearTs', 'customerTitle'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        AssetProfileId profileId = assetProfileId != null ? new AssetProfileId(UUID.fromString(assetProfileId)) : null;
        return JacksonUtil.toString(clientService.getClient().getCustomerAssetInfos(new CustomerId(UUID.fromString(customerId)), type, profileId, pageLink).getData());
    }

    @Tool(description = "Get Assets By Ids. Requested assets must be owned by tenant or assigned to customer which user is performing the request. " + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getAssetsByIds(@ToolParam(description = "A list of assets ids, separated by comma ','") String... assetIds) {
        return JacksonUtil.toString(clientService.getClient().getAssetsByIds(Arrays.stream(assetIds).map(UUID::fromString).map(AssetId::new).toList()));
    }

    @Tool(description = "Returns a page of assets assigned to edge. " + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getEdgeAssets(
            @ToolParam(description = EDGE_ID_PARAM_DESCRIPTION) String edgeId,
            @ToolParam(description = PAGE_SIZE_DESCRIPTION) int pageSize,
            @ToolParam(description = PAGE_NUMBER_DESCRIPTION) int page,
            @ToolParam(required = false, description = ASSET_TYPE_DESCRIPTION) String type,
            @ToolParam(required = false, description = ASSET_TEXT_SEARCH_DESCRIPTION) String textSearch,
            @ToolParam(required = false, description = SORT_PROPERTY_DESCRIPTION + ". Allowed values: 'createdTime', 'name', 'type', 'label', 'clearTs', 'customerTitle'") String sortProperty,
            @ToolParam(required = false, description = SORT_ORDER_DESCRIPTION) String sortOrder,
            @ToolParam(required = false, description = "Timestamp. Assets with creation time before it won't be queried") long startTime,
            @ToolParam(required = false, description = "Timestamp. Assets with creation time after it won't be queried") long endTime) throws ThingsboardException {
        TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
        var result = clientService.getClient().getEdgeAssets(new EdgeId(UUID.fromString(edgeId)), pageLink);
        return JacksonUtil.toString(result.getData());
    }

}
