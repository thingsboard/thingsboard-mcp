package org.thingsboard.ai.mcp.server.service.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.ai.mcp.server.data.ThingsBoardEdition;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.user.UserTools;
import org.thingsboard.ai.mcp.server.util.JacksonUtil;
import org.thingsboard.client.ThingsboardClient;
import org.thingsboard.client.model.PageDataUser;
import org.thingsboard.client.model.PageDataUserEmailInfo;
import org.thingsboard.client.model.User;
import org.thingsboard.client.model.UserEmailInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserToolsTest {

    @InjectMocks
    private UserTools tools;

    @Mock
    private ThingsboardClient restClient;

    @Mock
    private RestClientService clientService;

    @BeforeEach
    void setup() {
        when(clientService.getClient()).thenReturn(restClient);
    }

    private static User createUser() {
        return new User().email("test@test.com");
    }

    private static PageDataUser page(List<User> users) {
        return new PageDataUser(1, (long) users.size(), false).data(users);
    }

    @Test
    void testSaveUser_defaultSendActivationEmail_true_noGroups() {
        User payload = new User().email("john@ex.com").firstName("John");

        when(restClient.saveUser(any(User.class), eq("true"), isNull(), isNull()))
                .thenReturn(payload);

        String result = tools.saveUser(JacksonUtil.toString(payload), null, null, null);

        verify(restClient).saveUser(any(User.class), eq("true"), isNull(), isNull());
        assertThat(result).isEqualTo(JacksonUtil.toString(payload));
    }

    @Test
    void testSaveUser_withEntityGroupId() {
        User payload = new User().email("jane@ex.com");

        String groupId = UUID.randomUUID().toString();
        when(restClient.saveUser(any(User.class), eq("true"), eq(groupId), isNull()))
                .thenReturn(payload);

        String result = tools.saveUser(JacksonUtil.toString(payload), true, groupId, null);

        verify(restClient).saveUser(any(User.class), eq("true"), eq(groupId), isNull());
        assertThat(result).isEqualTo(JacksonUtil.toString(payload));
    }

    @Test
    void testSaveUser_withEntityGroupIds() {
        User payload = new User().email("kate@ex.com");

        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();
        String groupIds = id1 + "," + id2;
        when(restClient.saveUser(any(User.class), eq("true"), isNull(), eq(List.of(id1, id2))))
                .thenReturn(payload);

        String result = tools.saveUser(JacksonUtil.toString(payload), true, null, groupIds);

        verify(restClient).saveUser(any(User.class), eq("true"), isNull(), eq(List.of(id1, id2)));
        assertThat(result).isEqualTo(JacksonUtil.toString(payload));
    }

    @Test
    void testDeleteUser_ok() {
        String id = UUID.randomUUID().toString();

        String result = tools.deleteUser(id);

        verify(restClient).deleteUser(eq(id));
        assertThat(result).contains("\"status\":\"OK\"").contains(id);
    }

    @Test
    void testDeleteUser_error() {
        String id = UUID.randomUUID().toString();
        doThrow(new RuntimeException("boom")).when(restClient).deleteUser(anyString());

        String result = tools.deleteUser(id);

        assertThat(result).contains("\"status\":\"ERROR\"").contains("boom").contains(id);
    }

    @Test
    void testFindUserById() {
        String id = UUID.randomUUID().toString();
        User user = createUser();
        when(restClient.getUserById(anyString())).thenReturn(user);

        String result = tools.getUserById(id);

        verify(restClient).getUserById(eq(id));
        assertThat(result).isEqualTo(JacksonUtil.toString(user));
    }

    @Test
    void testFindUsers() throws Exception {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            users.add(createUser());
        }
        PageDataUser pageData = page(users);
        when(restClient.getAllCustomerUsers(anyInt(), anyInt(), any(), any(), any())).thenReturn(pageData);

        String result = tools.getUsers("25", "0", null, null, null);

        verify(restClient).getAllCustomerUsers(eq(25), eq(0), isNull(), isNull(), isNull());
        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @Test
    void testFindTenantAdmins() throws Exception {
        String tenantId = UUID.randomUUID().toString();
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            users.add(createUser());
        }
        PageDataUser pageData = page(users);
        when(restClient.getTenantAdmins(anyString(), anyInt(), anyInt(), any(), any(), any())).thenReturn(pageData);

        String result = tools.getTenantAdmins(tenantId, "50", "1", "john", "email", "ASC");

        verify(restClient).getTenantAdmins(eq(tenantId), eq(50), eq(1), eq("john"), eq("email"), eq("ASC"));
        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @Test
    void testFindCustomerUsers() throws Exception {
        String customerId = UUID.randomUUID().toString();
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            users.add(createUser());
        }
        PageDataUser pageData = page(users);
        when(restClient.getCustomerUsers(anyString(), anyInt(), anyInt(), any(), any(), any())).thenReturn(pageData);

        String result = tools.getCustomerUsers(customerId, "10", "2", "a", "firstName", "DESC");

        verify(restClient).getCustomerUsers(eq(customerId), eq(10), eq(2), eq("a"), eq("firstName"), eq("DESC"));
        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @Test
    void testFindAllCustomerUsers_peEdition() throws Exception {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.PE);

        List<User> users = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            users.add(createUser());
        }
        PageDataUser pageData = page(users);
        when(restClient.getAllCustomerUsers(anyInt(), anyInt(), any(), any(), any())).thenReturn(pageData);

        String result = tools.getAllCustomerUsers("15", "3", "x", "lastName", "ASC");

        verify(restClient).getAllCustomerUsers(eq(15), eq(3), eq("x"), eq("lastName"), eq("ASC"));
        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @Test
    void testFindUsersForAssign() throws Exception {
        String alarmId = UUID.randomUUID().toString();

        List<UserEmailInfo> users = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            users.add(new UserEmailInfo().email("email").firstName("name").lastName("surname"));
        }
        PageDataUserEmailInfo pageData = new PageDataUserEmailInfo(1, (long) users.size(), false).data(users);
        when(restClient.getUsersForAssign(anyString(), anyInt(), anyInt(), any(), any(), any())).thenReturn(pageData);

        String result = tools.getUsersForAssign(alarmId, "30", "0", "doe", "email", "ASC");

        verify(restClient).getUsersForAssign(eq(alarmId), eq(30), eq(0), eq("doe"), eq("email"), eq("ASC"));
        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @Test
    void testFindUsersByEntityGroupId_peEdition() throws Exception {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.PE);

        String groupId = UUID.randomUUID().toString();
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            users.add(createUser());
        }
        PageDataUser pageData = page(users);
        when(restClient.getUsersByEntityGroupId(anyString(), anyInt(), anyInt(), any(), any(), any())).thenReturn(pageData);

        String result = tools.getUsersByEntityGroupId(groupId, "40", "4", "k", "createdTime", "DESC");

        verify(restClient).getUsersByEntityGroupId(eq(groupId), eq(40), eq(4), eq("k"), eq("createdTime"), eq("DESC"));
        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

}
