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
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.group.EntityGroupTools;
import org.thingsboard.ai.mcp.server.util.JacksonUtil;
import org.thingsboard.client.ThingsboardClient;
import org.thingsboard.client.model.EntityGroup;
import org.thingsboard.client.model.EntityGroupId;
import org.thingsboard.client.model.EntityGroupInfo;
import org.thingsboard.client.model.PageDataEntityGroupInfo;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    private ThingsboardClient restClient;

    @Captor
    private ArgumentCaptor<List<String>> stringListCaptor;

    @BeforeEach
    void setup() {
        when(clientService.getClient()).thenReturn(restClient);
    }

    @Test
    void testFindEntityGroupById() {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.PE);

        String groupId = UUID.randomUUID().toString();
        EntityGroupInfo entityGroupInfo = new EntityGroupInfo();
        when(restClient.getEntityGroupById(anyString())).thenReturn(entityGroupInfo);

        String result = tools.getEntityGroupById(groupId);

        verify(restClient).getEntityGroupById(eq(groupId));
        assertThat(result).isEqualTo(JacksonUtil.toString(entityGroupInfo));
    }

    @ParameterizedTest(name = "getEntityGroupsByType → {0}")
    @CsvSource({"DEVICE", "ASSET", "CUSTOMER"})
    void testFindEntityGroupsByType(String typeStr) {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.PE);

        List<EntityGroupInfo> infos = List.of(new EntityGroupInfo(), new EntityGroupInfo());
        when(restClient.getAllEntityGroupsByType(eq(typeStr), any())).thenReturn(infos);

        String result = tools.getEntityGroupsByType(typeStr);

        verify(restClient).getAllEntityGroupsByType(eq(typeStr), any());
        assertThat(result).isEqualTo(JacksonUtil.toString(infos));
    }

    @ParameterizedTest
    @ValueSource(strings = {"TENANT", "CUSTOMER"})
    void testFindEntityGroupByOwnerAndNameAndType(String ownerType) {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.PE);

        String ownerId = UUID.randomUUID().toString();
        String entityType = "ASSET";
        String name = "Group A";

        EntityGroupInfo info = new EntityGroupInfo();
        when(restClient.getEntityGroupByOwnerAndNameAndType(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(info);

        String result = tools.getEntityGroupByOwnerAndNameAndType(ownerType, ownerId, entityType, name);

        ArgumentCaptor<String> ownerTypeCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> ownerIdCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> typeCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> nameCap = ArgumentCaptor.forClass(String.class);
        verify(restClient).getEntityGroupByOwnerAndNameAndType(ownerTypeCap.capture(), ownerIdCap.capture(), typeCap.capture(), nameCap.capture());

        assertThat(ownerTypeCap.getValue()).isEqualTo(ownerType);
        assertThat(ownerIdCap.getValue()).isEqualTo(ownerId);
        assertThat(typeCap.getValue()).isEqualTo("ASSET");
        assertThat(nameCap.getValue()).isEqualTo(name);
        assertThat(result).isEqualTo(JacksonUtil.toString(info));
    }

    @ParameterizedTest
    @CsvSource({"CUSTOMER,DEVICE", "TENANT,ASSET"})
    void testFindEntityGroupsByOwnerAndType(String ownerType, String typeStr) {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.PE);

        String ownerId = UUID.randomUUID().toString();
        PageDataEntityGroupInfo pageData = new PageDataEntityGroupInfo(1, 1L, false)
                .data(List.of(new EntityGroupInfo()));
        when(restClient.getEntityGroupsByOwnerAndTypeAndPageLink(anyString(), anyString(), anyString(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(pageData);

        String result = tools.getEntityGroupsByOwnerAndType(ownerType, ownerId, typeStr);

        verify(restClient).getEntityGroupsByOwnerAndTypeAndPageLink(eq(ownerType), eq(ownerId), eq(typeStr), eq("1000"), eq("0"), any(), any(), any());
        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @Test
    void testFindEntityGroupsForEntity() {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.PE);

        String entityType = "DEVICE";
        String entityId = UUID.randomUUID().toString();

        List<EntityGroupId> ids = List.of(new EntityGroupId().id(UUID.randomUUID()), new EntityGroupId().id(UUID.randomUUID()));
        when(restClient.getEntityGroupsForEntity(anyString(), anyString())).thenReturn(ids);

        String result = tools.getEntityGroupsForEntity(entityType, entityId);

        verify(restClient).getEntityGroupsForEntity(eq(entityType), eq(entityId));
        assertThat(result).isEqualTo(JacksonUtil.toString(ids));
    }

    @Test
    void testFindEntityGroupsByIds() {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.PE);

        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();

        List<EntityGroupInfo> infos = List.of(new EntityGroupInfo());
        when(restClient.getEntityGroupsByIds(anyList())).thenReturn(infos);

        String result = tools.getEntityGroupsByIds(id1 + "," + id2);

        verify(restClient).getEntityGroupsByIds(stringListCaptor.capture());

        List<String> passedIds = stringListCaptor.getValue();
        assertThat(passedIds).containsExactlyInAnyOrder(id1, id2);
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
            String id = UUID.randomUUID().toString();
            String res = tools.deleteEntityGroup(id);

            verify(restClient).deleteEntityGroup(eq(id));
            assertThat(res).contains("\"status\":\"OK\"");
        }

        @Test
        void testDeleteEntityGroup_error() {
            String id = UUID.randomUUID().toString();
            doThrow(new RuntimeException("boom")).when(restClient).deleteEntityGroup(anyString());

            String res = tools.deleteEntityGroup(id);

            assertThat(res).contains("ERROR").contains("boom").contains(id);
        }

    }

    @Nested
    @DisplayName("add/remove entities")
    class AddRemoveEntities {
        @Test
        void testAddEntities_ok() {
            String group = UUID.randomUUID().toString();
            String e1 = UUID.randomUUID().toString();
            String e2 = UUID.randomUUID().toString();

            String res = tools.addEntitiesToEntityGroup(group, e1 + "," + e2, "DEVICE");

            verify(restClient).addEntitiesToEntityGroup(eq(group), stringListCaptor.capture());
            List<String> passed = stringListCaptor.getValue();
            assertThat(passed).containsExactlyInAnyOrder(e1, e2);
            assertThat(res).contains("\"status\":\"OK\"");
        }

        @Test
        void testAddEntities_error() {
            String group = UUID.randomUUID().toString();
            doThrow(new RuntimeException("nope")).when(restClient).addEntitiesToEntityGroup(anyString(), anyList());

            String res = tools.addEntitiesToEntityGroup(group, UUID.randomUUID().toString(), "DEVICE");
            assertThat(res).contains("ERROR").contains("nope");
        }

        @Test
        void testRemoveEntities_ok() {
            String group = UUID.randomUUID().toString();
            String e1 = UUID.randomUUID().toString();

            String res = tools.removeEntitiesFromEntityGroup(group, e1, "ASSET");

            verify(restClient).removeEntitiesFromEntityGroup(eq(group), stringListCaptor.capture());
            assertThat(stringListCaptor.getValue()).hasSize(1);
            assertThat(stringListCaptor.getValue().get(0)).isEqualTo(e1);
            assertThat(res).contains("\"success\": true");
        }

        @Test
        void testRemoveEntities_error() {
            String group = UUID.randomUUID().toString();
            doThrow(new RuntimeException("fail")).when(restClient).removeEntitiesFromEntityGroup(anyString(), anyList());

            String res = tools.removeEntitiesFromEntityGroup(group, UUID.randomUUID().toString(), "ASSET");
            assertThat(res).contains("ERROR").contains("fail");
        }

    }

}
