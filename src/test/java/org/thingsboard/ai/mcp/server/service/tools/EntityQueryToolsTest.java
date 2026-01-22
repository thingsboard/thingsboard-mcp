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
import org.thingsboard.ai.mcp.server.data.KeyFilterInput;
import org.thingsboard.ai.mcp.server.rest.RestClient;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.query.EntityQueryTools;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AssetTypeFilter;
import org.thingsboard.server.common.data.query.BooleanFilterPredicate;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.query.EdgeTypeFilter;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.EntityListFilter;
import org.thingsboard.server.common.data.query.EntityNameFilter;
import org.thingsboard.server.common.data.query.EntityTypeFilter;
import org.thingsboard.server.common.data.query.EntityViewTypeFilter;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.data.query.RelationsQueryFilter;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.query.TsValue;

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
    private RestClient restClient;

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
        void testFindEntityDataBySingleEntityFilter_withAllParams() throws ThingsboardException {
            UUID deviceId = UUID.randomUUID();
            String filterJson = String.format("""
                    {"type":"singleEntity","singleEntity":{"entityType":"DEVICE","id":"%s"}}
                    """, deviceId);

            List<KeyFilterInput> keyFilters = List.of(createNumericTemperatureKeyTelemetryFilter(25.0));
            List<EntityKey> entityFields = List.of(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
            List<EntityKey> latestValues = List.of(new EntityKey(EntityKeyType.TIME_SERIES, "temperature"));

            PageData<EntityData> pageData = createMockPageData();
            when(restClient.findEntityDataByQuery(any(EntityDataQuery.class))).thenReturn(pageData);

            String result = tools.findEntityDataBySingleEntityFilter(
                    filterJson, keyFilters, entityFields, latestValues,
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
        void testFindEntityDataBySingleEntityFilter_minimalParams() throws ThingsboardException {
            UUID deviceId = UUID.randomUUID();
            String filterJson = String.format("""
                    {"type":"singleEntity","singleEntity":{"entityType":"DEVICE","id":"%s"}}
                    """, deviceId);

            PageData<EntityData> pageData = createMockPageData();
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
        void testFindEntityDataByEntityTypeFilter() throws ThingsboardException {
            EntityTypeFilter filter = new EntityTypeFilter();
            filter.setEntityType(EntityType.DEVICE);

            List<EntityKey> entityFields = List.of(
                    new EntityKey(EntityKeyType.ENTITY_FIELD, "name"),
                    new EntityKey(EntityKeyType.ENTITY_FIELD, "label")
            );

            PageData<EntityData> pageData = createMockPageDataWithFields(List.of("name", "label"));
            when(restClient.findEntityDataByQuery(any(EntityDataQuery.class))).thenReturn(pageData);

            String result = tools.findEntityDataByEntityTypeFilter(
                    JacksonUtil.toString(filter), null, entityFields, null,
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
        void testFindEntityDataByDeviceTypeFilter() throws ThingsboardException {
            DeviceTypeFilter filter = new DeviceTypeFilter();
            filter.setDeviceTypes(List.of("Temperature Sensor"));
            filter.setDeviceNameFilter("Room");

            List<KeyFilterInput> keyFilters = List.of(
                    createNumericTemperatureKeyTelemetryFilter(30.0)
            );

            List<EntityKey> latestValues = List.of(
                    new EntityKey(EntityKeyType.TIME_SERIES, "temperature"),
                    new EntityKey(EntityKeyType.TIME_SERIES, "humidity")
            );

            PageData<EntityData> pageData = createMockPageDataWithTelemetry(List.of("temperature", "humidity"));
            when(restClient.findEntityDataByQuery(any(EntityDataQuery.class))).thenReturn(pageData);

            String result = tools.findEntityDataByDeviceTypeFilter(
                    JacksonUtil.toString(filter), keyFilters, null, latestValues,
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
        void testFindEntityDataByRelationsQueryFilter() throws ThingsboardException {
            UUID assetId = UUID.randomUUID();
            String filterJson = String.format("""
                    {"type":"relationsQuery","rootEntity":{"entityType":"ASSET","id":"%s"},"direction":"FROM","maxLevel":1,"filters":[]}
                    """, assetId);

            PageData<EntityData> pageData = createMockPageData();
            when(restClient.findEntityDataByQuery(any(EntityDataQuery.class))).thenReturn(pageData);

            String result = tools.findEntityDataByRelationsQueryFilter(
                    filterJson, null, List.of(new EntityKey(EntityKeyType.ENTITY_FIELD, "name")), null,
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
        void testFindEntityDataByEntityNameFilter() throws ThingsboardException {
            EntityNameFilter filter = new EntityNameFilter();
            filter.setEntityType(EntityType.DEVICE);
            filter.setEntityNameFilter("Sensor");

            PageData<EntityData> pageData = createMockPageData();
            when(restClient.findEntityDataByQuery(any(EntityDataQuery.class))).thenReturn(pageData);

            String result = tools.findEntityDataByEntityNameFilter(
                    JacksonUtil.toString(filter), null, List.of(new EntityKey(EntityKeyType.ENTITY_FIELD, "name")), null,
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
        void testFindEntityDataByAssetTypeFilter() throws ThingsboardException {
            AssetTypeFilter filter = new AssetTypeFilter();
            filter.setAssetTypes(List.of("Building"));
            filter.setAssetNameFilter("Office");

            PageData<EntityData> pageData = createMockPageData();
            when(restClient.findEntityDataByQuery(any(EntityDataQuery.class))).thenReturn(pageData);

            String result = tools.findEntityDataByAssetTypeFilter(
                    JacksonUtil.toString(filter), null, List.of(new EntityKey(EntityKeyType.ENTITY_FIELD, "name")), null,
                    "10", "0", null, null, null, null
            );

            verify(restClient).findEntityDataByQuery(entityDataQueryCaptor.capture());
            assertThat(entityDataQueryCaptor.getValue().getEntityFilter()).isInstanceOf(AssetTypeFilter.class);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should find edges by type")
        void testFindEntityDataByEdgeTypeFilter() throws ThingsboardException {
            EdgeTypeFilter filter = new EdgeTypeFilter();
            filter.setEdgeTypes(List.of("Gateway"));

            PageData<EntityData> pageData = createMockPageData();
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
        void testFindEntityDataByEntityViewTypeFilter() throws ThingsboardException {
            EntityViewTypeFilter filter = new EntityViewTypeFilter();
            filter.setEntityViewTypes(List.of("Monitor"));

            PageData<EntityData> pageData = createMockPageData();
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

            List<KeyFilterInput> keyFilters = List.of(
                    createNumericTemperatureKeyTelemetryFilter(25.0)
            );

            when(restClient.countEntitiesByQuery(any(EntityCountQuery.class))).thenReturn(1L);

            String result = tools.countBySingleEntityFilter(filterJson, keyFilters);

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

            List<KeyFilterInput> keyFilters = List.of(
                    createNumericTemperatureKeyTelemetryFilter(30.0)
            );

            when(restClient.countEntitiesByQuery(any(EntityCountQuery.class))).thenReturn(23L);

            String result = tools.countByEntityTypeFilter(JacksonUtil.toString(filter), keyFilters);

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

            List<KeyFilterInput> keyFilters = List.of(
                    createNumericTemperatureKeyTelemetryFilter(30.0),
                    createBooleanActiveKeyAttributeFilter()
            );

            when(restClient.countEntitiesByQuery(any(EntityCountQuery.class))).thenReturn(7L);

            String result = tools.countByDeviceTypeFilter(JacksonUtil.toString(filter), keyFilters);

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

    private PageData<EntityData> createMockPageData() {
        List<EntityData> data = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            EntityData ed = new EntityData();
            ed.setEntityId(new DeviceId(UUID.randomUUID()));
            data.add(ed);
        }
        return new PageData<>(data, 1, 3, false);
    }

    private PageData<EntityData> createMockPageDataWithFields(List<String> fieldNames) {
        List<EntityData> data = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            EntityData ed = new EntityData();
            ed.setEntityId(new DeviceId(UUID.randomUUID()));

            Map<EntityKeyType, Map<String, TsValue>> latest = new HashMap<>();
            Map<String, TsValue> entityFields = new HashMap<>();

            for (String fieldName : fieldNames) {
                TsValue tsValue = new TsValue(System.currentTimeMillis(), fieldName + " value " + i);
                entityFields.put(fieldName, tsValue);
            }

            latest.put(EntityKeyType.ENTITY_FIELD, entityFields);
            ed.setLatest(latest);

            data.add(ed);
        }
        return new PageData<>(data, 1, 3, false);
    }

    private PageData<EntityData> createMockPageDataWithTelemetry(List<String> telemetryKeys) {
        List<EntityData> data = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            EntityData ed = new EntityData();
            ed.setEntityId(new DeviceId(UUID.randomUUID()));

            Map<EntityKeyType, Map<String, TsValue>> latest = new HashMap<>();
            Map<String, TsValue> timeseries = new HashMap<>();

            for (String key : telemetryKeys) {
                TsValue tsValue = new TsValue(System.currentTimeMillis(), String.valueOf(20.0 + i));
                timeseries.put(key, tsValue);
            }

            latest.put(EntityKeyType.TIME_SERIES, timeseries);
            ed.setLatest(latest);

            data.add(ed);
        }
        return new PageData<>(data, 1, 3, false);
    }

    private KeyFilterInput createNumericTemperatureKeyTelemetryFilter(double threshold) {
        KeyFilter filter = new KeyFilter();
        filter.setKey(new EntityKey(EntityKeyType.TIME_SERIES, "temperature"));
        filter.setValueType(EntityKeyValueType.NUMERIC);

        NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);

        FilterPredicateValue<Double> value = new FilterPredicateValue<>(threshold);
        predicate.setValue(value);

        filter.setPredicate(predicate);
        return new KeyFilterInput(filter);
    }

    private KeyFilterInput createBooleanActiveKeyAttributeFilter() {
        KeyFilter filter = new KeyFilter();
        filter.setKey(new EntityKey(EntityKeyType.ATTRIBUTE, "active"));
        filter.setValueType(EntityKeyValueType.BOOLEAN);

        BooleanFilterPredicate predicate = new BooleanFilterPredicate();
        predicate.setOperation(BooleanFilterPredicate.BooleanOperation.EQUAL);

        FilterPredicateValue<Boolean> predicateValue = new FilterPredicateValue<>(true);
        predicate.setValue(predicateValue);

        filter.setPredicate(predicate);
        return new KeyFilterInput(filter);
    }

}
