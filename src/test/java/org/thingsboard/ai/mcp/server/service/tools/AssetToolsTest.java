package org.thingsboard.ai.mcp.server.service.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AssetToolsTest {

    @InjectMocks
    private AssetTools tools;

    @Mock
    private RestClientService clientService;

    @Mock
    private RestClient restClient;

    @Captor
    private ArgumentCaptor<PageLink> pageLinkCaptor;

    @BeforeEach
    void setUp() {
        when(clientService.getClient()).thenReturn(restClient);
    }

    @Test
    void testFindAssetById() {
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

    @ParameterizedTest(name = "tenantAssets page={1} size={0} type={2} sort={4} {5}")
    @CsvSource({
            "50,1,building,name,name,DESC",
            "25,0,,temp,createdTime,ASC"
    })
    void testFindTenantAssets(int pageSize, int page, String type, String text, String sortProperty, String sortDir) throws Exception {
        List<Asset> assets = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Asset asset = new Asset();
            asset.setId(new AssetId(UUID.randomUUID()));
            assets.add(asset);
        }

        PageData<Asset> pageData = new PageData<>(assets, 2, assets.size(), true);
        when(restClient.getTenantAssets(any(PageLink.class), eq(type))).thenReturn(pageData);

        String result = tools.getTenantAssets(Integer.toString(pageSize), Integer.toString(page), type, text, sortProperty, sortDir);

        verify(restClient).getTenantAssets(pageLinkCaptor.capture(), eq(type));
        PageLink pl = pageLinkCaptor.getValue();
        assertThat(pl.getPageSize()).isEqualTo(pageSize);
        assertThat(pl.getPage()).isEqualTo(page);
        assertThat(pl.getTextSearch()).isEqualTo(text);
        if (sortProperty != null) {
            assertThat(pl.getSortOrder()).isNotNull();
            assertThat(pl.getSortOrder().getProperty()).isEqualTo(sortProperty);
            assertThat(pl.getSortOrder().getDirection()).isEqualTo(SortOrder.Direction.valueOf(sortDir));
        }
        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @Test
    void testFindTenantAsset() {
        Asset asset = new Asset();
        asset.setId(new AssetId(UUID.randomUUID()));
        when(restClient.getTenantAsset("Boiler-01")).thenReturn(Optional.of(asset));

        String result = tools.getTenantAsset("Boiler-01");

        verify(restClient).getTenantAsset("Boiler-01");
        assertThat(result).isEqualTo(JacksonUtil.toString(asset));
    }

    @ParameterizedTest(name = "customerAssets page={1} size={0} type={3} text={4}")
    @CsvSource({
            "25,0,meter,plant,createdTime,ASC",
            "10,2,,pump,name,DESC"
    })
    void testFindCustomerAssets(int pageSize, int page, String type, String textSearch, String sortProp, String dir) throws Exception {
        UUID customerUuid = UUID.randomUUID();
        List<Asset> assets = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Asset asset = new Asset();
            asset.setId(new AssetId(UUID.randomUUID()));
            assets.add(asset);
        }

        PageData<Asset> pageData = new PageData<>(assets, 1, assets.size(), false);
        when(restClient.getCustomerAssets(any(CustomerId.class), any(PageLink.class), eq(type))).thenReturn(pageData);

        String result = tools.getCustomerAssets(customerUuid.toString(), Integer.toString(pageSize), Integer.toString(page), type, textSearch, sortProp, dir);

        ArgumentCaptor<CustomerId> customerCap = ArgumentCaptor.forClass(CustomerId.class);
        verify(restClient).getCustomerAssets(customerCap.capture(), pageLinkCaptor.capture(), eq(type));

        assertThat(customerCap.getValue().getId()).isEqualTo(customerUuid);

        PageLink pl = pageLinkCaptor.getValue();
        assertThat(pl.getPageSize()).isEqualTo(pageSize);
        assertThat(pl.getPage()).isEqualTo(page);
        assertThat(pl.getTextSearch()).isEqualTo(textSearch);
        if (sortProp != null) {
            assertThat(pl.getSortOrder()).isNotNull();
            assertThat(pl.getSortOrder().getProperty()).isEqualTo(sortProp);
            assertThat(pl.getSortOrder().getDirection()).isEqualTo(SortOrder.Direction.valueOf(dir));
        }

        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @ParameterizedTest(name = "userAssets page={1} size={0} type={2} sort={4} {5}")
    @CsvSource({
            "15,2,pump,abc,createdTime,DESC",
            "5,0,,room,name,ASC"
    })
    void testFindUserAssets(int pageSize, int page, String type, String text, String sortProp, String dir) throws Exception {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.PE);
        when(clientService.getClient()).thenReturn(restClient);

        List<Asset> assets = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Asset asset = new Asset();
            asset.setId(new AssetId(UUID.randomUUID()));
            assets.add(asset);
        }

        PageData<Asset> pageData = new PageData<>(assets, 1, assets.size(), false);
        when(restClient.getUserAssets(eq(type), any(PageLink.class))).thenReturn(pageData);

        String result = tools.getUserAssets(Integer.toString(pageSize), Integer.toString(page), type, text, sortProp, dir);

        verify(restClient).getUserAssets(eq(type), pageLinkCaptor.capture());
        PageLink pl = pageLinkCaptor.getValue();
        assertThat(pl.getPageSize()).isEqualTo(pageSize);
        assertThat(pl.getPage()).isEqualTo(page);
        assertThat(pl.getTextSearch()).isEqualTo(text);
        if (sortProp != null) {
            assertThat(pl.getSortOrder()).isNotNull();
            assertThat(pl.getSortOrder().getProperty()).isEqualTo(sortProp);
            assertThat(pl.getSortOrder().getDirection()).isEqualTo(SortOrder.Direction.valueOf(dir));
        }

        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @ParameterizedTest(name = "assetsByGroup page={1} size={0} sort={4} {5}")
    @CsvSource({
            "20,1,email,ASC,xyz",
            "5,0,firstName,DESC,"
    })
    void testFindAssetsByEntityGroupId(int pageSize, int page, String sortProp, String dir, String text) throws Exception {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.PE);
        when(clientService.getClient()).thenReturn(restClient);

        UUID groupUuid = UUID.randomUUID();
        List<Asset> assets = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Asset asset = new Asset();
            asset.setId(new AssetId(UUID.randomUUID()));
            assets.add(asset);
        }

        PageData<Asset> pageData = new PageData<>(assets, 1, assets.size(), false);
        when(restClient.getAssetsByEntityGroupId(any(EntityGroupId.class), any(PageLink.class))).thenReturn(pageData);

        String result = tools.getAssetsByEntityGroupId(groupUuid.toString(), Integer.toString(pageSize), Integer.toString(page), text, sortProp, dir);

        ArgumentCaptor<EntityGroupId> groupCap = ArgumentCaptor.forClass(EntityGroupId.class);
        verify(restClient).getAssetsByEntityGroupId(groupCap.capture(), pageLinkCaptor.capture());

        assertThat(groupCap.getValue().getId()).isEqualTo(groupUuid);

        PageLink pl = pageLinkCaptor.getValue();
        assertThat(pl.getPageSize()).isEqualTo(pageSize);
        assertThat(pl.getPage()).isEqualTo(page);
        assertThat(pl.getTextSearch()).isEqualTo(text);
        assertThat(pl.getSortOrder()).isNotNull();
        assertThat(pl.getSortOrder().getProperty()).isEqualTo(sortProp);
        assertThat(pl.getSortOrder().getDirection()).isEqualTo(SortOrder.Direction.valueOf(dir));

        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @Nested
    @DisplayName("saveAsset variants")
    class SaveAssetVariants {
        @Test
        void testSaveAsset_withoutGroups() {
            Asset payload = new Asset();
            payload.setName("Room-234");
            when(restClient.saveAsset(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

            String res = tools.saveAsset(JacksonUtil.toString(payload), null, null);

            ArgumentCaptor<Asset> assetCap = ArgumentCaptor.forClass(Asset.class);
            verify(restClient).saveAsset(assetCap.capture());
            assertThat(assetCap.getValue().getName()).isEqualTo("Room-234");
            assertThat(res).isEqualTo(JacksonUtil.toString(payload));
        }

        @Test
        void testSaveAsset_withSingleGroup() {
            Asset payload = new Asset();
            payload.setName("Room-1");
            UUID groupId = UUID.randomUUID();

            when(restClient.saveAsset(any(Asset.class), any(EntityGroupId.class), eq(null)))
                    .thenAnswer(inv -> inv.getArgument(0));

            String res = tools.saveAsset(JacksonUtil.toString(payload), groupId.toString(), null);

            ArgumentCaptor<EntityGroupId> groupCap = ArgumentCaptor.forClass(EntityGroupId.class);
            verify(restClient).saveAsset(any(Asset.class), groupCap.capture(), eq(null));
            assertThat(groupCap.getValue().getId()).isEqualTo(groupId);
            assertThat(res).isEqualTo(JacksonUtil.toString(payload));
        }

        @Test
        void testSaveAsset_withMultipleGroups() {
            Asset payload = new Asset();
            payload.setName("Room-2");
            String groupIds = UUID.randomUUID() + "," + UUID.randomUUID();

            when(restClient.saveAsset(any(Asset.class), eq(null), eq(groupIds)))
                    .thenAnswer(inv -> inv.getArgument(0));

            String res = tools.saveAsset(JacksonUtil.toString(payload), null, groupIds);

            verify(restClient).saveAsset(any(Asset.class), eq(null), eq(groupIds));
            assertThat(res).isEqualTo(JacksonUtil.toString(payload));
        }

    }

    @Nested
    @DisplayName("deleteAsset JSON contract")
    class DeleteAssetContract {
        @Test
        void testDeleteAsset_ok() {
            UUID id = UUID.randomUUID();
            String res = tools.deleteAsset(id.toString());

            ArgumentCaptor<AssetId> idCap = ArgumentCaptor.forClass(AssetId.class);
            verify(restClient).deleteAsset(idCap.capture());
            assertThat(idCap.getValue().getId()).isEqualTo(id);

            assertThat(res).contains("\"status\":\"OK\"");
            assertThat(res).contains(id.toString());
        }

        @Test
        void testDeleteAsset_error() {
            UUID id = UUID.randomUUID();
            doThrow(new RuntimeException("boom")).when(restClient).deleteAsset(any(AssetId.class));

            String res = tools.deleteAsset(id.toString());

            assertThat(res).contains("\"status\":\"ERROR\"");
            assertThat(res).contains(id.toString());
            assertThat(res).contains("boom");
        }

    }

}
