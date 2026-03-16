package org.thingsboard.ai.mcp.server.service.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.telemetry.TelemetryTools;
import org.thingsboard.ai.mcp.server.util.JsonUtils;
import org.thingsboard.client.ThingsboardClient;
import org.thingsboard.client.model.AttributeData;
import org.thingsboard.client.model.TsData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TelemetryToolsTest {

    @InjectMocks
    private TelemetryTools tools;

    @Mock
    private RestClientService clientService;

    @Mock
    private ThingsboardClient restClient;

    @BeforeEach
    void setup() {
        when(clientService.getClient()).thenReturn(restClient);
    }

    @Test
    void testFindAttributeKeys() {
        UUID id = UUID.randomUUID();
        List<String> keys = List.of("a", "b");
        when(restClient.getAttributeKeys(anyString(), anyString())).thenReturn(keys);

        String result = tools.getAttributeKeys("DEVICE", id.toString());

        verify(restClient).getAttributeKeys(eq("DEVICE"), eq(id.toString()));
        assertThat(result).isEqualTo(JsonUtils.toString(keys));
    }

    @Test
    void testFindAttributeKeysByScope() {
        UUID id = UUID.randomUUID();
        List<String> keys = List.of("x", "y", "z");
        when(restClient.getAttributeKeysByScope(anyString(), anyString(), eq("SHARED_SCOPE"))).thenReturn(keys);

        String result = tools.getAttributeKeysByScope("DEVICE", id.toString(), "SHARED_SCOPE");

        verify(restClient).getAttributeKeysByScope(eq("DEVICE"), eq(id.toString()), eq("SHARED_SCOPE"));
        assertThat(result).isEqualTo(JsonUtils.toString(keys));
    }

    @Test
    void testFindAttributes() {
        UUID id = UUID.randomUUID();
        List<AttributeData> body = new ArrayList<>();
        AttributeData attr = new AttributeData(null, "temp");
        attr.setValue(22);
        body.add(attr);
        when(restClient.getAttributes(anyString(), anyString(), any(), any())).thenReturn(body);

        String result = tools.getAttributes("DEVICE", id.toString(), "temp,model");

        verify(restClient).getAttributes(eq("DEVICE"), eq(id.toString()), eq("temp,model"), isNull());
        assertThat(result).isEqualTo(JsonUtils.toString(body));
    }

    @Test
    void testFindAttributes_nullKeys() {
        UUID id = UUID.randomUUID();
        List<AttributeData> body = new ArrayList<>();
        AttributeData attr = new AttributeData(null, "temp");
        attr.setValue(22);
        body.add(attr);
        when(restClient.getAttributes(anyString(), anyString(), any(), any())).thenReturn(body);

        String result = tools.getAttributes("DEVICE", id.toString(), null);

        verify(restClient).getAttributes(eq("DEVICE"), eq(id.toString()), isNull(), isNull());
        assertThat(result).isEqualTo(JsonUtils.toString(body));
    }

    @Test
    void testFindAttributesByScope_nullKeys() {
        UUID id = UUID.randomUUID();
        List<AttributeData> body = new ArrayList<>();
        AttributeData attr = new AttributeData(null, "sharedKey");
        attr.setValue(true);
        body.add(attr);
        when(restClient.getAttributesByScope(anyString(), anyString(), eq("SHARED_SCOPE"), any(), any())).thenReturn(body);

        String result = tools.getAttributesByScope("DEVICE", id.toString(), "SHARED_SCOPE", null);

        verify(restClient).getAttributesByScope(eq("DEVICE"), eq(id.toString()), eq("SHARED_SCOPE"), isNull(), isNull());
        assertThat(result).isEqualTo(JsonUtils.toString(body));
    }

    @Test
    void testFindLatestTimeseries_nullKeys() {
        UUID id = UUID.randomUUID();
        Map<String, List<TsData>> body = Map.of(
                "temperature", List.of(new TsData(1L).value("22.0"))
        );
        when(restClient.getLatestTimeseries(anyString(), anyString(), any(), any(), any())).thenReturn(body);

        String result = tools.getLatestTimeseries("DEVICE", id.toString(), null, "false");

        verify(restClient).getLatestTimeseries(eq("DEVICE"), eq(id.toString()), isNull(), eq(false), isNull());
        assertThat(result).isEqualTo(JsonUtils.toString(body));
    }

    @Test
    void testFindAttributesByScope() {
        UUID id = UUID.randomUUID();
        List<AttributeData> body = new ArrayList<>();
        AttributeData attr = new AttributeData(null, "sharedKey");
        attr.setValue(true);
        body.add(attr);
        when(restClient.getAttributesByScope(anyString(), anyString(), eq("SHARED_SCOPE"), any(), any())).thenReturn(body);

        String result = tools.getAttributesByScope("DEVICE", id.toString(), "SHARED_SCOPE", "sharedKey");

        verify(restClient).getAttributesByScope(eq("DEVICE"), eq(id.toString()), eq("SHARED_SCOPE"), eq("sharedKey"), isNull());
        assertThat(result).isEqualTo(JsonUtils.toString(body));
    }

    @Test
    void testFindTimeseriesKeys() {
        UUID id = UUID.randomUUID();
        List<String> keys = List.of("temperature", "battery");
        when(restClient.getTimeseriesKeys(anyString(), anyString())).thenReturn(keys);

        String result = tools.getTimeseriesKeys("DEVICE", id.toString());

        verify(restClient).getTimeseriesKeys(eq("DEVICE"), eq(id.toString()));
        assertThat(result).isEqualTo(JsonUtils.toString(keys));
    }

    @Test
    void testFindTimeseries_defaults() {
        UUID id = UUID.randomUUID();

        Map<String, List<TsData>> ts = Map.of(
                "battery", List.of(new TsData(1L).value("90.0"))
        );

        when(restClient.getTimeseriesHistory(
                anyString(), anyString(),
                any(), any(), anyString(),
                any(), any(), any(),
                any(), anyString(), anyString(),
                any(), any()
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

        verify(restClient).getTimeseriesHistory(
                eq("DEVICE"),
                eq(id.toString()),
                eq(0L),
                eq(0L),
                eq("battery"),
                isNull(),
                eq(0L),
                isNull(),
                eq("100"),
                eq("NONE"),
                eq("DESC"),
                eq(false),
                isNull()
        );

        assertThat(result).isEqualTo(JsonUtils.toString(ts));
    }

    @Test
    void testFindTimeseries_withAggAndInterval() {
        UUID id = UUID.randomUUID();

        Map<String, List<TsData>> ts = Map.of(
                "temperature", List.of(new TsData(1000L).value("21.5"))
        );

        when(restClient.getTimeseriesHistory(
                anyString(), anyString(),
                any(), any(), anyString(),
                any(), any(), any(),
                any(), anyString(), anyString(),
                any(), any()
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

        verify(restClient).getTimeseriesHistory(
                eq("DEVICE"),
                eq(id.toString()),
                eq(0L),
                eq(3600000L),
                eq("temperature"),
                eq("MILLISECONDS"),
                eq(60000L),
                eq("UTC"),
                eq("1000"),
                eq("AVG"),
                eq("ASC"),
                eq(true),
                isNull()
        );

        assertThat(result).isEqualTo(JsonUtils.toString(ts));
    }

    @Test
    void testFindSaveDeviceAttributesSuccess() {
        UUID id = UUID.randomUUID();
        when(restClient.saveDeviceAttributes(anyString(), anyString(), anyString())).thenReturn("ok");

        String result = tools.saveDeviceAttributes(id.toString(), "SERVER_SCOPE", "{\"k\":\"v\"}");

        verify(restClient).saveDeviceAttributes(eq(id.toString()), eq("SERVER_SCOPE"), eq("{\"k\":\"v\"}"));
        assertThat(result).isEqualTo("{\"status\":\"Device attributes saved successfully\"}");
    }

    @Test
    void testFindSaveEntityAttributesV2Success() {
        UUID id = UUID.randomUUID();
        when(restClient.saveEntityAttributesV2(anyString(), anyString(), anyString(), anyString())).thenReturn("ok");

        String result = tools.saveEntityAttributesV2("DEVICE", id.toString(), "SERVER_SCOPE", "{\"y\":true}");

        verify(restClient).saveEntityAttributesV2(eq("DEVICE"), eq(id.toString()), eq("SERVER_SCOPE"), eq("{\"y\":true}"));
        assertThat(result).isEqualTo("{\"status\":\"Entity attributes saved using V2 API\"}");
    }

    @Test
    void testFindSaveEntityTelemetrySuccess() {
        UUID id = UUID.randomUUID();
        when(restClient.saveEntityTelemetry(anyString(), anyString(), anyString(), anyString())).thenReturn("ok");

        String result = tools.saveEntityTelemetry("DEVICE", id.toString(), "{\"ts\":1,\"values\":{\"t\":20}}");

        verify(restClient).saveEntityTelemetry(eq("DEVICE"), eq(id.toString()), eq("ANY"), eq("{\"ts\":1,\"values\":{\"t\":20}}"));
        assertThat(result).isEqualTo("{\"status\":\"Telemetry submitted successfully\"}");
    }

    @Test
    void testFindSaveEntityTelemetryWithTTLSuccess() {
        UUID id = UUID.randomUUID();
        when(restClient.saveEntityTelemetryWithTTL(anyString(), anyString(), anyString(), any(), anyString())).thenReturn("ok");

        String result = tools.saveEntityTelemetryWithTTL("DEVICE", id.toString(), "3600", "{\"ts\":1,\"values\":{\"t\":21}}");

        verify(restClient).saveEntityTelemetryWithTTL(eq("DEVICE"), eq(id.toString()), eq("ANY"), eq(3600L), eq("{\"ts\":1,\"values\":{\"t\":21}}"));
        assertThat(result).isEqualTo("{\"status\":\"Telemetry with TTL submitted successfully\"}");
    }

}
