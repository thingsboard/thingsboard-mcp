package org.thingsboard.ai.mcp.server.service.tools;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.ai.mcp.server.rest.RestClient;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.telemetry.TelemetryTools;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.IntervalType;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.SortOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TelemetryToolsTest {

    @InjectMocks
    private TelemetryTools tools;

    @Mock
    private RestClientService clientService;

    @Mock
    private RestClient restClient;

    @BeforeEach
    void setup() {
        when(clientService.getClient()).thenReturn(restClient);
    }

    @Test
    void testFindAttributeKeys() {
        UUID id = UUID.randomUUID();
        List<String> keys = List.of("a", "b");
        when(restClient.getAttributeKeys(any(EntityId.class))).thenReturn(keys);

        String result = tools.getAttributeKeys("DEVICE", id.toString());

        ArgumentCaptor<EntityId> entityCap = ArgumentCaptor.forClass(EntityId.class);
        verify(restClient).getAttributeKeys(entityCap.capture());
        assertThat(entityCap.getValue().getId()).isEqualTo(id);
        assertThat(entityCap.getValue().getEntityType().name()).isEqualTo("DEVICE");

        assertThat(result).isEqualTo(JacksonUtil.toString(keys));
    }

    @Test
    void testFindAttributeKeysByScope() {
        UUID id = UUID.randomUUID();
        List<String> keys = List.of("x", "y", "z");
        when(restClient.getAttributeKeysByScope(any(EntityId.class), eq("SHARED_SCOPE"))).thenReturn(keys);

        String result = tools.getAttributeKeysByScope("DEVICE", id.toString(), "SHARED_SCOPE");

        ArgumentCaptor<EntityId> entityCap = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<String> scopeCap = ArgumentCaptor.forClass(String.class);
        verify(restClient).getAttributeKeysByScope(entityCap.capture(), scopeCap.capture());
        assertThat(entityCap.getValue().getId()).isEqualTo(id);
        assertThat(scopeCap.getValue()).isEqualTo("SHARED_SCOPE");

        assertThat(result).isEqualTo(JacksonUtil.toString(keys));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testFindAttributes() {
        UUID id = UUID.randomUUID();
        List<Map<String, Object>> body = new ArrayList<>();
        body.add(Map.of("k", "temp", "v", 22));
        when(restClient.getAttributeKvEntries(any(EntityId.class), anyList())).thenReturn((List) body);

        String result = tools.getAttributes("DEVICE", id.toString(), "temp,model");

        ArgumentCaptor<EntityId> entityCap = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<List<String>> keysCap = ArgumentCaptor.forClass(List.class);
        verify(restClient).getAttributeKvEntries(entityCap.capture(), keysCap.capture());
        assertThat(entityCap.getValue().getId()).isEqualTo(id);
        assertThat(keysCap.getValue()).containsExactly("temp", "model");

        assertThat(result).isEqualTo(JacksonUtil.toString(body));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testFindAttributesByScope() {
        UUID id = UUID.randomUUID();
        List<Map<String, Object>> body = List.of(Map.of("k", "sharedKey", "v", true));
        when(restClient.getAttributesByScope(any(EntityId.class), eq("SHARED_SCOPE"), anyList())).thenReturn((List) body);

        String result = tools.getAttributesByScope("DEVICE", id.toString(), "SHARED_SCOPE", "sharedKey");

        ArgumentCaptor<EntityId> entityCap = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<String> scopeCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<String>> keysCap = ArgumentCaptor.forClass(List.class);
        verify(restClient).getAttributesByScope(entityCap.capture(), scopeCap.capture(), keysCap.capture());
        assertThat(entityCap.getValue().getId()).isEqualTo(id);
        assertThat(scopeCap.getValue()).isEqualTo("SHARED_SCOPE");
        assertThat(keysCap.getValue()).containsExactly("sharedKey");

        assertThat(result).isEqualTo(JacksonUtil.toString(body));
    }

    @Test
    void testFindTimeseriesKeys() {
        UUID id = UUID.randomUUID();
        List<String> keys = List.of("temperature", "battery");
        when(restClient.getTimeseriesKeys(any(EntityId.class))).thenReturn(keys);

        String result = tools.getTimeseriesKeys("DEVICE", id.toString());

        ArgumentCaptor<EntityId> entityCap = ArgumentCaptor.forClass(EntityId.class);
        verify(restClient).getTimeseriesKeys(entityCap.capture());
        assertThat(entityCap.getValue().getId()).isEqualTo(id);

        assertThat(result).isEqualTo(JacksonUtil.toString(keys));
    }

    @Test
    void testFindTimeseries_defaults() {
        when(clientService.getClient()).thenReturn(restClient);

        UUID id = UUID.randomUUID();

        List<TsKvEntry> ts = new ArrayList<>();
        ts.add(new BasicTsKvEntry(1L, new DoubleDataEntry("battery", 90.0)));

        when(restClient.getTimeseries(
                any(EntityId.class),
                anyList(),
                eq(0L),
                eq(Aggregation.NONE),
                isNull(),
                isNull(),
                eq(SortOrder.Direction.DESC),
                eq(0L),
                eq(0L),
                eq(100),
                eq(false)
        )).thenReturn(ts);

        String result = tools.getTimeseries(
                "DEVICE",
                id.toString(),
                "battery",
                "0",
                "0",
                null,
                null,
                null,
                null,
                "NONE",
                "DESC",
                "false"
        );

        ArgumentCaptor<EntityId> entityCap = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<List<String>> keysCap = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Long> intervalCap = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Aggregation> aggCap = ArgumentCaptor.forClass(Aggregation.class);
        ArgumentCaptor<IntervalType> typeCap = ArgumentCaptor.forClass(IntervalType.class);
        ArgumentCaptor<String> tzCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SortOrder.Direction> orderCap = ArgumentCaptor.forClass(SortOrder.Direction.class);
        ArgumentCaptor<Long> startCap = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> endCap = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Integer> limitCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Boolean> strictCap = ArgumentCaptor.forClass(Boolean.class);

        verify(restClient).getTimeseries(
                entityCap.capture(),
                keysCap.capture(),
                intervalCap.capture(),
                aggCap.capture(),
                typeCap.capture(),
                tzCap.capture(),
                orderCap.capture(),
                startCap.capture(),
                endCap.capture(),
                limitCap.capture(),
                strictCap.capture()
        );

        assertThat(entityCap.getValue().getId()).isEqualTo(id);
        assertThat(keysCap.getValue()).containsExactly("battery");
        assertThat(intervalCap.getValue()).isEqualTo(0L);
        assertThat(aggCap.getValue()).isEqualTo(Aggregation.NONE);
        assertThat(typeCap.getValue()).isNull();
        assertThat(tzCap.getValue()).isNull();
        assertThat(orderCap.getValue()).isEqualTo(SortOrder.Direction.DESC);
        assertThat(startCap.getValue()).isEqualTo(0L);
        assertThat(endCap.getValue()).isEqualTo(0L);
        assertThat(limitCap.getValue()).isEqualTo(100);
        assertThat(strictCap.getValue()).isFalse();

        assertThat(result).isEqualTo(JacksonUtil.toString(ts));
    }

    @Test
    void testFindTimeseries_withAggAndInterval() {
        when(clientService.getClient()).thenReturn(restClient);

        UUID id = UUID.randomUUID();

        List<TsKvEntry> ts = new ArrayList<>();
        ts.add(new BasicTsKvEntry(1000L, new DoubleDataEntry("temperature", 21.5)));

        when(restClient.getTimeseries(
                any(EntityId.class),
                anyList(),
                eq(60000L),
                eq(Aggregation.AVG),
                eq(IntervalType.MILLISECONDS),
                eq("UTC"),
                eq(SortOrder.Direction.ASC),
                eq(0L),
                eq(3600000L),
                eq(1000),
                eq(true)
        )).thenReturn(ts);

        String result = tools.getTimeseries(
                "DEVICE",
                id.toString(),
                "temperature",
                "0",
                "3600000",
                "MILLISECONDS",
                "60000",
                "UTC",
                "1000",
                "AVG",
                "ASC",
                "true"
        );

        ArgumentCaptor<EntityId> entityCap = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<List<String>> keysCap = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Long> intervalCap = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Aggregation> aggCap = ArgumentCaptor.forClass(Aggregation.class);
        ArgumentCaptor<IntervalType> typeCap = ArgumentCaptor.forClass(IntervalType.class);
        ArgumentCaptor<String> tzCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SortOrder.Direction> orderCap = ArgumentCaptor.forClass(SortOrder.Direction.class);
        ArgumentCaptor<Long> startCap = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> endCap = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Integer> limitCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Boolean> strictCap = ArgumentCaptor.forClass(Boolean.class);

        verify(restClient).getTimeseries(
                entityCap.capture(),
                keysCap.capture(),
                intervalCap.capture(),
                aggCap.capture(),
                typeCap.capture(),
                tzCap.capture(),
                orderCap.capture(),
                startCap.capture(),
                endCap.capture(),
                limitCap.capture(),
                strictCap.capture()
        );

        assertThat(entityCap.getValue().getId()).isEqualTo(id);
        assertThat(keysCap.getValue()).containsExactly("temperature");
        assertThat(intervalCap.getValue()).isEqualTo(60000L);
        assertThat(aggCap.getValue()).isEqualTo(Aggregation.AVG);
        assertThat(typeCap.getValue()).isEqualTo(IntervalType.MILLISECONDS);
        assertThat(tzCap.getValue()).isEqualTo("UTC");
        assertThat(orderCap.getValue()).isEqualTo(SortOrder.Direction.ASC);
        assertThat(startCap.getValue()).isEqualTo(0L);
        assertThat(endCap.getValue()).isEqualTo(3600000L);
        assertThat(limitCap.getValue()).isEqualTo(1000);
        assertThat(strictCap.getValue()).isTrue();

        assertThat(result).isEqualTo(JacksonUtil.toString(ts));
    }

    @Test
    void testFindSaveDeviceAttributesSuccess() {
        UUID id = UUID.randomUUID();
        when(restClient.saveDeviceAttributes(any(DeviceId.class), eq("SERVER_SCOPE"), any(JsonNode.class))).thenReturn(true);

        String result = tools.saveDeviceAttributes(id.toString(), "SERVER_SCOPE", "{\"k\":\"v\"}");

        ArgumentCaptor<DeviceId> deviceCap = ArgumentCaptor.forClass(DeviceId.class);
        ArgumentCaptor<String> scopeCap = ArgumentCaptor.forClass(String.class);
        verify(restClient).saveDeviceAttributes(deviceCap.capture(), scopeCap.capture(), any(JsonNode.class));
        assertThat(deviceCap.getValue().getId()).isEqualTo(id);
        assertThat(scopeCap.getValue()).isEqualTo("SERVER_SCOPE");

        assertThat(result).isEqualTo("{\"status\":\"Device attributes saved successfully\"}");
    }

    @Test
    void testFindSaveEntityAttributesV1Success() {
        UUID id = UUID.randomUUID();
        when(restClient.saveEntityAttributesV1(any(EntityId.class), eq("SHARED_SCOPE"), any(JsonNode.class))).thenReturn(true);

        String result = tools.saveEntityAttributesV1("DEVICE", id.toString(), "SHARED_SCOPE", "{\"x\":1}");

        ArgumentCaptor<EntityId> entityCap = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<String> scopeCap = ArgumentCaptor.forClass(String.class);
        verify(restClient).saveEntityAttributesV1(entityCap.capture(), scopeCap.capture(), any(JsonNode.class));
        assertThat(entityCap.getValue().getId()).isEqualTo(id);
        assertThat(scopeCap.getValue()).isEqualTo("SHARED_SCOPE");

        assertThat(result).isEqualTo("{\"status\":\"Entity attributes saved using V1 API\"}");
    }

    @Test
    void testFindSaveEntityAttributesV2Success() {
        UUID id = UUID.randomUUID();
        when(restClient.saveEntityAttributesV2(any(EntityId.class), eq("SERVER_SCOPE"), any(JsonNode.class))).thenReturn(true);

        String result = tools.saveEntityAttributesV2("DEVICE", id.toString(), "SERVER_SCOPE", "{\"y\":true}");

        ArgumentCaptor<EntityId> entityCap = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<String> scopeCap = ArgumentCaptor.forClass(String.class);
        verify(restClient).saveEntityAttributesV2(entityCap.capture(), scopeCap.capture(), any(JsonNode.class));
        assertThat(entityCap.getValue().getId()).isEqualTo(id);
        assertThat(scopeCap.getValue()).isEqualTo("SERVER_SCOPE");

        assertThat(result).isEqualTo("{\"status\":\"Entity attributes saved using V2 API\"}");
    }

    @Test
    void testFindSaveEntityTelemetrySuccess() {
        UUID id = UUID.randomUUID();
        when(restClient.saveEntityTelemetry(any(EntityId.class), eq("ANY"), any(JsonNode.class))).thenReturn(true);

        String result = tools.saveEntityTelemetry("DEVICE", id.toString(), "{\"ts\":1,\"values\":{\"t\":20}}");

        ArgumentCaptor<EntityId> entityCap = ArgumentCaptor.forClass(EntityId.class);
        verify(restClient).saveEntityTelemetry(entityCap.capture(), eq("ANY"), any(JsonNode.class));
        assertThat(entityCap.getValue().getId()).isEqualTo(id);

        assertThat(result).isEqualTo("{\"status\":\"Telemetry submitted successfully\"}");
    }

    @Test
    void testFindSaveEntityTelemetryWithTTLSuccess() {
        UUID id = UUID.randomUUID();
        when(restClient.saveEntityTelemetryWithTTL(any(EntityId.class), eq("ANY"), eq(3600L), any(JsonNode.class))).thenReturn(true);

        String result = tools.saveEntityTelemetryWithTTL("DEVICE", id.toString(), "3600", "{\"ts\":1,\"values\":{\"t\":21}}");

        ArgumentCaptor<EntityId> entityCap = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<Long> ttlCap = ArgumentCaptor.forClass(Long.class);
        verify(restClient).saveEntityTelemetryWithTTL(entityCap.capture(), eq("ANY"), ttlCap.capture(), any(JsonNode.class));
        assertThat(entityCap.getValue().getId()).isEqualTo(id);
        assertThat(ttlCap.getValue()).isEqualTo(3600L);

        assertThat(result).isEqualTo("{\"status\":\"Telemetry with TTL submitted successfully\"}");
    }

}
