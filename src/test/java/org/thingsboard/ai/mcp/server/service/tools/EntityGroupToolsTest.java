package org.thingsboard.ai.mcp.server.service.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PE_ONLY_AVAILABLE;

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

    @Test
    void testFindEntityGroupById_ceEdition() {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.CE);

        String result = tools.getEntityGroupById(UUID.randomUUID().toString());

        verify(clientService, never()).getClient();
        assertThat(result).isEqualTo(PE_ONLY_AVAILABLE);
    }

    @Test
    void testFindEntityGroupById_peEdition() {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.PE);
        when(clientService.getClient()).thenReturn(restClient);

        UUID groupUuid = UUID.randomUUID();
        EntityGroupInfo entityGroupInfo = new EntityGroupInfo();
        when(restClient.getEntityGroupById(any(EntityGroupId.class))).thenReturn(Optional.of(entityGroupInfo));

        String result = tools.getEntityGroupById(groupUuid.toString());

        ArgumentCaptor<EntityGroupId> idCap = ArgumentCaptor.forClass(EntityGroupId.class);
        verify(restClient).getEntityGroupById(idCap.capture());

        assertThat(idCap.getValue().getId()).isEqualTo(groupUuid);
        assertThat(result).isEqualTo(JacksonUtil.toString(entityGroupInfo));
    }

    @Test
    void testFindEntityGroupsByType_ceEdition() {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.CE);

        String result = tools.getEntityGroupsByType("DEVICE");

        verify(clientService, never()).getClient();
        assertThat(result).isEqualTo(PE_ONLY_AVAILABLE);
    }

    @Test
    void testFindEntityGroupsByType_peEdition() {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.PE);
        when(clientService.getClient()).thenReturn(restClient);

        List<EntityGroupInfo> entityGroupInfos = List.of(new EntityGroupInfo(), new EntityGroupInfo());
        when(restClient.getEntityGroupsByType(eq(EntityType.DEVICE))).thenReturn(entityGroupInfos);

        String result = tools.getEntityGroupsByType("DEVICE");

        verify(restClient).getEntityGroupsByType(eq(EntityType.DEVICE));
        assertThat(result).isEqualTo(JacksonUtil.toString(entityGroupInfos));
    }

    @Test
    void testFindEntityGroupByOwnerAndNameAndType_ceEdition() {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.CE);

        String result = tools.getEntityGroupByOwnerAndNameAndType("TENANT", UUID.randomUUID().toString(), "ASSET", "Group A");

        verify(clientService, never()).getClient();
        assertThat(result).isEqualTo(PE_ONLY_AVAILABLE);
    }

    @Test
    void testFindEntityGroupByOwnerAndNameAndType_peEdition() {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.PE);
        when(clientService.getClient()).thenReturn(restClient);

        String ownerType = "TENANT";
        UUID ownerUuid = UUID.randomUUID();
        String entityType = "ASSET";
        String name = "Group A";

        EntityGroupInfo entityGroupInfo = new EntityGroupInfo();
        when(restClient.getEntityGroupInfoByOwnerAndNameAndType(any(EntityId.class), eq(EntityType.ASSET), eq(name))).thenReturn(Optional.of(entityGroupInfo));

        String result = tools.getEntityGroupByOwnerAndNameAndType(ownerType, ownerUuid.toString(), entityType, name);

        ArgumentCaptor<EntityId> ownerCap = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<EntityType> typeCap = ArgumentCaptor.forClass(EntityType.class);
        ArgumentCaptor<String> nameCap = ArgumentCaptor.forClass(String.class);
        verify(restClient).getEntityGroupInfoByOwnerAndNameAndType(ownerCap.capture(), typeCap.capture(), nameCap.capture());

        assertThat(ownerCap.getValue().getEntityType().name()).isEqualTo(ownerType);
        assertThat(ownerCap.getValue().getId()).isEqualTo(ownerUuid);
        assertThat(typeCap.getValue()).isEqualTo(EntityType.ASSET);
        assertThat(nameCap.getValue()).isEqualTo(name);
        assertThat(result).isEqualTo(JacksonUtil.toString(entityGroupInfo));
    }

    @Test
    void testFindEntityGroupsByOwnerAndType_ceEdition() {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.CE);

        String result = tools.getEntityGroupsByOwnerAndType("CUSTOMER", UUID.randomUUID().toString(), "DEVICE");

        verify(clientService, never()).getClient();
        assertThat(result).isEqualTo(PE_ONLY_AVAILABLE);
    }

    @Test
    void testFindEntityGroupsByOwnerAndType_peEdition() {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.PE);
        when(clientService.getClient()).thenReturn(restClient);

        String ownerType = "CUSTOMER";
        UUID ownerUuid = UUID.randomUUID();
        String entityType = "DEVICE";

        List<EntityGroupInfo> entityGroupInfos = List.of(new EntityGroupInfo());
        when(restClient.getEntityGroupsByOwnerAndType(any(EntityId.class), eq(EntityType.DEVICE))).thenReturn(entityGroupInfos);

        String result = tools.getEntityGroupsByOwnerAndType(ownerType, ownerUuid.toString(), entityType);

        ArgumentCaptor<EntityId> ownerCap = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<EntityType> typeCap = ArgumentCaptor.forClass(EntityType.class);
        verify(restClient).getEntityGroupsByOwnerAndType(ownerCap.capture(), typeCap.capture());

        assertThat(ownerCap.getValue().getEntityType().name()).isEqualTo(ownerType);
        assertThat(ownerCap.getValue().getId()).isEqualTo(ownerUuid);
        assertThat(typeCap.getValue()).isEqualTo(EntityType.DEVICE);
        assertThat(result).isEqualTo(JacksonUtil.toString(entityGroupInfos));
    }

    @Test
    void testFindEntityGroupsForEntity_ceEdition() {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.CE);

        String result = tools.getEntityGroupsForEntity("DEVICE", UUID.randomUUID().toString());

        verify(clientService, never()).getClient();
        assertThat(result).isEqualTo(PE_ONLY_AVAILABLE);
    }

    @Test
    void testFindEntityGroupsForEntity_peEdition() {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.PE);
        when(clientService.getClient()).thenReturn(restClient);

        String entityType = "DEVICE";
        UUID entityUuid = UUID.randomUUID();

        List<EntityGroupId> entityGroupInfos = List.of(new EntityGroupId(UUID.randomUUID()), new EntityGroupId(UUID.randomUUID()));
        when(restClient.getEntityGroupsForEntity(any(EntityId.class))).thenReturn(entityGroupInfos);

        String result = tools.getEntityGroupsForEntity(entityType, entityUuid.toString());

        ArgumentCaptor<EntityId> entityCap = ArgumentCaptor.forClass(EntityId.class);
        verify(restClient).getEntityGroupsForEntity(entityCap.capture());

        assertThat(entityCap.getValue().getEntityType().name()).isEqualTo(entityType);
        assertThat(entityCap.getValue().getId()).isEqualTo(entityUuid);
        assertThat(result).isEqualTo(JacksonUtil.toString(entityGroupInfos));
    }

    @Test
    void testFindEntityGroupsByIds_ceEdition() {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.CE);

        String result = tools.getEntityGroupsByIds(UUID.randomUUID().toString(), UUID.randomUUID().toString());

        verify(clientService, never()).getClient();
        assertThat(result).isEqualTo(PE_ONLY_AVAILABLE);
    }

    @Test
    void testFindEntityGroupsByIds_peEdition() {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.PE);
        when(clientService.getClient()).thenReturn(restClient);

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        List<EntityGroup> entityGroupInfos = List.of(new EntityGroup());
        when(restClient.getEntityGroupsByIds(anyList())).thenReturn(entityGroupInfos);

        String result = tools.getEntityGroupsByIds(id1.toString(), id2.toString());

        verify(restClient).getEntityGroupsByIds(entityGroupIdsCaptor.capture());

        List<EntityGroupId> passedIds = entityGroupIdsCaptor.getValue();
        assertThat(passedIds).extracting(UUIDBased::getId).containsExactlyInAnyOrder(id1, id2);
        assertThat(result).isEqualTo(JacksonUtil.toString(entityGroupInfos));
    }

}
