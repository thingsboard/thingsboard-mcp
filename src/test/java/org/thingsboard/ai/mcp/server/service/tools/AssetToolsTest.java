package org.thingsboard.ai.mcp.server.service.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.ai.mcp.server.data.ThingsBoardEdition;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.asset.AssetTools;
import org.thingsboard.ai.mcp.server.util.JsonUtils;
import org.thingsboard.client.ThingsboardClient;
import org.thingsboard.client.model.Asset;
import org.thingsboard.client.model.PageDataAsset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
    private ThingsboardClient restClient;

    @BeforeEach
    void setUp() {
        when(clientService.getClient()).thenReturn(restClient);
    }

    @Test
    void testFindAssetById() {
        String assetId = UUID.randomUUID().toString();
        Asset asset = new Asset().name("test-asset");
        when(restClient.getAssetById(anyString())).thenReturn(asset);

        String result = tools.getAssetById(assetId);

        verify(restClient).getAssetById(eq(assetId));
        assertThat(result).isEqualTo(JsonUtils.toString(asset));
    }

    @ParameterizedTest(name = "tenantAssets page={1} size={0} type={2} sort={4} {5}")
    @CsvSource({
            "50,1,building,name,name,DESC",
            "25,0,,temp,createdTime,ASC"
    })
    void testFindTenantAssets(int pageSize, int page, String type, String text, String sortProperty, String sortDir) throws Exception {
        List<Asset> assets = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            assets.add(new Asset().name("asset-" + i));
        }

        PageDataAsset pageData = new PageDataAsset(2, (long) assets.size(), true).data(assets);
        when(restClient.getTenantAssets(anyInt(), anyInt(), any(), any(), any(), any())).thenReturn(pageData);

        String result = tools.getTenantAssets(Integer.toString(pageSize), Integer.toString(page), type, text, sortProperty, sortDir);

        verify(restClient).getTenantAssets(eq(pageSize), eq(page), eq(type), eq(text), eq(sortProperty), eq(sortDir));
        assertThat(result).isEqualTo(JsonUtils.toString(pageData));
    }

    @Test
    void testFindTenantAsset() {
        Asset asset = new Asset().name("Boiler-01");
        when(restClient.getTenantAssetByName("Boiler-01")).thenReturn(asset);

        String result = tools.getTenantAsset("Boiler-01");

        verify(restClient).getTenantAssetByName("Boiler-01");
        assertThat(result).isEqualTo(JsonUtils.toString(asset));
    }

    @ParameterizedTest(name = "customerAssets page={1} size={0} type={3} text={4}")
    @CsvSource({
            "25,0,meter,plant,createdTime,ASC",
            "10,2,,pump,name,DESC"
    })
    void testFindCustomerAssets(int pageSize, int page, String type, String textSearch, String sortProp, String dir) throws Exception {
        String customerId = UUID.randomUUID().toString();
        List<Asset> assets = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            assets.add(new Asset().name("asset-" + i));
        }

        PageDataAsset pageData = new PageDataAsset(1, (long) assets.size(), false).data(assets);
        when(restClient.getCustomerAssets(anyString(), anyInt(), anyInt(), any(), any(), any(), any())).thenReturn(pageData);

        String result = tools.getCustomerAssets(customerId, Integer.toString(pageSize), Integer.toString(page), type, textSearch, sortProp, dir);

        verify(restClient).getCustomerAssets(eq(customerId), eq(pageSize), eq(page), eq(type), eq(textSearch), eq(sortProp), eq(dir));
        assertThat(result).isEqualTo(JsonUtils.toString(pageData));
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
            assets.add(new Asset().name("asset-" + i));
        }

        PageDataAsset pageData = new PageDataAsset(1, (long) assets.size(), false).data(assets);
        when(restClient.getUserAssets(any(), any(), any(), any(), any(), any(), any())).thenReturn(pageData);

        String result = tools.getUserAssets(Integer.toString(pageSize), Integer.toString(page), type, text, sortProp, dir);

        verify(restClient).getUserAssets(any(), any(), any(), any(), any(), any(), any());
        assertThat(result).isEqualTo(JsonUtils.toString(pageData));
    }

    @ParameterizedTest(name = "assetsByGroup page={1} size={0} sort={4} {5}")
    @CsvSource({
            "20,1,email,ASC,xyz",
            "5,0,firstName,DESC,"
    })
    void testFindAssetsByEntityGroupId(int pageSize, int page, String sortProp, String dir, String text) throws Exception {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.PE);
        when(clientService.getClient()).thenReturn(restClient);

        String groupId = UUID.randomUUID().toString();
        List<Asset> assets = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            assets.add(new Asset().name("asset-" + i));
        }

        PageDataAsset pageData = new PageDataAsset(1, (long) assets.size(), false).data(assets);
        when(restClient.getAssetsByEntityGroupId(anyString(), any(), any(), any(), any(), any())).thenReturn(pageData);

        String result = tools.getAssetsByEntityGroupId(groupId, Integer.toString(pageSize), Integer.toString(page), text, sortProp, dir);

        verify(restClient).getAssetsByEntityGroupId(eq(groupId), any(), any(), any(), any(), any());
        assertThat(result).isEqualTo(JsonUtils.toString(pageData));
    }

    @Nested
    @DisplayName("saveAsset variants")
    class SaveAssetVariants {
        @Test
        void testSaveAsset_withoutGroups() {
            Asset payload = new Asset().name("Room-234");
            when(restClient.saveAsset(any(Asset.class), isNull(), isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(payload);

            String res = tools.saveAsset(JsonUtils.toString(payload), null, null);

            verify(restClient).saveAsset(any(Asset.class), isNull(), isNull(), isNull(), isNull(), isNull());
            assertThat(res).isEqualTo(JsonUtils.toString(payload));
        }

        @Test
        void testSaveAsset_withSingleGroup() {
            Asset payload = new Asset().name("Room-1");
            String groupId = UUID.randomUUID().toString();

            when(restClient.saveAsset(any(Asset.class), eq(groupId), isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(payload);

            String res = tools.saveAsset(JsonUtils.toString(payload), groupId, null);

            verify(restClient).saveAsset(any(Asset.class), eq(groupId), isNull(), isNull(), isNull(), isNull());
            assertThat(res).isEqualTo(JsonUtils.toString(payload));
        }

        @Test
        void testSaveAsset_withMultipleGroups() {
            Asset payload = new Asset().name("Room-2");
            String id1 = UUID.randomUUID().toString();
            String id2 = UUID.randomUUID().toString();
            String groupIds = id1 + "," + id2;

            when(restClient.saveAsset(any(Asset.class), isNull(), eq(Arrays.asList(id1, id2)), isNull(), isNull(), isNull()))
                    .thenReturn(payload);

            String res = tools.saveAsset(JsonUtils.toString(payload), null, groupIds);

            verify(restClient).saveAsset(any(Asset.class), isNull(), eq(Arrays.asList(id1, id2)), isNull(), isNull(), isNull());
            assertThat(res).isEqualTo(JsonUtils.toString(payload));
        }

    }

    @Nested
    @DisplayName("deleteAsset JSON contract")
    class DeleteAssetContract {
        @Test
        void testDeleteAsset_ok() {
            String id = UUID.randomUUID().toString();
            String res = tools.deleteAsset(id);

            verify(restClient).deleteAsset(eq(id));

            assertThat(res).contains("\"status\":\"OK\"");
            assertThat(res).contains(id);
        }

        @Test
        void testDeleteAsset_error() {
            String id = UUID.randomUUID().toString();
            doThrow(new RuntimeException("boom")).when(restClient).deleteAsset(anyString());

            String res = tools.deleteAsset(id);

            assertThat(res).contains("\"status\":\"ERROR\"");
            assertThat(res).contains(id);
            assertThat(res).contains("boom");
        }

    }

}
