package org.thingsboard.ai.mcp.server.service.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.relation.RelationTools;
import org.thingsboard.ai.mcp.server.util.JacksonUtil;
import org.thingsboard.client.ThingsboardClient;
import org.thingsboard.client.model.EntityRelation;
import org.thingsboard.client.model.EntityRelationInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RelationToolsTest {

    @InjectMocks
    private RelationTools tools;

    @Mock
    private RestClientService clientService;

    @Mock
    private ThingsboardClient restClient;

    @Test
    void testFindRelation_commonGroup() {
        when(clientService.getClient()).thenReturn(restClient);

        UUID fromUuid = UUID.randomUUID();
        UUID toUuid = UUID.randomUUID();

        EntityRelation relation = new EntityRelation();
        when(restClient.getRelation(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(relation);

        String result = tools.getRelation(fromUuid.toString(), "DEVICE", "Contains", null, toUuid.toString(), "ASSET");

        verify(restClient).getRelation(
                eq(fromUuid.toString()), eq("DEVICE"), eq("Contains"),
                eq(toUuid.toString()), eq("ASSET"), eq("COMMON")
        );

        assertThat(result).isEqualTo(JacksonUtil.toString(relation));
    }

    @Test
    void testFindRelation_withGroupProvided() {
        when(clientService.getClient()).thenReturn(restClient);

        UUID fromUuid = UUID.randomUUID();
        UUID toUuid = UUID.randomUUID();

        EntityRelation relation = new EntityRelation();
        when(restClient.getRelation(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(relation);

        String result = tools.getRelation(fromUuid.toString(), "USER", "Manages", "RULE_CHAIN", toUuid.toString(), "DEVICE");

        verify(restClient).getRelation(
                eq(fromUuid.toString()), eq("USER"), eq("Manages"),
                eq(toUuid.toString()), eq("DEVICE"), eq("RULE_CHAIN")
        );

        assertThat(result).isEqualTo(JacksonUtil.toString(relation));
    }

    @Test
    void testFindInfoByFrom_alarmGroup() {
        when(clientService.getClient()).thenReturn(restClient);

        UUID fromUuid = UUID.randomUUID();

        List<EntityRelationInfo> relationInfos = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            relationInfos.add(new EntityRelationInfo());
        }
        when(restClient.findEntityRelationInfosByFrom(anyString(), anyString(), anyString())).thenReturn(relationInfos);

        String result = tools.findInfoByFrom(fromUuid.toString(), "DEVICE", "RULE_CHAIN");

        verify(restClient).findEntityRelationInfosByFrom(eq("DEVICE"), eq(fromUuid.toString()), eq("RULE_CHAIN"));

        assertThat(result).isEqualTo(JacksonUtil.toString(relationInfos));
    }

    @Test
    void testFindByFromWithRelationType_common() {
        when(clientService.getClient()).thenReturn(restClient);

        UUID fromUuid = UUID.randomUUID();

        List<EntityRelation> relations = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            relations.add(new EntityRelation());
        }
        when(restClient.findEntityRelationsByFromAndRelationType(anyString(), anyString(), anyString(), anyString())).thenReturn(relations);

        String result = tools.findByFromWithRelationType(fromUuid.toString(), "TENANT", "Owns", null);

        verify(restClient).findEntityRelationsByFromAndRelationType(eq("TENANT"), eq(fromUuid.toString()), eq("Owns"), eq("COMMON"));

        assertThat(result).isEqualTo(JacksonUtil.toString(relations));
    }

    @Test
    void testFindInfoByTo_alarmGroup() {
        when(clientService.getClient()).thenReturn(restClient);

        UUID fromUuid = UUID.randomUUID();

        List<EntityRelationInfo> relationInfos = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            relationInfos.add(new EntityRelationInfo());
        }
        when(restClient.findEntityRelationInfosByTo(anyString(), anyString(), anyString())).thenReturn(relationInfos);

        String result = tools.findInfoByTo(fromUuid.toString(), "DEVICE", "RULE_CHAIN");

        verify(restClient).findEntityRelationInfosByTo(eq("DEVICE"), eq(fromUuid.toString()), eq("RULE_CHAIN"));

        assertThat(result).isEqualTo(JacksonUtil.toString(relationInfos));
    }

    @Test
    void testFindByToWithRelationType_dashboard() {
        when(clientService.getClient()).thenReturn(restClient);

        UUID toUuid = UUID.randomUUID();

        List<EntityRelation> relations = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            relations.add(new EntityRelation());
        }
        when(restClient.findEntityRelationsByToAndRelationType(anyString(), anyString(), anyString(), anyString())).thenReturn(relations);

        String result = tools.findByToWithRelationType(toUuid.toString(), "DASHBOARD", "Contains", "DASHBOARD");

        verify(restClient).findEntityRelationsByToAndRelationType(eq("DASHBOARD"), eq(toUuid.toString()), eq("Contains"), eq("DASHBOARD"));

        assertThat(result).isEqualTo(JacksonUtil.toString(relations));
    }

    @Test
    void testFindByToWithRelationType_common() {
        when(clientService.getClient()).thenReturn(restClient);

        UUID toUuid = UUID.randomUUID();

        List<EntityRelation> relations = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            relations.add(new EntityRelation());
        }
        when(restClient.findEntityRelationsByToAndRelationType(anyString(), anyString(), anyString(), anyString())).thenReturn(relations);

        String result = tools.findByToWithRelationType(toUuid.toString(), "DASHBOARD", "Contains", null);

        verify(restClient).findEntityRelationsByToAndRelationType(eq("DASHBOARD"), eq(toUuid.toString()), eq("Contains"), eq("COMMON"));

        assertThat(result).isEqualTo(JacksonUtil.toString(relations));
    }

    @Test
    void testDeleteRelation_ok_defaultGroup() {
        when(clientService.getClient()).thenReturn(restClient);

        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();

        String res = tools.deleteRelation(fromId.toString(), "DEVICE", "Contains", null, toId.toString(), "ASSET");
        assertThat(res).contains("\"status\":\"OK\"");
    }

    @Test
    void testDeleteRelation_ok_explicitGroup() {
        when(clientService.getClient()).thenReturn(restClient);

        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();

        String res = tools.deleteRelation(fromId.toString(), "TENANT", "Owns", "RULE_CHAIN", toId.toString(), "CUSTOMER");
        assertThat(res).contains("\"status\":\"OK\"");
    }

    @Test
    void testDeleteRelations_ok() {
        when(clientService.getClient()).thenReturn(restClient);

        UUID entityUuid = UUID.randomUUID();
        String type = "DEVICE";

        String result = tools.deleteRelations(entityUuid.toString(), type);

        verify(restClient).deleteRelations(eq(entityUuid.toString()), eq(type));
        assertThat(result).contains("\"status\":\"OK\"");
    }

    @Test
    void testDeleteRelations_error() {
        when(clientService.getClient()).thenReturn(restClient);

        UUID entityUuid = UUID.randomUUID();
        String type = "ASSET";

        doThrow(new RuntimeException("boom")).when(restClient).deleteRelations(anyString(), anyString());

        String result = tools.deleteRelations(entityUuid.toString(), type);

        assertThat(result)
                .contains("\"status\":\"ERROR\"")
                .contains("\"entityId\":\"" + entityUuid + "\"")
                .contains("\"entityType\":\"" + type + "\"")
                .contains("boom");
    }

    @Test
    void testDeleteRelation_error() {
        when(clientService.getClient()).thenReturn(restClient);

        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();

        doThrow(new RuntimeException("boom"))
                .when(restClient)
                .deleteRelation(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());

        String res = tools.deleteRelation(fromId.toString(), "DEVICE", "Contains", null, toId.toString(), "ASSET");
        assertThat(res).contains("\"status\":\"ERROR\"").contains("boom");
    }

}
