package org.thingsboard.ai.mcp.server.service.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.ai.mcp.server.data.ThingsBoardEdition;
import org.thingsboard.ai.mcp.server.rest.RestClient;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.user.UserTools;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.UserEmailInfo;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserToolsTest {

    @InjectMocks
    private UserTools tools;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClientService clientService;

    @BeforeEach
    void setup() {
        when(clientService.getClient()).thenReturn(restClient);
    }

    private static User createUser(UUID id) {
        User u = new User();
        u.setId(new UserId(id));
        return u;
    }

    private static PageData<User> page(List<User> users) {
        return new PageData<>(users, 1, users.size(), false);
    }

    @Test
    void testFindUserById() {
        UUID id = UUID.randomUUID();
        User user = createUser(id);
        when(restClient.getUserById(any(UserId.class))).thenReturn(Optional.of(user));

        String result = tools.getUserById(id.toString());

        ArgumentCaptor<UserId> idCap = ArgumentCaptor.forClass(UserId.class);
        verify(restClient).getUserById(idCap.capture());
        assertThat(idCap.getValue().getId()).isEqualTo(id);

        assertThat(result).isEqualTo(JacksonUtil.toString(user));
    }

    @Test
    void testFindUsers() throws Exception {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            users.add(createUser(UUID.randomUUID()));
        }
        PageData<User> pageData = page(users);
        when(restClient.getUsers(any(PageLink.class))).thenReturn(pageData);

        String result = tools.getUsers(25, 0, null, null, null);

        ArgumentCaptor<PageLink> pageCap = ArgumentCaptor.forClass(PageLink.class);
        verify(restClient).getUsers(pageCap.capture());
        PageLink pl = pageCap.getValue();
        assertThat(pl.getPageSize()).isEqualTo(25);
        assertThat(pl.getPage()).isEqualTo(0);

        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @Test
    void testFindTenantAdmins() throws Exception {
        UUID tenantUuid = UUID.randomUUID();
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            users.add(createUser(UUID.randomUUID()));
        }
        PageData<User> pageData = page(users);
        when(restClient.getTenantAdmins(any(TenantId.class), any(PageLink.class))).thenReturn(pageData);

        String result = tools.getTenantAdmins(tenantUuid.toString(), 50, 1, "john", "email", "ASC");

        ArgumentCaptor<TenantId> tenantCap = ArgumentCaptor.forClass(TenantId.class);
        ArgumentCaptor<PageLink> pageCap = ArgumentCaptor.forClass(PageLink.class);
        verify(restClient).getTenantAdmins(tenantCap.capture(), pageCap.capture());
        assertThat(tenantCap.getValue().getId()).isEqualTo(tenantUuid);

        PageLink pl = pageCap.getValue();
        assertThat(pl.getPageSize()).isEqualTo(50);
        assertThat(pl.getPage()).isEqualTo(1);
        assertThat(pl.getTextSearch()).isEqualTo("john");

        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @Test
    void testFindCustomerUsers() throws Exception {
        UUID customerUuid = UUID.randomUUID();
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            users.add(createUser(UUID.randomUUID()));
        }
        PageData<User> pageData = page(users);
        when(restClient.getCustomerUsers(any(CustomerId.class), any(PageLink.class))).thenReturn(pageData);

        String result = tools.getCustomerUsers(customerUuid.toString(), 10, 2, "a", "firstName", "DESC");

        ArgumentCaptor<CustomerId> custCap = ArgumentCaptor.forClass(CustomerId.class);
        ArgumentCaptor<PageLink> pageCap = ArgumentCaptor.forClass(PageLink.class);
        verify(restClient).getCustomerUsers(custCap.capture(), pageCap.capture());
        assertThat(custCap.getValue().getId()).isEqualTo(customerUuid);

        PageLink pl = pageCap.getValue();
        assertThat(pl.getPageSize()).isEqualTo(10);
        assertThat(pl.getPage()).isEqualTo(2);
        assertThat(pl.getTextSearch()).isEqualTo("a");

        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @Test
    void testFindAllCustomerUsers_peEdition() throws Exception {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.PE);

        List<User> users = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            users.add(createUser(UUID.randomUUID()));
        }
        PageData<User> pageData = page(users);
        when(restClient.getAllCustomerUsers(any(PageLink.class))).thenReturn(pageData);

        String result = tools.getAllCustomerUsers(15, 3, "x", "lastName", "ASC");

        ArgumentCaptor<PageLink> pageCap = ArgumentCaptor.forClass(PageLink.class);
        verify(restClient).getAllCustomerUsers(pageCap.capture());
        PageLink pl = pageCap.getValue();
        assertThat(pl.getPageSize()).isEqualTo(15);
        assertThat(pl.getPage()).isEqualTo(3);
        assertThat(pl.getTextSearch()).isEqualTo("x");

        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @Test
    void testFindUsersForAssign() throws Exception {
        UUID alarmUuid = UUID.randomUUID();

        List<UserEmailInfo> users = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            users.add(new UserEmailInfo(new UserId(UUID.randomUUID()), "email", "name", "surname"));
        }
        PageData<UserEmailInfo> pageData = new PageData<>(users, 1, users.size(), false);
        when(restClient.getUsersForAssign(any(AlarmId.class), any(PageLink.class))).thenReturn(pageData);

        String result = tools.getUsersForAssign(alarmUuid.toString(), 30, 0, "doe", "email", "ASC");

        ArgumentCaptor<AlarmId> alarmCap = ArgumentCaptor.forClass(AlarmId.class);
        ArgumentCaptor<PageLink> pageCap = ArgumentCaptor.forClass(PageLink.class);
        verify(restClient).getUsersForAssign(alarmCap.capture(), pageCap.capture());
        assertThat(alarmCap.getValue().getId()).isEqualTo(alarmUuid);

        PageLink pl = pageCap.getValue();
        assertThat(pl.getPageSize()).isEqualTo(30);
        assertThat(pl.getPage()).isEqualTo(0);
        assertThat(pl.getTextSearch()).isEqualTo("doe");

        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @Test
    void testFindUsersByEntityGroupId_peEdition() throws Exception {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.PE);

        UUID groupId = UUID.randomUUID();
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            users.add(createUser(UUID.randomUUID()));
        }
        PageData<User> pageData = page(users);
        when(restClient.getUsersByEntityGroupId(any(EntityGroupId.class), any(PageLink.class))).thenReturn(pageData);

        String result = tools.getUsersByEntityGroupId(groupId.toString(), 40, 4, "k", "createdTime", "DESC");

        ArgumentCaptor<EntityGroupId> groupCap = ArgumentCaptor.forClass(EntityGroupId.class);
        ArgumentCaptor<PageLink> pageCap = ArgumentCaptor.forClass(PageLink.class);
        verify(restClient).getUsersByEntityGroupId(groupCap.capture(), pageCap.capture());
        assertThat(groupCap.getValue().getId()).isEqualTo(groupId);

        PageLink pl = pageCap.getValue();
        assertThat(pl.getPageSize()).isEqualTo(40);
        assertThat(pl.getPage()).isEqualTo(4);
        assertThat(pl.getTextSearch()).isEqualTo("k");

        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

}
