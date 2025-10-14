package org.thingsboard.ai.mcp.server.service.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.ai.mcp.server.data.ThingsBoardEdition;
import org.thingsboard.ai.mcp.server.rest.RestClient;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.group.EntityGroupTools;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UUIDBased;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EntityGroupToolsTest {

    @InjectMocks
    private EntityGroupTools tools;

    @Mock
    private RestClientService clientService;

    @Mock
    private RestClient restClient;

    @Captor
    private ArgumentCaptor<List<EntityGroupId>> entityGroupIdsCaptor;

    @Captor
    private ArgumentCaptor<List<EntityId>> entityIdsCaptor;

    @BeforeEach
    void setup() {
        // Single place to stub the RestClient
        when(clientService.getClient()).thenReturn(restClient);
    }

    @Test
    void testFindEntityGroupById() {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.PE);

        UUID groupUuid = UUID.randomUUID();
        EntityGroupInfo entityGroupInfo = new EntityGroupInfo();
        when(restClient.getEntityGroupById(any(EntityGroupId.class))).thenReturn(Optional.of(entityGroupInfo));

        String result = tools.getEntityGroupById(groupUuid.toString());

        ArgumentCaptor<EntityGroupId> idCap = ArgumentCaptor.forClass(EntityGroupId.class);
        verify(restClient).getEntityGroupById(idCap.capture());

        assertThat(idCap.getValue().getId()).isEqualTo(groupUuid);
        assertThat(result).isEqualTo(JacksonUtil.toString(entityGroupInfo));
    }

    @ParameterizedTest(name = "getEntityGroupsByType → {0}")
    @CsvSource({"DEVICE", "ASSET", "CUSTOMER"})
    void testFindEntityGroupsByType(String typeStr) {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.PE);

        List<EntityGroupInfo> infos = List.of(new EntityGroupInfo(), new EntityGroupInfo());
        when(restClient.getEntityGroupsByType(EntityType.valueOf(typeStr))).thenReturn(infos);

        String result = tools.getEntityGroupsByType(typeStr);

        verify(restClient).getEntityGroupsByType(EntityType.valueOf(typeStr));
        assertThat(result).isEqualTo(JacksonUtil.toString(infos));
    }

    @ParameterizedTest
    @ValueSource(strings = {"TENANT", "CUSTOMER"})
    void testFindEntityGroupByOwnerAndNameAndType(String ownerType) {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.PE);

        UUID ownerUuid = UUID.randomUUID();
        String entityType = "ASSET";
        String name = "Group A";

        EntityGroupInfo info = new EntityGroupInfo();
        when(restClient.getEntityGroupInfoByOwnerAndNameAndType(any(EntityId.class), eq(EntityType.ASSET), eq(name)))
                .thenReturn(Optional.of(info));

        String result = tools.getEntityGroupByOwnerAndNameAndType(ownerType, ownerUuid.toString(), entityType, name);

        ArgumentCaptor<EntityId> ownerCap = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<EntityType> typeCap = ArgumentCaptor.forClass(EntityType.class);
        ArgumentCaptor<String> nameCap = ArgumentCaptor.forClass(String.class);
        verify(restClient).getEntityGroupInfoByOwnerAndNameAndType(ownerCap.capture(), typeCap.capture(), nameCap.capture());

        assertThat(ownerCap.getValue().getEntityType().name()).isEqualTo(ownerType);
        assertThat(ownerCap.getValue().getId()).isEqualTo(ownerUuid);
        assertThat(typeCap.getValue()).isEqualTo(EntityType.ASSET);
        assertThat(nameCap.getValue()).isEqualTo(name);
        assertThat(result).isEqualTo(JacksonUtil.toString(info));
    }

    @ParameterizedTest
    @CsvSource({"CUSTOMER,DEVICE", "TENANT,ASSET"})
    void testFindEntityGroupsByOwnerAndType(String ownerType, String typeStr) {
        UUID ownerUuid = UUID.randomUUID();
        List<EntityGroupInfo> infos = List.of(new EntityGroupInfo());
        when(restClient.getEntityGroupsByOwnerAndType(any(EntityId.class), eq(EntityType.valueOf(typeStr))))
                .thenReturn(infos);

        String result = tools.getEntityGroupsByOwnerAndType(ownerType, ownerUuid.toString(), typeStr);

        ArgumentCaptor<EntityId> ownerCap = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<EntityType> typeCap = ArgumentCaptor.forClass(EntityType.class);
        verify(restClient).getEntityGroupsByOwnerAndType(ownerCap.capture(), typeCap.capture());

        assertThat(ownerCap.getValue().getEntityType().name()).isEqualTo(ownerType);
        assertThat(ownerCap.getValue().getId()).isEqualTo(ownerUuid);
        assertThat(typeCap.getValue()).isEqualTo(EntityType.valueOf(typeStr));
        assertThat(result).isEqualTo(JacksonUtil.toString(infos));
    }

    @Test
    void testFindEntityGroupsForEntity() {
        String entityType = "DEVICE";
        UUID entityUuid = UUID.randomUUID();

        List<EntityGroupId> ids = List.of(new EntityGroupId(UUID.randomUUID()), new EntityGroupId(UUID.randomUUID()));
        when(restClient.getEntityGroupsForEntity(any(EntityId.class))).thenReturn(ids);

        String result = tools.getEntityGroupsForEntity(entityType, entityUuid.toString());

        ArgumentCaptor<EntityId> entityCap = ArgumentCaptor.forClass(EntityId.class);
        verify(restClient).getEntityGroupsForEntity(entityCap.capture());

        assertThat(entityCap.getValue().getEntityType().name()).isEqualTo(entityType);
        assertThat(entityCap.getValue().getId()).isEqualTo(entityUuid);
        assertThat(result).isEqualTo(JacksonUtil.toString(ids));
    }

    @Test
    void testFindEntityGroupsByIds() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        List<EntityGroup> infos = List.of(new EntityGroup());
        when(restClient.getEntityGroupsByIds(anyList())).thenReturn(infos);

        String result = tools.getEntityGroupsByIds(id1 + "," + id2);

        verify(restClient).getEntityGroupsByIds(entityGroupIdsCaptor.capture());

        List<EntityGroupId> passedIds = entityGroupIdsCaptor.getValue();
        assertThat(passedIds).extracting(UUIDBased::getId).containsExactlyInAnyOrder(id1, id2);
        assertThat(result).isEqualTo(JacksonUtil.toString(infos));
    }

    @Nested
    @DisplayName("save/delete EntityGroup")
    class SaveDelete {
        @Test
        void testSaveEntityGroup() {
            EntityGroup payload = new EntityGroup();
            payload.setName("Water meters");

            EntityGroupInfo returned = new EntityGroupInfo();
            returned.setName("Water meters");
            when(restClient.saveEntityGroup(any(EntityGroup.class))).thenReturn(returned);

            String res = tools.saveEntityGroup(JacksonUtil.toString(payload));

            ArgumentCaptor<EntityGroup> argCap = ArgumentCaptor.forClass(EntityGroup.class);
            verify(restClient).saveEntityGroup(argCap.capture());
            assertThat(argCap.getValue().getName()).isEqualTo("Water meters");

            assertThat(res).isEqualTo(JacksonUtil.toString(returned));
        }

        @Test
        void testDeleteEntityGroup_ok() {
            UUID id = UUID.randomUUID();
            String res = tools.deleteEntityGroup(id.toString());

            ArgumentCaptor<EntityGroupId> idCap = ArgumentCaptor.forClass(EntityGroupId.class);
            verify(restClient).deleteEntityGroup(idCap.capture());
            assertThat(idCap.getValue().getId()).isEqualTo(id);
            assertThat(res).contains("\"status\":\"OK\"");
        }

        @Test
        void testDeleteEntityGroup_error() {
            UUID id = UUID.randomUUID();
            doThrow(new RuntimeException("boom")).when(restClient).deleteEntityGroup(any(EntityGroupId.class));

            String res = tools.deleteEntityGroup(id.toString());

            assertThat(res).contains("ERROR").contains("boom").contains(id.toString());
        }

    }

    @Nested
    @DisplayName("add/remove entities")
    class AddRemoveEntities {
        @Test
        void testAddEntities_ok() {
            UUID group = UUID.randomUUID();
            UUID e1 = UUID.randomUUID();
            UUID e2 = UUID.randomUUID();

            String res = tools.addEntitiesToEntityGroup(group.toString(), e1 + "," + e2, "DEVICE");

            ArgumentCaptor<EntityGroupId> idCap = ArgumentCaptor.forClass(EntityGroupId.class);
            verify(restClient).addEntitiesToEntityGroup(idCap.capture(), entityIdsCaptor.capture());
            assertThat(idCap.getValue().getId()).isEqualTo(group);
            List<UUID> passed = entityIdsCaptor.getValue().stream().map(EntityId::getId).toList();
            assertThat(passed).containsExactlyInAnyOrder(e1, e2);
            assertThat(res).contains("\"status\":\"OK\"");
        }

        @Test
        void testAddEntities_error() {
            UUID group = UUID.randomUUID();
            doThrow(new RuntimeException("nope")).when(restClient).addEntitiesToEntityGroup(any(EntityGroupId.class), anyList());

            String res = tools.addEntitiesToEntityGroup(group.toString(), UUID.randomUUID().toString(), "DEVICE");
            assertThat(res).contains("ERROR").contains("nope");
        }

        @Test
        void testRemoveEntities_ok() {
            UUID group = UUID.randomUUID();
            UUID e1 = UUID.randomUUID();

            String res = tools.removeEntitiesFromEntityGroup(group.toString(), e1.toString(), "ASSET");

            ArgumentCaptor<EntityGroupId> idCap = ArgumentCaptor.forClass(EntityGroupId.class);
            verify(restClient).removeEntitiesFromEntityGroup(idCap.capture(), entityIdsCaptor.capture());
            assertThat(idCap.getValue().getId()).isEqualTo(group);
            assertThat(entityIdsCaptor.getValue()).hasSize(1);
            assertThat(entityIdsCaptor.getValue().get(0).getId()).isEqualTo(e1);
            assertThat(res).contains("\"success\": true");
        }

        @Test
        void testRemoveEntities_error() {
            UUID group = UUID.randomUUID();
            doThrow(new RuntimeException("fail")).when(restClient).removeEntitiesFromEntityGroup(any(EntityGroupId.class), anyList());

            String res = tools.removeEntitiesFromEntityGroup(group.toString(), UUID.randomUUID().toString(), "ASSET");
            assertThat(res).contains("ERROR").contains("fail");
        }

    }

}
