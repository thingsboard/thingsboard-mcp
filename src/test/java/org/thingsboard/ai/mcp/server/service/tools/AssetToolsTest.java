package org.thingsboard.ai.mcp.server.service.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.ai.mcp.server.data.ThingsBoardEdition;
import org.thingsboard.ai.mcp.server.rest.RestClient;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.asset.AssetTools;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PE_ONLY_AVAILABLE;

@ExtendWith(MockitoExtension.class)
public class AssetToolsTest {

    @InjectMocks
    private AssetTools tools;

    @Mock
    private RestClientService clientService;

    @Mock
    private RestClient restClient;

    @Test
    void testFindAssetById() {
        when(clientService.getClient()).thenReturn(restClient);

        UUID assetUuid = UUID.randomUUID();
        Asset asset = new Asset();
        asset.setId(new AssetId(assetUuid));
        when(restClient.getAssetById(any(AssetId.class))).thenReturn(Optional.of(asset));

        String result = tools.getAssetById(assetUuid.toString());

        ArgumentCaptor<AssetId> idCap = ArgumentCaptor.forClass(AssetId.class);
        verify(restClient).getAssetById(idCap.capture());

        assertThat(idCap.getValue().getId()).isEqualTo(assetUuid);
        assertThat(result).isEqualTo(JacksonUtil.toString(asset));
    }

    @Test
    void testFindTenantAssets() throws Exception {
        when(clientService.getClient()).thenReturn(restClient);

        List<Asset> assets = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Asset asset = new Asset();
            asset.setId(new AssetId(UUID.randomUUID()));
            assets.add(asset);
        }

        PageData<Asset> page = new PageData<>(assets, 2, assets.size(), true);
        when(restClient.getTenantAssets(any(PageLink.class), eq("building"))).thenReturn(page);

        String result = tools.getTenantAssets(50, 1, "building", "name", "name", "DESC");

        ArgumentCaptor<PageLink> pageCap = ArgumentCaptor.forClass(PageLink.class);
        verify(restClient).getTenantAssets(pageCap.capture(), eq("building"));

        PageLink pageLink = pageCap.getValue();
        assertThat(pageLink.getPageSize()).isEqualTo(50);
        assertThat(pageLink.getPage()).isEqualTo(1);
        assertThat(pageLink.getTextSearch()).isEqualTo("name");
        assertThat(pageLink.getSortOrder().getDirection().name()).isEqualTo("DESC");

        assertThat(result).isEqualTo(JacksonUtil.toString(page));
    }

    @Test
    void testFindTenantAsset() {
        when(clientService.getClient()).thenReturn(restClient);

        Asset asset = new Asset();
        asset.setId(new AssetId(UUID.randomUUID()));
        when(restClient.getTenantAsset("Boiler-01")).thenReturn(Optional.of(asset));

        String result = tools.getTenantAsset("Boiler-01");

        verify(restClient).getTenantAsset("Boiler-01");
        assertThat(result).isEqualTo(JacksonUtil.toString(asset));
    }

    @Test
    void testFindCustomerAssets() throws Exception {
        when(clientService.getClient()).thenReturn(restClient);

        UUID customerUuid = UUID.randomUUID();
        List<Asset> assets = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Asset asset = new Asset();
            asset.setId(new AssetId(UUID.randomUUID()));
            assets.add(asset);
        }

        PageData<Asset> page = new PageData<>(assets, 1, assets.size(), false);
        when(restClient.getCustomerAssets(any(CustomerId.class), any(PageLink.class), eq("meter"))).thenReturn(page);

        String result = tools.getCustomerAssets(customerUuid.toString(), 25, 0, "meter", "plant", null, null);

        ArgumentCaptor<CustomerId> customerCap = ArgumentCaptor.forClass(CustomerId.class);
        ArgumentCaptor<PageLink> pageCap = ArgumentCaptor.forClass(PageLink.class);
        verify(restClient).getCustomerAssets(customerCap.capture(), pageCap.capture(), eq("meter"));

        assertThat(customerCap.getValue().getId()).isEqualTo(customerUuid);

        PageLink pageLink = pageCap.getValue();
        assertThat(pageLink.getPageSize()).isEqualTo(25);
        assertThat(pageLink.getPage()).isEqualTo(0);
        assertThat(pageLink.getTextSearch()).isEqualTo("plant");

        assertThat(result).isEqualTo(JacksonUtil.toString(page));
    }

    @Test
    void testFindUserAssets_ceEdition() throws Exception {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.CE);

        String result = tools.getUserAssets(10, 0, null, null, null, null);

        verify(clientService, never()).getClient();
        assertThat(result).isEqualTo(PE_ONLY_AVAILABLE);
    }

    @Test
    void testFindUserAssets_peEdition() throws Exception {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.PE);
        when(clientService.getClient()).thenReturn(restClient);

        List<Asset> assets = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Asset asset = new Asset();
            asset.setId(new AssetId(UUID.randomUUID()));
            assets.add(asset);
        }

        PageData<Asset> page = new PageData<>(assets, 1, assets.size(), false);
        when(restClient.getUserAssets(eq("pump"), any(PageLink.class))).thenReturn(page);

        String result = tools.getUserAssets(15, 2, "pump", "abc", "createdTime", "DESC");

        ArgumentCaptor<PageLink> pageCap = ArgumentCaptor.forClass(PageLink.class);
        verify(restClient).getUserAssets(eq("pump"), pageCap.capture());

        PageLink pageLink = pageCap.getValue();
        assertThat(pageLink.getPageSize()).isEqualTo(15);
        assertThat(pageLink.getPage()).isEqualTo(2);
        assertThat(pageLink.getTextSearch()).isEqualTo("abc");
        assertThat(pageLink.getSortOrder().getDirection().name()).isEqualTo("DESC");

        assertThat(result).isEqualTo(JacksonUtil.toString(page));
    }

    @Test
    void testFindAssetsByIds() {
        when(clientService.getClient()).thenReturn(restClient);

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        Asset asset1 = new Asset(); asset1.setId(new AssetId(id1));
        Asset asset2 = new Asset(); asset2.setId(new AssetId(id2));

        List<Asset> assets = List.of(asset1, asset2);
        when(restClient.getAssetsByIds(anyList())).thenReturn(assets);

        String result = tools.getAssetsByIds(id1.toString(), id2.toString());

        ArgumentCaptor<List> listCap = ArgumentCaptor.forClass(List.class);
        verify(restClient).getAssetsByIds(listCap.capture());

        @SuppressWarnings("unchecked")
        List<AssetId> passedIds = (List<AssetId>) listCap.getValue();
        assertThat(passedIds).extracting(UUIDBased::getId).containsExactlyInAnyOrder(id1, id2);

        assertThat(result).isEqualTo(JacksonUtil.toString(assets));
    }

    @Test
    void testFindAssetsByEntityGroupId_ceEdition() throws Exception {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.CE);

        String result = tools.getAssetsByEntityGroupId(UUID.randomUUID().toString(), 10, 0, null, null, null);

        verify(clientService, never()).getClient();
        assertThat(result).isEqualTo(PE_ONLY_AVAILABLE);
    }

    @Test
    void testFindAssetsByEntityGroupId_peEdition() throws Exception {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.PE);
        when(clientService.getClient()).thenReturn(restClient);

        UUID groupUuid = UUID.randomUUID();
        List<Asset> assets = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Asset asset = new Asset();
            asset.setId(new AssetId(UUID.randomUUID()));
            assets.add(asset);
        }

        PageData<Asset> page = new PageData<>(assets, 1, assets.size(), false);
        when(restClient.getAssetsByEntityGroupId(any(EntityGroupId.class), any(PageLink.class))).thenReturn(page);

        String result = tools.getAssetsByEntityGroupId(groupUuid.toString(), 20, 1, "xyz", "email", "ASC");

        ArgumentCaptor<EntityGroupId> groupCap = ArgumentCaptor.forClass(EntityGroupId.class);
        ArgumentCaptor<PageLink> pageCap = ArgumentCaptor.forClass(PageLink.class);
        verify(restClient).getAssetsByEntityGroupId(groupCap.capture(), pageCap.capture());

        assertThat(groupCap.getValue().getId()).isEqualTo(groupUuid);

        PageLink pageLink = pageCap.getValue();
        assertThat(pageLink.getPageSize()).isEqualTo(20);
        assertThat(pageLink.getPage()).isEqualTo(1);
        assertThat(pageLink.getTextSearch()).isEqualTo("xyz");
        assertThat(pageLink.getSortOrder().getDirection().name()).isEqualTo("ASC");

        assertThat(result).isEqualTo(JacksonUtil.toString(page));
    }

}
