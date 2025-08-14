package org.thingsboard.ai.mcp.server.service.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.ai.mcp.server.rest.RestClient;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.relation.RelationTools;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationInfo;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RelationToolsTest {

    @InjectMocks
    private RelationTools tools;

    @Mock
    private RestClientService clientService;

    @Mock
    private RestClient restClient;

    @Test
    void testFindRelation_commonGroup() {
        when(clientService.getClient()).thenReturn(restClient);

        UUID fromUuid = UUID.randomUUID();
        UUID toUuid = UUID.randomUUID();

        EntityRelation relation = new EntityRelation();
        when(restClient.getRelation(any(EntityId.class), eq("Contains"), eq(RelationTypeGroup.COMMON), any(EntityId.class))).thenReturn(Optional.of(relation));

        String result = tools.getRelation(fromUuid.toString(), "DEVICE", "Contains", null, toUuid.toString(), "ASSET");

        ArgumentCaptor<EntityId> fromCap = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<String> typeCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<RelationTypeGroup> groupCap = ArgumentCaptor.forClass(RelationTypeGroup.class);
        ArgumentCaptor<EntityId> toCap = ArgumentCaptor.forClass(EntityId.class);

        verify(restClient).getRelation(fromCap.capture(), typeCap.capture(), groupCap.capture(), toCap.capture());

        assertThat(fromCap.getValue().getEntityType().name()).isEqualTo("DEVICE");
        assertThat(fromCap.getValue().getId()).isEqualTo(fromUuid);
        assertThat(typeCap.getValue()).isEqualTo("Contains");
        assertThat(groupCap.getValue()).isEqualTo(RelationTypeGroup.COMMON);
        assertThat(toCap.getValue().getEntityType().name()).isEqualTo("ASSET");
        assertThat(toCap.getValue().getId()).isEqualTo(toUuid);

        assertThat(result).isEqualTo(JacksonUtil.toString(relation));
    }

    @Test
    void testFindRelation_withGroupProvided() {
        when(clientService.getClient()).thenReturn(restClient);

        UUID fromUuid = UUID.randomUUID();
        UUID toUuid = UUID.randomUUID();

        EntityRelation relation = new EntityRelation();
        when(restClient.getRelation(any(EntityId.class), eq("Manages"), eq(RelationTypeGroup.RULE_CHAIN), any(EntityId.class))).thenReturn(Optional.of(relation));

        String result = tools.getRelation(fromUuid.toString(), "USER", "Manages", "RULE_CHAIN", toUuid.toString(), "DEVICE");

        ArgumentCaptor<RelationTypeGroup> groupCap = ArgumentCaptor.forClass(RelationTypeGroup.class);
        verify(restClient).getRelation(any(EntityId.class), eq("Manages"), groupCap.capture(), any(EntityId.class));

        assertThat(groupCap.getValue()).isEqualTo(RelationTypeGroup.RULE_CHAIN);
        assertThat(result).isEqualTo(JacksonUtil.toString(relation));
    }

    @Test
    void testFindByFrom_common() {
        when(clientService.getClient()).thenReturn(restClient);

        UUID fromUuid = UUID.randomUUID();

        List<EntityRelation> relations = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            relations.add(new EntityRelation());
        }
        when(restClient.findByFrom(any(EntityId.class), eq(RelationTypeGroup.COMMON))).thenReturn(relations);

        String result = tools.findByFrom(fromUuid.toString(), "ASSET", null);

        ArgumentCaptor<EntityId> entityCap = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<RelationTypeGroup> groupCap = ArgumentCaptor.forClass(RelationTypeGroup.class);
        verify(restClient).findByFrom(entityCap.capture(), groupCap.capture());

        assertThat(entityCap.getValue().getEntityType().name()).isEqualTo("ASSET");
        assertThat(entityCap.getValue().getId()).isEqualTo(fromUuid);
        assertThat(groupCap.getValue()).isEqualTo(RelationTypeGroup.COMMON);

        assertThat(result).isEqualTo(JacksonUtil.toString(relations));
    }

    @Test
    void testFindInfoByFrom_alarmGroup() {
        when(clientService.getClient()).thenReturn(restClient);

        UUID fromUuid = UUID.randomUUID();

        List<EntityRelationInfo> relationInfos = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            relationInfos.add(new EntityRelationInfo());
        }
        when(restClient.findInfoByFrom(any(EntityId.class), eq(RelationTypeGroup.RULE_CHAIN))).thenReturn(relationInfos);

        String result = tools.findInfoByFrom(fromUuid.toString(), "DEVICE", "RULE_CHAIN");

        ArgumentCaptor<EntityId> entityCap = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<RelationTypeGroup> groupCap = ArgumentCaptor.forClass(RelationTypeGroup.class);
        verify(restClient).findInfoByFrom(entityCap.capture(), groupCap.capture());

        assertThat(entityCap.getValue().getEntityType().name()).isEqualTo("DEVICE");
        assertThat(entityCap.getValue().getId()).isEqualTo(fromUuid);
        assertThat(groupCap.getValue()).isEqualTo(RelationTypeGroup.RULE_CHAIN);

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
        when(restClient.findByFrom(any(EntityId.class), eq("Owns"), eq(RelationTypeGroup.COMMON))).thenReturn(relations);

        String result = tools.findByFromWithRelationType(fromUuid.toString(), "TENANT", "Owns", null);

        ArgumentCaptor<EntityId> entityCap = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<String> typeCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<RelationTypeGroup> groupCap = ArgumentCaptor.forClass(RelationTypeGroup.class);
        verify(restClient).findByFrom(entityCap.capture(), typeCap.capture(), groupCap.capture());

        assertThat(entityCap.getValue().getEntityType().name()).isEqualTo("TENANT");
        assertThat(entityCap.getValue().getId()).isEqualTo(fromUuid);
        assertThat(typeCap.getValue()).isEqualTo("Owns");
        assertThat(groupCap.getValue()).isEqualTo(RelationTypeGroup.COMMON);

        assertThat(result).isEqualTo(JacksonUtil.toString(relations));
    }

    @Test
    void testFindByTo_common() {
        when(clientService.getClient()).thenReturn(restClient);

        UUID toUuid = UUID.randomUUID();

        List<EntityRelation> relations = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            relations.add(new EntityRelation());
        }
        when(restClient.findByTo(any(EntityId.class), eq(RelationTypeGroup.COMMON))).thenReturn(relations);

        String result = tools.findByTo(toUuid.toString(), "CUSTOMER", null);

        ArgumentCaptor<EntityId> entityCap = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<RelationTypeGroup> groupCap = ArgumentCaptor.forClass(RelationTypeGroup.class);
        verify(restClient).findByTo(entityCap.capture(), groupCap.capture());

        assertThat(entityCap.getValue().getEntityType().name()).isEqualTo("CUSTOMER");
        assertThat(entityCap.getValue().getId()).isEqualTo(toUuid);
        assertThat(groupCap.getValue()).isEqualTo(RelationTypeGroup.COMMON);

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
        when(restClient.findInfoByTo(any(EntityId.class), eq(RelationTypeGroup.RULE_CHAIN))).thenReturn(relationInfos);

        String result = tools.findInfoByTo(fromUuid.toString(), "DEVICE", "RULE_CHAIN");

        ArgumentCaptor<EntityId> entityCap = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<RelationTypeGroup> groupCap = ArgumentCaptor.forClass(RelationTypeGroup.class);
        verify(restClient).findInfoByTo(entityCap.capture(), groupCap.capture());

        assertThat(entityCap.getValue().getEntityType().name()).isEqualTo("DEVICE");
        assertThat(entityCap.getValue().getId()).isEqualTo(fromUuid);
        assertThat(groupCap.getValue()).isEqualTo(RelationTypeGroup.RULE_CHAIN);

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
        when(restClient.findByTo(any(EntityId.class), eq("Contains"), eq(RelationTypeGroup.DASHBOARD))).thenReturn(relations);

        String result = tools.findByToWithRelationType(toUuid.toString(), "DASHBOARD", "Contains", RelationTypeGroup.DASHBOARD.name());

        ArgumentCaptor<EntityId> entityCap = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<String> typeCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<RelationTypeGroup> groupCap = ArgumentCaptor.forClass(RelationTypeGroup.class);
        verify(restClient).findByTo(entityCap.capture(), typeCap.capture(), groupCap.capture());

        assertThat(entityCap.getValue().getEntityType().name()).isEqualTo("DASHBOARD");
        assertThat(entityCap.getValue().getId()).isEqualTo(toUuid);
        assertThat(typeCap.getValue()).isEqualTo("Contains");
        assertThat(groupCap.getValue()).isEqualTo(RelationTypeGroup.DASHBOARD);

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
        when(restClient.findByTo(any(EntityId.class), eq("Contains"), eq(RelationTypeGroup.COMMON))).thenReturn(relations);

        String result = tools.findByToWithRelationType(toUuid.toString(), "DASHBOARD", "Contains", null);

        ArgumentCaptor<EntityId> entityCap = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<String> typeCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<RelationTypeGroup> groupCap = ArgumentCaptor.forClass(RelationTypeGroup.class);
        verify(restClient).findByTo(entityCap.capture(), typeCap.capture(), groupCap.capture());

        assertThat(entityCap.getValue().getEntityType().name()).isEqualTo("DASHBOARD");
        assertThat(entityCap.getValue().getId()).isEqualTo(toUuid);
        assertThat(typeCap.getValue()).isEqualTo("Contains");
        assertThat(groupCap.getValue()).isEqualTo(RelationTypeGroup.COMMON);

        assertThat(result).isEqualTo(JacksonUtil.toString(relations));
    }

}
