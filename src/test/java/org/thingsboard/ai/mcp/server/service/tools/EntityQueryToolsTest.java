package org.thingsboard.ai.mcp.server.service.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.query.EntityQueryTools;
import org.thingsboard.ai.mcp.server.util.JacksonUtil;
import org.thingsboard.client.ThingsboardClient;
import org.thingsboard.client.model.ApiUsageStateFilter;
import org.thingsboard.client.model.AssetSearchQueryFilter;
import org.thingsboard.client.model.AssetTypeFilter;
import org.thingsboard.client.model.DeviceId;
import org.thingsboard.client.model.DeviceSearchQueryFilter;
import org.thingsboard.client.model.DeviceTypeFilter;
import org.thingsboard.client.model.EdgeSearchQueryFilter;
import org.thingsboard.client.model.EdgeTypeFilter;
import org.thingsboard.client.model.EntitiesByGroupNameFilter;
import org.thingsboard.client.model.EntityCountQuery;
import org.thingsboard.client.model.EntityData;
import org.thingsboard.client.model.EntityDataQuery;
import org.thingsboard.client.model.EntityGroupFilter;
import org.thingsboard.client.model.EntityGroupListFilter;
import org.thingsboard.client.model.EntityGroupNameFilter;
import org.thingsboard.client.model.EntityKeyType;
import org.thingsboard.client.model.EntityListFilter;
import org.thingsboard.client.model.EntityNameFilter;
import org.thingsboard.client.model.EntityType;
import org.thingsboard.client.model.EntityTypeFilter;
import org.thingsboard.client.model.EntityViewSearchQueryFilter;
import org.thingsboard.client.model.EntityViewTypeFilter;
import org.thingsboard.client.model.PageDataEntityData;
import org.thingsboard.client.model.RelationsQueryFilter;
import org.thingsboard.client.model.SingleEntityFilter;
import org.thingsboard.client.model.StateEntityOwnerFilter;
import org.thingsboard.client.model.TsValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EntityQueryToolsTest {

    @InjectMocks
    private EntityQueryTools tools;

    @Mock
    private RestClientService clientService;

    @Mock
    private ThingsboardClient restClient;

    @Captor
    private ArgumentCaptor<EntityDataQuery> entityDataQueryCaptor;

    @Captor
    private ArgumentCaptor<EntityCountQuery> entityCountQueryCaptor;

    @BeforeEach
    void setup() {
        when(clientService.getClient()).thenReturn(restClient);
    }

    @Nested
    @DisplayName("Entity Data Query - Single Entity Filter")
    class SingleEntityFilterTests {

        @Test
        @DisplayName("Should find entity data by single entity filter with all parameters")
        void testFindEntityDataBySingleEntityFilter_withAllParams() {
            UUID deviceId = UUID.randomUUID();
            String filterJson = String.format("""
                    {"type":"singleEntity","singleEntity":{"entityType":"DEVICE","id":"%s"}}
                    """, deviceId);

            String keyFiltersJson = "[{\"keyType\":\"TIME_SERIES\",\"key\":\"temperature\",\"valueType\":\"NUMERIC\",\"predicateType\":\"NUMERIC\",\"operation\":\"GREATER\",\"defaultValue\":25.0}]";
            String entityFieldsJson = "[{\"type\":\"ENTITY_FIELD\",\"key\":\"name\"}]";
            String latestValuesJson = "[{\"type\":\"TIME_SERIES\",\"key\":\"temperature\"}]";

            PageDataEntityData pageData = createMockPageData();
            when(restClient.findEntityDataByQuery(any(EntityDataQuery.class))).thenReturn(pageData);

            String result = tools.findEntityDataBySingleEntityFilter(
                    filterJson, keyFiltersJson, entityFieldsJson, latestValuesJson,
                    "10", "0", "sensor", "name", "ENTITY_FIELD", "ASC"
            );

            verify(restClient).findEntityDataByQuery(entityDataQueryCaptor.capture());
            EntityDataQuery query = entityDataQueryCaptor.getValue();

            assertThat(query.getEntityFilter()).isInstanceOf(SingleEntityFilter.class);
            assertThat(query.getKeyFilters()).hasSize(1);
            assertThat(query.getEntityFields()).hasSize(1);
            assertThat(query.getLatestValues()).hasSize(1);
            assertThat(query.getPageLink().getPageSize()).isEqualTo(10);
            assertThat(query.getPageLink().getPage()).isEqualTo(0);
            assertThat(query.getPageLink().getTextSearch()).isEqualTo("sensor");

            assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
        }

        @Test
        @DisplayName("Should find entity data with minimal parameters")
        void testFindEntityDataBySingleEntityFilter_minimalParams() {
            UUID deviceId = UUID.randomUUID();
            String filterJson = String.format("""
                    {"type":"singleEntity","singleEntity":{"entityType":"DEVICE","id":"%s"}}
                    """, deviceId);

            PageDataEntityData pageData = createMockPageData();
            when(restClient.findEntityDataByQuery(any(EntityDataQuery.class))).thenReturn(pageData);

            String result = tools.findEntityDataBySingleEntityFilter(
                    filterJson, null, null, null,
                    "20", "0", null, null, null, null
            );

            verify(restClient).findEntityDataByQuery(entityDataQueryCaptor.capture());
            EntityDataQuery query = entityDataQueryCaptor.getValue();

            assertThat(query.getKeyFilters()).isNullOrEmpty();
            assertThat(query.getEntityFields()).isNullOrEmpty();
            assertThat(query.getLatestValues()).isNullOrEmpty();
            assertThat(query.getPageLink().getPageSize()).isEqualTo(20);
            assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
        }

    }

    @Nested
    @DisplayName("Entity Data Query - Entity Type Filter")
    class EntityTypeFilterTests {

        @Test
        @DisplayName("Should find all devices by entity type filter")
        void testFindEntityDataByEntityTypeFilter() {
            EntityTypeFilter filter = new EntityTypeFilter();
            filter.setEntityType(EntityType.DEVICE);

            String entityFieldsJson = "[{\"type\":\"ENTITY_FIELD\",\"key\":\"name\"},{\"type\":\"ENTITY_FIELD\",\"key\":\"label\"}]";

            PageDataEntityData pageData = createMockPageDataWithFields(List.of("name", "label"));
            when(restClient.findEntityDataByQuery(any(EntityDataQuery.class))).thenReturn(pageData);

            String result = tools.findEntityDataByEntityTypeFilter(
                    JacksonUtil.toString(filter), null, entityFieldsJson, null,
                    "50", "0", null, null, null, null
            );

            verify(restClient).findEntityDataByQuery(entityDataQueryCaptor.capture());
            EntityDataQuery query = entityDataQueryCaptor.getValue();

            assertThat(query.getEntityFilter()).isInstanceOf(EntityTypeFilter.class);
            assertThat(query.getEntityFields()).hasSize(2);
            assertThat(result).contains("name");
            assertThat(result).contains("label");
        }

    }

    @Nested
    @DisplayName("Entity Data Query - Device Type Filter")
    class DeviceTypeFilterTests {

        @Test
        @DisplayName("Should find devices by device type with filters")
        void testFindEntityDataByDeviceTypeFilter() {
            DeviceTypeFilter filter = new DeviceTypeFilter();
            filter.setDeviceTypes(List.of("Temperature Sensor"));
            filter.setDeviceNameFilter("Room");

            String keyFiltersJson = "[{\"keyType\":\"TIME_SERIES\",\"key\":\"temperature\",\"valueType\":\"NUMERIC\",\"predicateType\":\"NUMERIC\",\"operation\":\"GREATER\",\"defaultValue\":30.0}]";
            String latestValuesJson = "[{\"type\":\"TIME_SERIES\",\"key\":\"temperature\"},{\"type\":\"TIME_SERIES\",\"key\":\"humidity\"}]";

            PageDataEntityData pageData = createMockPageDataWithTelemetry(List.of("temperature", "humidity"));
            when(restClient.findEntityDataByQuery(any(EntityDataQuery.class))).thenReturn(pageData);

            String result = tools.findEntityDataByDeviceTypeFilter(
                    JacksonUtil.toString(filter), keyFiltersJson, null, latestValuesJson,
                    "25", "1", "Room", "temperature", "TIME_SERIES", "DESC"
            );

            verify(restClient).findEntityDataByQuery(entityDataQueryCaptor.capture());
            EntityDataQuery query = entityDataQueryCaptor.getValue();

            assertThat(query.getEntityFilter()).isInstanceOf(DeviceTypeFilter.class);
            assertThat(query.getKeyFilters()).hasSize(1);
            assertThat(query.getLatestValues()).hasSize(2);
            assertThat(query.getPageLink().getPage()).isEqualTo(1);
            assertThat(result).contains("temperature");
            assertThat(result).contains("humidity");
        }

    }

    @Nested
    @DisplayName("Entity Data Query - Relations Query Filter")
    class RelationsQueryFilterTests {

        @Test
        @DisplayName("Should find entities related to root entity")
        void testFindEntityDataByRelationsQueryFilter() {
            UUID assetId = UUID.randomUUID();
            String filterJson = String.format("""
                    {"type":"relationsQuery","rootEntity":{"entityType":"ASSET","id":"%s"},"direction":"FROM","maxLevel":1,"filters":[]}
                    """, assetId);

            PageDataEntityData pageData = createMockPageData();
            when(restClient.findEntityDataByQuery(any(EntityDataQuery.class))).thenReturn(pageData);

            String result = tools.findEntityDataByRelationsQueryFilter(
                    filterJson, null, "[{\"type\":\"ENTITY_FIELD\",\"key\":\"name\"}]", null,
                    "100", "0", null, null, null, null
            );

            verify(restClient).findEntityDataByQuery(entityDataQueryCaptor.capture());
            EntityDataQuery query = entityDataQueryCaptor.getValue();

            assertThat(query.getEntityFilter()).isInstanceOf(RelationsQueryFilter.class);
            assertThat(result).isNotNull();
        }

    }

    @Nested
    @DisplayName("Entity Data Query - Entity Name Filter")
    class EntityNameFilterTests {

        @Test
        @DisplayName("Should find entities by name pattern")
        void testFindEntityDataByEntityNameFilter() {
            EntityNameFilter filter = new EntityNameFilter();
            filter.setEntityType(EntityType.DEVICE);
            filter.setEntityNameFilter("Sensor");

            PageDataEntityData pageData = createMockPageData();
            when(restClient.findEntityDataByQuery(any(EntityDataQuery.class))).thenReturn(pageData);

            String result = tools.findEntityDataByEntityNameFilter(
                    JacksonUtil.toString(filter), null, "[{\"type\":\"ENTITY_FIELD\",\"key\":\"name\"}]", null,
                    "30", "0", "Sensor", null, null, null
            );

            verify(restClient).findEntityDataByQuery(entityDataQueryCaptor.capture());
            EntityDataQuery query = entityDataQueryCaptor.getValue();

            assertThat(query.getEntityFilter()).isInstanceOf(EntityNameFilter.class);
            assertThat(query.getPageLink().getTextSearch()).isEqualTo("Sensor");
            assertThat(result).isNotNull();
        }

    }

    @Nested
    @DisplayName("Entity Data Query - Asset/Edge/EntityView Type Filters")
    class SpecificTypeFilterTests {

        @Test
        @DisplayName("Should find assets by type")
        void testFindEntityDataByAssetTypeFilter() {
            AssetTypeFilter filter = new AssetTypeFilter();
            filter.setAssetTypes(List.of("Building"));
            filter.setAssetNameFilter("Office");

            PageDataEntityData pageData = createMockPageData();
            when(restClient.findEntityDataByQuery(any(EntityDataQuery.class))).thenReturn(pageData);

            String result = tools.findEntityDataByAssetTypeFilter(
                    JacksonUtil.toString(filter), null, "[{\"type\":\"ENTITY_FIELD\",\"key\":\"name\"}]", null,
                    "10", "0", null, null, null, null
            );

            verify(restClient).findEntityDataByQuery(entityDataQueryCaptor.capture());
            assertThat(entityDataQueryCaptor.getValue().getEntityFilter()).isInstanceOf(AssetTypeFilter.class);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should find edges by type")
        void testFindEntityDataByEdgeTypeFilter() {
            EdgeTypeFilter filter = new EdgeTypeFilter();
            filter.setEdgeTypes(List.of("Gateway"));

            PageDataEntityData pageData = createMockPageData();
            when(restClient.findEntityDataByQuery(any(EntityDataQuery.class))).thenReturn(pageData);

            String result = tools.findEntityDataByEdgeTypeFilter(
                    JacksonUtil.toString(filter), null, null, null,
                    "15", "0", null, null, null, null
            );

            verify(restClient).findEntityDataByQuery(entityDataQueryCaptor.capture());
            assertThat(entityDataQueryCaptor.getValue().getEntityFilter()).isInstanceOf(EdgeTypeFilter.class);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should find entity views by type")
        void testFindEntityDataByEntityViewTypeFilter() {
            EntityViewTypeFilter filter = new EntityViewTypeFilter();
            filter.setEntityViewTypes(List.of("Monitor"));

            PageDataEntityData pageData = createMockPageData();
            when(restClient.findEntityDataByQuery(any(EntityDataQuery.class))).thenReturn(pageData);

            String result = tools.findEntityDataByEntityViewTypeFilter(
                    JacksonUtil.toString(filter), null, null, null,
                    "20", "0", null, null, null, null
            );

            verify(restClient).findEntityDataByQuery(entityDataQueryCaptor.capture());
            assertThat(entityDataQueryCaptor.getValue().getEntityFilter()).isInstanceOf(EntityViewTypeFilter.class);
            assertThat(result).isNotNull();
        }

    }

    @Nested
    @DisplayName("Entity Count Query - Single Entity Filter")
    class CountBySingleEntityFilterTests {

        @Test
        @DisplayName("Should count single entity without filters")
        void testCountBySingleEntityFilter_noFilters() {
            UUID deviceId = UUID.randomUUID();
            String filterJson = String.format("""
                    {"type":"singleEntity","singleEntity":{"entityType":"DEVICE","id":"%s"}}
                    """, deviceId);

            when(restClient.countEntitiesByQuery(any(EntityCountQuery.class))).thenReturn(1L);

            String result = tools.countBySingleEntityFilter(filterJson, null);

            verify(restClient).countEntitiesByQuery(entityCountQueryCaptor.capture());
            EntityCountQuery query = entityCountQueryCaptor.getValue();

            assertThat(query.getEntityFilter()).isInstanceOf(SingleEntityFilter.class);
            assertThat(query.getKeyFilters()).isNullOrEmpty();
            assertThat(result).contains("1");
        }

        @Test
        @DisplayName("Should count single entity with key filters")
        void testCountBySingleEntityFilter_withFilters() {
            UUID deviceId = UUID.randomUUID();
            String filterJson = String.format("""
                    {"type":"singleEntity","singleEntity":{"entityType":"DEVICE","id":"%s"}}
                    """, deviceId);

            String keyFiltersJson = "[{\"keyType\":\"TIME_SERIES\",\"key\":\"temperature\",\"valueType\":\"NUMERIC\",\"predicateType\":\"NUMERIC\",\"operation\":\"GREATER\",\"defaultValue\":25.0}]";

            when(restClient.countEntitiesByQuery(any(EntityCountQuery.class))).thenReturn(1L);

            String result = tools.countBySingleEntityFilter(filterJson, keyFiltersJson);

            verify(restClient).countEntitiesByQuery(entityCountQueryCaptor.capture());
            EntityCountQuery query = entityCountQueryCaptor.getValue();

            assertThat(query.getKeyFilters()).hasSize(1);
            assertThat(result).contains("1");
        }

    }

    @Nested
    @DisplayName("Entity Count Query - Entity Type Filter")
    class CountByEntityTypeFilterTests {

        @Test
        @DisplayName("Should count all devices")
        void testCountByEntityTypeFilter_allDevices() {
            EntityTypeFilter filter = new EntityTypeFilter();
            filter.setEntityType(EntityType.DEVICE);

            when(restClient.countEntitiesByQuery(any(EntityCountQuery.class))).thenReturn(157L);

            String result = tools.countByEntityTypeFilter(JacksonUtil.toString(filter), null);

            verify(restClient).countEntitiesByQuery(entityCountQueryCaptor.capture());
            EntityCountQuery query = entityCountQueryCaptor.getValue();

            assertThat(query.getEntityFilter()).isInstanceOf(EntityTypeFilter.class);
            assertThat(result).contains("157");
        }

        @Test
        @DisplayName("Should count devices with temperature filter")
        void testCountByEntityTypeFilter_withTemperatureFilter() {
            EntityTypeFilter filter = new EntityTypeFilter();
            filter.setEntityType(EntityType.DEVICE);

            String keyFiltersJson = "[{\"keyType\":\"TIME_SERIES\",\"key\":\"temperature\",\"valueType\":\"NUMERIC\",\"predicateType\":\"NUMERIC\",\"operation\":\"GREATER\",\"defaultValue\":30.0}]";

            when(restClient.countEntitiesByQuery(any(EntityCountQuery.class))).thenReturn(23L);

            String result = tools.countByEntityTypeFilter(JacksonUtil.toString(filter), keyFiltersJson);

            verify(restClient).countEntitiesByQuery(entityCountQueryCaptor.capture());
            EntityCountQuery query = entityCountQueryCaptor.getValue();

            assertThat(query.getKeyFilters()).hasSize(1);
            assertThat(result).contains("23");
        }

    }

    @Nested
    @DisplayName("Entity Count Query - Device Type Filter")
    class CountByDeviceTypeFilterTests {

        @Test
        @DisplayName("Should count devices by type")
        void testCountByDeviceTypeFilter() {
            DeviceTypeFilter filter = new DeviceTypeFilter();
            filter.setDeviceTypes(List.of("Temperature Sensor"));

            when(restClient.countEntitiesByQuery(any(EntityCountQuery.class))).thenReturn(45L);

            String result = tools.countByDeviceTypeFilter(JacksonUtil.toString(filter), null);

            verify(restClient).countEntitiesByQuery(entityCountQueryCaptor.capture());
            EntityCountQuery query = entityCountQueryCaptor.getValue();

            assertThat(query.getEntityFilter()).isInstanceOf(DeviceTypeFilter.class);
            assertThat(result).contains("45");
        }

        @Test
        @DisplayName("Should count devices with multiple filters")
        void testCountByDeviceTypeFilter_multipleFilters() {
            DeviceTypeFilter filter = new DeviceTypeFilter();
            filter.setDeviceTypes(List.of("Temperature Sensor"));

            String keyFiltersJson = "[{\"keyType\":\"TIME_SERIES\",\"key\":\"temperature\",\"valueType\":\"NUMERIC\",\"predicateType\":\"NUMERIC\",\"operation\":\"GREATER\",\"defaultValue\":30.0},{\"keyType\":\"ATTRIBUTE\",\"key\":\"active\",\"valueType\":\"BOOLEAN\",\"predicateType\":\"BOOLEAN\",\"operation\":\"EQUAL\",\"defaultValue\":true}]";

            when(restClient.countEntitiesByQuery(any(EntityCountQuery.class))).thenReturn(7L);

            String result = tools.countByDeviceTypeFilter(JacksonUtil.toString(filter), keyFiltersJson);

            verify(restClient).countEntitiesByQuery(entityCountQueryCaptor.capture());
            EntityCountQuery query = entityCountQueryCaptor.getValue();

            assertThat(query.getKeyFilters()).hasSize(2);
            assertThat(result).contains("7");
        }

    }

    @Nested
    @DisplayName("Entity Count Query - Relations Query Filter")
    class CountByRelationsQueryFilterTests {

        @Test
        @DisplayName("Should count related entities")
        void testCountByRelationsQueryFilter() {
            UUID assetId = UUID.randomUUID();
            String filterJson = String.format("""
                    {"type":"relationsQuery","rootEntity":{"entityType":"ASSET","id":"%s"},"direction":"FROM","maxLevel":1,"filters":[]}
                    """, assetId);

            when(restClient.countEntitiesByQuery(any(EntityCountQuery.class))).thenReturn(12L);

            String result = tools.countByRelationsQueryFilter(filterJson, null);

            verify(restClient).countEntitiesByQuery(entityCountQueryCaptor.capture());
            EntityCountQuery query = entityCountQueryCaptor.getValue();

            assertThat(query.getEntityFilter()).isInstanceOf(RelationsQueryFilter.class);
            assertThat(result).contains("12");
        }

    }

    @Nested
    @DisplayName("Entity Count Query - Asset/Edge/EntityView Type Filters")
    class CountBySpecificTypeFilterTests {

        @Test
        @DisplayName("Should count assets by type")
        void testCountByAssetTypeFilter() {
            AssetTypeFilter filter = new AssetTypeFilter();
            filter.setAssetTypes(List.of("Building"));

            when(restClient.countEntitiesByQuery(any(EntityCountQuery.class))).thenReturn(8L);

            String result = tools.countByAssetTypeFilter(JacksonUtil.toString(filter), null);

            verify(restClient).countEntitiesByQuery(entityCountQueryCaptor.capture());
            assertThat(entityCountQueryCaptor.getValue().getEntityFilter()).isInstanceOf(AssetTypeFilter.class);
            assertThat(result).contains("8");
        }

        @Test
        @DisplayName("Should count edges by type")
        void testCountByEdgeTypeFilter() {
            EdgeTypeFilter filter = new EdgeTypeFilter();
            filter.setEdgeTypes(List.of("Gateway"));

            when(restClient.countEntitiesByQuery(any(EntityCountQuery.class))).thenReturn(3L);

            String result = tools.countByEdgeTypeFilter(JacksonUtil.toString(filter), null);

            verify(restClient).countEntitiesByQuery(entityCountQueryCaptor.capture());
            assertThat(entityCountQueryCaptor.getValue().getEntityFilter()).isInstanceOf(EdgeTypeFilter.class);
            assertThat(result).contains("3");
        }

        @Test
        @DisplayName("Should count entity views by type")
        void testCountByEntityViewTypeFilter() {
            EntityViewTypeFilter filter = new EntityViewTypeFilter();
            filter.setEntityViewTypes(List.of("Monitor"));

            when(restClient.countEntitiesByQuery(any(EntityCountQuery.class))).thenReturn(5L);

            String result = tools.countByEntityViewTypeFilter(JacksonUtil.toString(filter), null);

            verify(restClient).countEntitiesByQuery(entityCountQueryCaptor.capture());
            assertThat(entityCountQueryCaptor.getValue().getEntityFilter()).isInstanceOf(EntityViewTypeFilter.class);
            assertThat(result).contains("5");
        }

    }

    @Nested
    @DisplayName("Entity Count Query - Entity Name Filter")
    class CountByEntityNameFilterTests {

        @Test
        @DisplayName("Should count entities by name pattern")
        void testCountByEntityNameFilter() {
            EntityNameFilter filter = new EntityNameFilter();
            filter.setEntityType(EntityType.DEVICE);
            filter.setEntityNameFilter("Sensor");

            when(restClient.countEntitiesByQuery(any(EntityCountQuery.class))).thenReturn(34L);

            String result = tools.countByEntityNameFilter(JacksonUtil.toString(filter), null);

            verify(restClient).countEntitiesByQuery(entityCountQueryCaptor.capture());
            EntityCountQuery query = entityCountQueryCaptor.getValue();

            assertThat(query.getEntityFilter()).isInstanceOf(EntityNameFilter.class);
            assertThat(result).contains("34");
        }

    }

    @Nested
    @DisplayName("Entity Count Query - Entity List Filter")
    class CountByEntityListFilterTests {

        @Test
        @DisplayName("Should count entities from list")
        void testCountByEntityListFilter() {
            EntityListFilter filter = new EntityListFilter();
            filter.setEntityType(EntityType.DEVICE);
            List<String> entityIds = List.of(
                    UUID.randomUUID().toString(),
                    UUID.randomUUID().toString()
            );
            filter.setEntityList(entityIds);

            when(restClient.countEntitiesByQuery(any(EntityCountQuery.class))).thenReturn(2L);

            String result = tools.countByEntityListFilter(JacksonUtil.toString(filter), null);

            verify(restClient).countEntitiesByQuery(entityCountQueryCaptor.capture());
            EntityCountQuery query = entityCountQueryCaptor.getValue();

            assertThat(query.getEntityFilter()).isInstanceOf(EntityListFilter.class);
            assertThat(result).contains("2");
        }

    }

    @Nested
    @DisplayName("Entity Data Query - Entity Group Filter")
    class EntityGroupFilterTests {

        @Test
        @DisplayName("Should find entity data by entity group filter")
        void testFindEntityDataByEntityGroupFilter() {
            UUID groupId = UUID.randomUUID();
            String filterJson = String.format("""
                    {"type":"entityGroup","groupType":"DEVICE","entityGroup":"%s"}
                    """, groupId);

            PageDataEntityData pageData = createMockPageData();
            when(restClient.findEntityDataByQuery(any(EntityDataQuery.class))).thenReturn(pageData);

            String result = tools.findEntityDataByEntityGroupFilter(
                    filterJson, null, null, null,
                    "10", "0", null, null, null, null
            );

            verify(restClient).findEntityDataByQuery(entityDataQueryCaptor.capture());
            assertThat(entityDataQueryCaptor.getValue().getEntityFilter()).isInstanceOf(EntityGroupFilter.class);
            assertThat(result).isNotNull();
        }

    }

    @Nested
    @DisplayName("Entity Count Query - Entity Group Filter")
    class CountByEntityGroupFilterTests {

        @Test
        @DisplayName("Should count entities by group filter")
        void testCountByEntityGroupFilter() {
            UUID groupId = UUID.randomUUID();
            String filterJson = String.format("""
                    {"type":"entityGroup","groupType":"DEVICE","entityGroup":"%s"}
                    """, groupId);

            when(restClient.countEntitiesByQuery(any(EntityCountQuery.class))).thenReturn(15L);

            String result = tools.countByEntityGroupFilter(filterJson, null);

            verify(restClient).countEntitiesByQuery(entityCountQueryCaptor.capture());
            assertThat(entityCountQueryCaptor.getValue().getEntityFilter()).isInstanceOf(EntityGroupFilter.class);
            assertThat(result).contains("15");
        }

    }

    @Nested
    @DisplayName("Entity Data Query - Entity List Filter")
    class EntityListFilterTests {

        @Test
        @DisplayName("Should find entity data by entity list filter")
        void testFindEntityDataByEntityListFilter() {
            EntityListFilter filter = new EntityListFilter();
            filter.setEntityType(EntityType.DEVICE);
            filter.setEntityList(List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()));

            PageDataEntityData pageData = createMockPageData();
            when(restClient.findEntityDataByQuery(any(EntityDataQuery.class))).thenReturn(pageData);

            String result = tools.findEntityDataByEntityListFilter(
                    JacksonUtil.toString(filter), null, null, null,
                    "10", "0", null, null, null, null
            );

            verify(restClient).findEntityDataByQuery(entityDataQueryCaptor.capture());
            assertThat(entityDataQueryCaptor.getValue().getEntityFilter()).isInstanceOf(EntityListFilter.class);
            assertThat(result).isNotNull();
        }

    }

    @Nested
    @DisplayName("Entity Data Query - Entity Group List Filter")
    class EntityGroupListFilterTests {

        @Test
        @DisplayName("Should find entity data by entity group list filter")
        void testFindEntityDataByEntityGroupListFilter() {
            UUID group1 = UUID.randomUUID();
            UUID group2 = UUID.randomUUID();
            String filterJson = String.format("""
                    {"type":"entityGroupList","groupType":"DEVICE","entityGroupList":["%s","%s"]}
                    """, group1, group2);

            PageDataEntityData pageData = createMockPageData();
            when(restClient.findEntityDataByQuery(any(EntityDataQuery.class))).thenReturn(pageData);

            String result = tools.findEntityDataByEntityGroupListFilter(
                    filterJson, null, null, null,
                    "10", "0", null, null, null, null
            );

            verify(restClient).findEntityDataByQuery(entityDataQueryCaptor.capture());
            assertThat(entityDataQueryCaptor.getValue().getEntityFilter()).isInstanceOf(EntityGroupListFilter.class);
            assertThat(result).isNotNull();
        }

    }

    @Nested
    @DisplayName("Entity Count Query - Entity Group List Filter")
    class CountByEntityGroupListFilterTests {

        @Test
        @DisplayName("Should count entities by group list filter")
        void testCountByEntityGroupListFilter() {
            UUID group1 = UUID.randomUUID();
            UUID group2 = UUID.randomUUID();
            String filterJson = String.format("""
                    {"type":"entityGroupList","groupType":"DEVICE","entityGroupList":["%s","%s"]}
                    """, group1, group2);

            when(restClient.countEntitiesByQuery(any(EntityCountQuery.class))).thenReturn(25L);

            String result = tools.countByEntityGroupListFilter(filterJson, null);

            verify(restClient).countEntitiesByQuery(entityCountQueryCaptor.capture());
            assertThat(entityCountQueryCaptor.getValue().getEntityFilter()).isInstanceOf(EntityGroupListFilter.class);
            assertThat(result).contains("25");
        }

    }

    @Nested
    @DisplayName("Entity Data Query - Entity Group Name Filter")
    class EntityGroupNameFilterTests {

        @Test
        @DisplayName("Should find entity data by entity group name filter")
        void testFindEntityDataByEntityGroupNameFilter() {
            String filterJson = """
                    {"type":"entityGroupName","groupType":"DEVICE","entityGroupNameFilter":"Sensors"}
                    """;

            PageDataEntityData pageData = createMockPageData();
            when(restClient.findEntityDataByQuery(any(EntityDataQuery.class))).thenReturn(pageData);

            String result = tools.findEntityDataByEntityGroupNameFilter(
                    filterJson, null, null, null,
                    "10", "0", null, null, null, null
            );

            verify(restClient).findEntityDataByQuery(entityDataQueryCaptor.capture());
            assertThat(entityDataQueryCaptor.getValue().getEntityFilter()).isInstanceOf(EntityGroupNameFilter.class);
            assertThat(result).isNotNull();
        }

    }

    @Nested
    @DisplayName("Entity Count Query - Entity Group Name Filter")
    class CountByEntityGroupNameFilterTests {

        @Test
        @DisplayName("Should count entities by group name filter")
        void testCountByEntityGroupNameFilter() {
            String filterJson = """
                    {"type":"entityGroupName","groupType":"DEVICE","entityGroupNameFilter":"Sensors"}
                    """;

            when(restClient.countEntitiesByQuery(any(EntityCountQuery.class))).thenReturn(18L);

            String result = tools.countByEntityGroupNameFilter(filterJson, null);

            verify(restClient).countEntitiesByQuery(entityCountQueryCaptor.capture());
            assertThat(entityCountQueryCaptor.getValue().getEntityFilter()).isInstanceOf(EntityGroupNameFilter.class);
            assertThat(result).contains("18");
        }

    }

    @Nested
    @DisplayName("Entity Data Query - Entities By Group Name Filter")
    class EntitiesByGroupNameFilterTests {

        @Test
        @DisplayName("Should find entity data by entities group name filter")
        void testFindEntityDataByEntitiesGroupNameFilter() {
            String filterJson = """
                    {"type":"entitiesByGroupName","groupType":"DEVICE","entityGroupNameFilter":"Water Meters"}
                    """;

            PageDataEntityData pageData = createMockPageData();
            when(restClient.findEntityDataByQuery(any(EntityDataQuery.class))).thenReturn(pageData);

            String result = tools.findEntityDataByEntitiesGroupNameFilter(
                    filterJson, null, null, null,
                    "10", "0", null, null, null, null
            );

            verify(restClient).findEntityDataByQuery(entityDataQueryCaptor.capture());
            assertThat(entityDataQueryCaptor.getValue().getEntityFilter()).isInstanceOf(EntitiesByGroupNameFilter.class);
            assertThat(result).isNotNull();
        }

    }

    @Nested
    @DisplayName("Entity Count Query - Entities By Group Name Filter")
    class CountByEntitiesGroupNameFilterTests {

        @Test
        @DisplayName("Should count entities by entities group name filter")
        void testCountByEntitiesGroupNameFilter() {
            String filterJson = """
                    {"type":"entitiesByGroupName","groupType":"DEVICE","entityGroupNameFilter":"Water Meters"}
                    """;

            when(restClient.countEntitiesByQuery(any(EntityCountQuery.class))).thenReturn(42L);

            String result = tools.countByEntitiesGroupNameFilter(filterJson, null);

            verify(restClient).countEntitiesByQuery(entityCountQueryCaptor.capture());
            assertThat(entityCountQueryCaptor.getValue().getEntityFilter()).isInstanceOf(EntitiesByGroupNameFilter.class);
            assertThat(result).contains("42");
        }

    }

    @Nested
    @DisplayName("Entity Data Query - State Entity Owner Filter")
    class StateEntityOwnerFilterTests {

        @Test
        @DisplayName("Should find entity data by state entity owner filter")
        void testFindEntityDataByStateEntityOwnerFilter() {
            UUID deviceId = UUID.randomUUID();
            String filterJson = String.format("""
                    {"type":"stateEntityOwner","singleEntity":{"id":"%s","entityType":"DEVICE"}}
                    """, deviceId);

            PageDataEntityData pageData = createMockPageData();
            when(restClient.findEntityDataByQuery(any(EntityDataQuery.class))).thenReturn(pageData);

            String result = tools.findEntityDataByStateEntityOwnerFilter(
                    filterJson, null, null, null,
                    "10", "0", null, null, null, null
            );

            verify(restClient).findEntityDataByQuery(entityDataQueryCaptor.capture());
            assertThat(entityDataQueryCaptor.getValue().getEntityFilter()).isInstanceOf(StateEntityOwnerFilter.class);
            assertThat(result).isNotNull();
        }

    }

    @Nested
    @DisplayName("Entity Data Query - API Usage State Filter")
    class ApiUsageStateFilterTests {

        @Test
        @DisplayName("Should find entity data by API usage state filter")
        void testFindEntityDataByApiUsageStateFilter() {
            String filterJson = """
                    {"type":"apiUsageState"}
                    """;

            PageDataEntityData pageData = createMockPageData();
            when(restClient.findEntityDataByQuery(any(EntityDataQuery.class))).thenReturn(pageData);

            String result = tools.findEntityDataByApiUsageStateFilter(
                    filterJson, null, null, null,
                    "10", "0", null, null, null, null
            );

            verify(restClient).findEntityDataByQuery(entityDataQueryCaptor.capture());
            assertThat(entityDataQueryCaptor.getValue().getEntityFilter()).isInstanceOf(ApiUsageStateFilter.class);
            assertThat(result).isNotNull();
        }

    }

    @Nested
    @DisplayName("Entity Count Query - API Usage State Filter")
    class CountByApiUsageStateFilterTests {

        @Test
        @DisplayName("Should count by API usage state filter")
        void testCountByApiUsageStateFilter() {
            String filterJson = """
                    {"type":"apiUsageState"}
                    """;

            when(restClient.countEntitiesByQuery(any(EntityCountQuery.class))).thenReturn(1L);

            String result = tools.countByApiUsageStateFilter(filterJson, null);

            verify(restClient).countEntitiesByQuery(entityCountQueryCaptor.capture());
            assertThat(entityCountQueryCaptor.getValue().getEntityFilter()).isInstanceOf(ApiUsageStateFilter.class);
            assertThat(result).contains("1");
        }

    }

    @Nested
    @DisplayName("Entity Data Query - Asset Search Query Filter")
    class AssetSearchQueryFilterTests {

        @Test
        @DisplayName("Should find entity data by asset search query filter")
        void testFindEntityDataByAssetSearchQueryFilter() {
            UUID assetId = UUID.randomUUID();
            String filterJson = String.format("""
                    {"type":"assetSearchQuery","rootEntity":{"entityType":"ASSET","id":"%s"},"direction":"FROM","maxLevel":1,"relationType":"Contains","assetTypes":["Building"]}
                    """, assetId);

            PageDataEntityData pageData = createMockPageData();
            when(restClient.findEntityDataByQuery(any(EntityDataQuery.class))).thenReturn(pageData);

            String result = tools.findEntityDataByAssetSearchQueryFilter(
                    filterJson, null, null, null,
                    "10", "0", null, null, null, null
            );

            verify(restClient).findEntityDataByQuery(entityDataQueryCaptor.capture());
            assertThat(entityDataQueryCaptor.getValue().getEntityFilter()).isInstanceOf(AssetSearchQueryFilter.class);
            assertThat(result).isNotNull();
        }

    }

    @Nested
    @DisplayName("Entity Count Query - Asset Search Query Filter")
    class CountByAssetSearchQueryFilterTests {

        @Test
        @DisplayName("Should count by asset search query filter")
        void testCountByAssetSearchQueryFilter() {
            UUID assetId = UUID.randomUUID();
            String filterJson = String.format("""
                    {"type":"assetSearchQuery","rootEntity":{"entityType":"ASSET","id":"%s"},"direction":"FROM","maxLevel":1,"relationType":"Contains","assetTypes":["Building"]}
                    """, assetId);

            when(restClient.countEntitiesByQuery(any(EntityCountQuery.class))).thenReturn(6L);

            String result = tools.countByAssetSearchQueryFilter(filterJson, null);

            verify(restClient).countEntitiesByQuery(entityCountQueryCaptor.capture());
            assertThat(entityCountQueryCaptor.getValue().getEntityFilter()).isInstanceOf(AssetSearchQueryFilter.class);
            assertThat(result).contains("6");
        }

    }

    @Nested
    @DisplayName("Entity Data Query - Device Search Query Filter")
    class DeviceSearchQueryFilterTests {

        @Test
        @DisplayName("Should find entity data by device search query filter")
        void testFindEntityDataByDeviceSearchQueryFilter() {
            UUID assetId = UUID.randomUUID();
            String filterJson = String.format("""
                    {"type":"deviceSearchQuery","rootEntity":{"entityType":"ASSET","id":"%s"},"direction":"FROM","maxLevel":2,"relationType":"Contains","deviceTypes":["Sensor"]}
                    """, assetId);

            PageDataEntityData pageData = createMockPageData();
            when(restClient.findEntityDataByQuery(any(EntityDataQuery.class))).thenReturn(pageData);

            String result = tools.findEntityDataByDeviceSearchQueryFilter(
                    filterJson, null, null, null,
                    "10", "0", null, null, null, null
            );

            verify(restClient).findEntityDataByQuery(entityDataQueryCaptor.capture());
            assertThat(entityDataQueryCaptor.getValue().getEntityFilter()).isInstanceOf(DeviceSearchQueryFilter.class);
            assertThat(result).isNotNull();
        }

    }

    @Nested
    @DisplayName("Entity Count Query - Device Search Query Filter")
    class CountByDeviceSearchQueryFilterTests {

        @Test
        @DisplayName("Should count by device search query filter")
        void testCountByDeviceSearchQueryFilter() {
            UUID assetId = UUID.randomUUID();
            String filterJson = String.format("""
                    {"type":"deviceSearchQuery","rootEntity":{"entityType":"ASSET","id":"%s"},"direction":"FROM","maxLevel":2,"relationType":"Contains","deviceTypes":["Sensor"]}
                    """, assetId);

            when(restClient.countEntitiesByQuery(any(EntityCountQuery.class))).thenReturn(14L);

            String result = tools.countByDeviceSearchQueryFilter(filterJson, null);

            verify(restClient).countEntitiesByQuery(entityCountQueryCaptor.capture());
            assertThat(entityCountQueryCaptor.getValue().getEntityFilter()).isInstanceOf(DeviceSearchQueryFilter.class);
            assertThat(result).contains("14");
        }

    }

    @Nested
    @DisplayName("Entity Data Query - Entity View Search Query Filter")
    class EntityViewSearchQueryFilterTests {

        @Test
        @DisplayName("Should find entity data by entity view search query filter")
        void testFindEntityDataByEntityViewSearchQueryFilter() {
            UUID assetId = UUID.randomUUID();
            String filterJson = String.format("""
                    {"type":"entityViewSearchQuery","rootEntity":{"entityType":"ASSET","id":"%s"},"direction":"FROM","maxLevel":1,"relationType":"Contains","entityViewTypes":["Monitor"]}
                    """, assetId);

            PageDataEntityData pageData = createMockPageData();
            when(restClient.findEntityDataByQuery(any(EntityDataQuery.class))).thenReturn(pageData);

            String result = tools.findEntityDataByEntityViewSearchQueryFilter(
                    filterJson, null, null, null,
                    "10", "0", null, null, null, null
            );

            verify(restClient).findEntityDataByQuery(entityDataQueryCaptor.capture());
            assertThat(entityDataQueryCaptor.getValue().getEntityFilter()).isInstanceOf(EntityViewSearchQueryFilter.class);
            assertThat(result).isNotNull();
        }

    }

    @Nested
    @DisplayName("Entity Count Query - Entity View Search Query Filter")
    class CountByEntityViewSearchQueryFilterTests {

        @Test
        @DisplayName("Should count by entity view search query filter")
        void testCountByEntityViewSearchQueryFilter() {
            UUID assetId = UUID.randomUUID();
            String filterJson = String.format("""
                    {"type":"entityViewSearchQuery","rootEntity":{"entityType":"ASSET","id":"%s"},"direction":"FROM","maxLevel":1,"relationType":"Contains","entityViewTypes":["Monitor"]}
                    """, assetId);

            when(restClient.countEntitiesByQuery(any(EntityCountQuery.class))).thenReturn(4L);

            String result = tools.countByEntityViewSearchQueryFilter(filterJson, null);

            verify(restClient).countEntitiesByQuery(entityCountQueryCaptor.capture());
            assertThat(entityCountQueryCaptor.getValue().getEntityFilter()).isInstanceOf(EntityViewSearchQueryFilter.class);
            assertThat(result).contains("4");
        }

    }

    @Nested
    @DisplayName("Entity Data Query - Edge Search Query Filter")
    class EdgeSearchQueryFilterTests {

        @Test
        @DisplayName("Should find entity data by edge search query filter")
        void testFindEntityDataByEdgeQueryFilter() {
            UUID assetId = UUID.randomUUID();
            String filterJson = String.format("""
                    {"type":"edgeSearchQuery","rootEntity":{"entityType":"ASSET","id":"%s"},"direction":"FROM","maxLevel":2,"relationType":"Contains","edgeTypes":["Gateway"]}
                    """, assetId);

            PageDataEntityData pageData = createMockPageData();
            when(restClient.findEntityDataByQuery(any(EntityDataQuery.class))).thenReturn(pageData);

            String result = tools.findEntityDataByEdgeQueryFilter(
                    filterJson, null, null, null,
                    "10", "0", null, null, null, null
            );

            verify(restClient).findEntityDataByQuery(entityDataQueryCaptor.capture());
            assertThat(entityDataQueryCaptor.getValue().getEntityFilter()).isInstanceOf(EdgeSearchQueryFilter.class);
            assertThat(result).isNotNull();
        }

    }

    @Nested
    @DisplayName("Entity Count Query - Edge Search Query Filter")
    class CountByEdgeSearchQueryFilterTests {

        @Test
        @DisplayName("Should count by edge search query filter")
        void testCountByEdgeQueryFilter() {
            UUID assetId = UUID.randomUUID();
            String filterJson = String.format("""
                    {"type":"edgeSearchQuery","rootEntity":{"entityType":"ASSET","id":"%s"},"direction":"FROM","maxLevel":2,"relationType":"Contains","edgeTypes":["Gateway"]}
                    """, assetId);

            when(restClient.countEntitiesByQuery(any(EntityCountQuery.class))).thenReturn(2L);

            String result = tools.countByEdgeQueryFilter(filterJson, null);

            verify(restClient).countEntitiesByQuery(entityCountQueryCaptor.capture());
            assertThat(entityCountQueryCaptor.getValue().getEntityFilter()).isInstanceOf(EdgeSearchQueryFilter.class);
            assertThat(result).contains("2");
        }

    }

    private PageDataEntityData createMockPageData() {
        List<EntityData> data = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            EntityData ed = new EntityData();
            ed.setEntityId(new DeviceId().id(UUID.randomUUID()));
            data.add(ed);
        }
        return new PageDataEntityData(1, 3L, false).data(data);
    }

    private PageDataEntityData createMockPageDataWithFields(List<String> fieldNames) {
        List<EntityData> data = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            EntityData ed = new EntityData();
            ed.setEntityId(new DeviceId().id(UUID.randomUUID()));

            Map<String, Map<String, TsValue>> latest = new HashMap<>();
            Map<String, TsValue> entityFields = new HashMap<>();

            for (String fieldName : fieldNames) {
                TsValue tsValue = new TsValue().ts(System.currentTimeMillis()).value(fieldName + " value " + i);
                entityFields.put(fieldName, tsValue);
            }

            latest.put(EntityKeyType.ENTITY_FIELD.getValue(), entityFields);
            ed.setLatest(latest);

            data.add(ed);
        }
        return new PageDataEntityData(1, 3L, false).data(data);
    }

    @Nested
    @DisplayName("Canonical (nested) KeyFilter format support")
    class CanonicalKeyFilterFormatTests {

        @Test
        @DisplayName("Should accept canonical nested key filter format")
        void testCanonicalNestedFormat() {
            UUID deviceId = UUID.randomUUID();
            String filterJson = String.format("""
                    {"type":"singleEntity","singleEntity":{"entityType":"DEVICE","id":"%s"}}
                    """, deviceId);

            String keyFiltersJson = """
                    [{"key":{"type":"TIME_SERIES","key":"temperature"},"valueType":"NUMERIC","predicate":{"operation":"GREATER","value":{"defaultValue":25.0},"type":"NUMERIC"}}]
                    """;

            PageDataEntityData pageData = createMockPageData();
            when(restClient.findEntityDataByQuery(any(EntityDataQuery.class))).thenReturn(pageData);

            String result = tools.findEntityDataBySingleEntityFilter(
                    filterJson, keyFiltersJson, null, null,
                    "10", "0", null, null, null, null
            );

            verify(restClient).findEntityDataByQuery(entityDataQueryCaptor.capture());
            EntityDataQuery query = entityDataQueryCaptor.getValue();

            assertThat(query.getKeyFilters()).hasSize(1);
            assertThat(query.getKeyFilters().get(0).getKey().getType()).isEqualTo(EntityKeyType.TIME_SERIES);
            assertThat(query.getKeyFilters().get(0).getKey().getKey()).isEqualTo("temperature");
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should accept canonical format with complex predicate")
        void testCanonicalComplexPredicate() {
            UUID deviceId = UUID.randomUUID();
            String filterJson = String.format("""
                    {"type":"singleEntity","singleEntity":{"entityType":"DEVICE","id":"%s"}}
                    """, deviceId);

            String keyFiltersJson = """
                    [{"key":{"type":"TIME_SERIES","key":"temperature"},"valueType":"NUMERIC","predicate":{"type":"COMPLEX","operation":"OR","predicates":[{"operation":"LESS","value":{"defaultValue":10},"type":"NUMERIC"},{"operation":"GREATER","value":{"defaultValue":30},"type":"NUMERIC"}]}}]
                    """;

            PageDataEntityData pageData = createMockPageData();
            when(restClient.findEntityDataByQuery(any(EntityDataQuery.class))).thenReturn(pageData);

            String result = tools.findEntityDataBySingleEntityFilter(
                    filterJson, keyFiltersJson, null, null,
                    "10", "0", null, null, null, null
            );

            verify(restClient).findEntityDataByQuery(entityDataQueryCaptor.capture());
            EntityDataQuery query = entityDataQueryCaptor.getValue();

            assertThat(query.getKeyFilters()).hasSize(1);
            assertThat(query.getKeyFilters().get(0).getKey().getKey()).isEqualTo("temperature");
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should accept canonical format with multiple filters")
        void testCanonicalMultipleFilters() {
            DeviceTypeFilter filter = new DeviceTypeFilter();
            filter.setDeviceTypes(List.of("Temperature Sensor"));

            String keyFiltersJson = """
                    [{"key":{"type":"TIME_SERIES","key":"temperature"},"valueType":"NUMERIC","predicate":{"operation":"GREATER","value":{"defaultValue":30.0},"type":"NUMERIC"}},{"key":{"type":"ATTRIBUTE","key":"active"},"valueType":"BOOLEAN","predicate":{"operation":"EQUAL","value":{"defaultValue":true},"type":"BOOLEAN"}}]
                    """;

            when(restClient.countEntitiesByQuery(any(EntityCountQuery.class))).thenReturn(7L);

            String result = tools.countByDeviceTypeFilter(JacksonUtil.toString(filter), keyFiltersJson);

            verify(restClient).countEntitiesByQuery(entityCountQueryCaptor.capture());
            EntityCountQuery query = entityCountQueryCaptor.getValue();

            assertThat(query.getKeyFilters()).hasSize(2);
            assertThat(result).contains("7");
        }

        @Test
        @DisplayName("Should accept canonical format with dynamic values")
        void testCanonicalDynamicValues() {
            UUID deviceId = UUID.randomUUID();
            String filterJson = String.format("""
                    {"type":"singleEntity","singleEntity":{"entityType":"DEVICE","id":"%s"}}
                    """, deviceId);

            String keyFiltersJson = """
                    [{"key":{"type":"TIME_SERIES","key":"temperature"},"valueType":"NUMERIC","predicate":{"operation":"GREATER","value":{"defaultValue":20,"dynamicValue":{"sourceType":"CURRENT_TENANT","sourceAttribute":"tempThreshold","inherit":false}},"type":"NUMERIC"}}]
                    """;

            PageDataEntityData pageData = createMockPageData();
            when(restClient.findEntityDataByQuery(any(EntityDataQuery.class))).thenReturn(pageData);

            String result = tools.findEntityDataBySingleEntityFilter(
                    filterJson, keyFiltersJson, null, null,
                    "10", "0", null, null, null, null
            );

            verify(restClient).findEntityDataByQuery(entityDataQueryCaptor.capture());
            EntityDataQuery query = entityDataQueryCaptor.getValue();

            assertThat(query.getKeyFilters()).hasSize(1);
            assertThat(result).isNotNull();
        }

    }

    private PageDataEntityData createMockPageDataWithTelemetry(List<String> telemetryKeys) {
        List<EntityData> data = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            EntityData ed = new EntityData();
            ed.setEntityId(new DeviceId().id(UUID.randomUUID()));

            Map<String, Map<String, TsValue>> latest = new HashMap<>();
            Map<String, TsValue> timeseries = new HashMap<>();

            for (String key : telemetryKeys) {
                TsValue tsValue = new TsValue().ts(System.currentTimeMillis()).value(String.valueOf(20.0 + i));
                timeseries.put(key, tsValue);
            }

            latest.put(EntityKeyType.TIME_SERIES.getValue(), timeseries);
            ed.setLatest(latest);

            data.add(ed);
        }
        return new PageDataEntityData(1, 3L, false).data(data);
    }

}
