package org.thingsboard.ai.mcp.server.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.Arrays;
import java.util.UUID;

import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.ASSET_INFO_DESCRIPTION;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

@Service
public class AssetTools {

    @Autowired
    private RestClientService clientService;

    @Tool(description = "Fetch the Asset object based on the provided Asset Id. " +
            "If the user has the authority of 'Tenant Administrator', the server checks that the asset is owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the asset is assigned to the same customer." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getAssetById(String assetId) {
        return JacksonUtil.toString(clientService.getClient().getAssetById(new AssetId(UUID.fromString(assetId))));
    }

    @Tool(description = "Fetch the Asset Info object based on the provided Asset Id. " +
            "If the user has the authority of 'Tenant Administrator', the server checks that the asset is owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the asset is assigned to the same customer. "
            + ASSET_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getAssetInfoById(String assetId) {
        return JacksonUtil.toString(clientService.getClient().getAssetInfoById(new AssetId(UUID.fromString(assetId))));
    }

    @Tool(description = "Requested assets must be owned by tenant or assigned to customer which user is performing the request.")
    public String getAssetsByIds(String... assetIds) {
        return JacksonUtil.toString(clientService.getClient().getAssetsByIds(Arrays.stream(assetIds).map(UUID::fromString).map(AssetId::new).toList()));
    }

    @Tool(description = "Returns a page of assets info objects assigned to customer. " +
            PAGE_DATA_PARAMETERS + ASSET_INFO_DESCRIPTION)
    public String getCustomerAssetInfos(String customerId, String type, String assetProfileId, int pageSize, int page, String textSearch) {
        AssetProfileId profileId = assetProfileId != null ? new AssetProfileId(UUID.fromString(assetProfileId)) : null;
        var result = clientService.getClient().getCustomerAssetInfos(new CustomerId(UUID.fromString(customerId)), type, profileId, new PageLink(pageSize, page, textSearch));
        return JacksonUtil.toString(result.getData());
    }

    @Tool(description = "Returns a page of assets objects assigned to customer. " +
            PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    public String getCustomerAssets(String customerId, String type, int pageSize, int page, String textSearch) {
        var result = clientService.getClient().getCustomerAssets(new CustomerId(UUID.fromString(customerId)), new PageLink(pageSize, page, textSearch), type);
        return JacksonUtil.toString(result.getData());
    }

    @Tool(description = "Returns a page of assets assigned to edge. " + PAGE_DATA_PARAMETERS)
    public String getEdgeAssets(String edgeId, int pageSize, int page, String textSearch) {
        var result = clientService.getClient().getEdgeAssets(new EdgeId(UUID.fromString(edgeId)), new PageLink(pageSize, page, textSearch));
        return JacksonUtil.toString(result.getData());
    }

    @Tool(description = "Returns a page of assets info objects owned by tenant. " +
            PAGE_DATA_PARAMETERS + ASSET_INFO_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH)
    public String getTenantAssetInfos(String type, String assetProfileId, int pageSize, int page, String textSearch) {
        AssetProfileId profileId = assetProfileId != null ? new AssetProfileId(UUID.fromString(assetProfileId)) : null;
        var result = clientService.getClient().getTenantAssetInfos(type, profileId, new PageLink(pageSize, page, textSearch));
        return JacksonUtil.toString(result.getData());
    }

    @Tool(description = "Requested asset must be owned by tenant that the user belongs to. " +
            "Asset name is a unique property of asset. So it can be used to identify the asset." + TENANT_AUTHORITY_PARAGRAPH)
    public String getTenantAsset(String assetName) {
        return JacksonUtil.toString(clientService.getClient().getTenantAsset(assetName));
    }

    @Tool(description = "Returns a page of assets owned by tenant. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    public String getTenantAssets(String type, int pageSize, int page, String textSearch) {
        var result = clientService.getClient().getTenantAssets(new PageLink(pageSize, page, textSearch), type);
        return JacksonUtil.toString(result.getData());
    }
}
