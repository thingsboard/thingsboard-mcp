package org.thingsboard.ai.mcp.server.service.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.ai.mcp.server.rest.RestClient;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.customer.CustomerTools;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.ai.mcp.server.constant.ControllerConstants.PE_ONLY_AVAILABLE;

@ExtendWith(MockitoExtension.class)
public class CustomerToolsTest {

    @InjectMocks
    private CustomerTools tools;

    @Mock
    private RestClientService clientService;

    @Mock
    private RestClient restClient;

    @Test
    void testFindCustomerById() {
        when(clientService.getClient()).thenReturn(restClient);

        UUID id = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(new CustomerId(id));
        customer.setTitle("Acme");
        when(restClient.getCustomerById(any(CustomerId.class))).thenReturn(Optional.of(customer));

        String result = tools.getCustomerById(id.toString());

        ArgumentCaptor<CustomerId> idCap = ArgumentCaptor.forClass(CustomerId.class);
        verify(restClient).getCustomerById(idCap.capture());

        assertThat(idCap.getValue().getId()).isEqualTo(id);
        assertThat(result).isEqualTo(JacksonUtil.toString(customer));
    }

    @Test
    void testFindCustomers() throws Exception {
        when(clientService.getClient()).thenReturn(restClient);

        List<Customer> items = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Customer customer = new Customer();
            customer.setId(new CustomerId(UUID.randomUUID()));
            customer.setTitle("C" + i);
            items.add(customer);
        }
        PageData<Customer> page = new PageData<>(items, 2, items.size(), true);
        when(restClient.getCustomers(any(PageLink.class))).thenReturn(page);

        String result = tools.getCustomers(50, 2, "acme", "title", "DESC");

        ArgumentCaptor<PageLink> pageCap = ArgumentCaptor.forClass(PageLink.class);
        verify(restClient).getCustomers(pageCap.capture());

        PageLink pageLink = pageCap.getValue();
        assertThat(pageLink.getPageSize()).isEqualTo(50);
        assertThat(pageLink.getPage()).isEqualTo(2);
        assertThat(pageLink.getTextSearch()).isEqualTo("acme");
        assertThat(pageLink.getSortOrder().getDirection().name()).isEqualTo("DESC");

        assertThat(result).isEqualTo(JacksonUtil.toString(page));
    }

    @Test
    void testFindTenantCustomer() {
        when(clientService.getClient()).thenReturn(restClient);

        Customer customer = new Customer();
        customer.setId(new CustomerId(UUID.randomUUID()));
        customer.setTitle("Acme");
        when(restClient.getTenantCustomer("Acme")).thenReturn(Optional.of(customer));

        String result = tools.getTenantCustomer("Acme");

        verify(restClient).getTenantCustomer("Acme");
        assertThat(result).isEqualTo(JacksonUtil.toString(customer));
    }

    @Test
    void testFindUserCustomers_ceEdition() throws Exception {
        when(clientService.getEdition()).thenReturn(org.thingsboard.ai.mcp.server.data.ThingsBoardEdition.CE);

        String result = tools.getUserCustomers(25, 0, null, null, null);

        verify(clientService, never()).getClient();
        assertThat(result).isEqualTo(PE_ONLY_AVAILABLE);
    }

    @Test
    void testFindUserCustomers_peEdition() throws Exception {
        when(clientService.getEdition()).thenReturn(org.thingsboard.ai.mcp.server.data.ThingsBoardEdition.PE);
        when(clientService.getClient()).thenReturn(restClient);

        List<Customer> items = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Customer customer = new Customer();
            customer.setId(new CustomerId(UUID.randomUUID()));
            customer.setTitle("U" + i);
            items.add(customer);
        }
        PageData<Customer> page = new PageData<>(items, 1, items.size(), false);
        when(restClient.getUserCustomers(any(PageLink.class))).thenReturn(page);

        String result = tools.getUserCustomers(25, 1, "user", "createdTime", "ASC");

        ArgumentCaptor<PageLink> pageCap = ArgumentCaptor.forClass(PageLink.class);
        verify(restClient).getUserCustomers(pageCap.capture());

        PageLink pageLink = pageCap.getValue();
        assertThat(pageLink.getPageSize()).isEqualTo(25);
        assertThat(pageLink.getPage()).isEqualTo(1);
        assertThat(pageLink.getTextSearch()).isEqualTo("user");
        assertThat(pageLink.getSortOrder().getDirection().name()).isEqualTo("ASC");

        assertThat(result).isEqualTo(JacksonUtil.toString(page));
    }

    @Test
    void testFindCustomersByEntityGroupId_ceEdition() throws Exception {
        when(clientService.getEdition()).thenReturn(org.thingsboard.ai.mcp.server.data.ThingsBoardEdition.CE);

        String result = tools.getCustomersByEntityGroupId(UUID.randomUUID().toString(), 10, 0, null, null, null);

        verify(clientService, never()).getClient();
        assertThat(result).isEqualTo(PE_ONLY_AVAILABLE);
    }

    @Test
    void testFindCustomersByEntityGroupId_peEdition() throws Exception {
        when(clientService.getEdition()).thenReturn(org.thingsboard.ai.mcp.server.data.ThingsBoardEdition.PE);
        when(clientService.getClient()).thenReturn(restClient);

        UUID groupUuid = UUID.randomUUID();
        List<Customer> items = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Customer customer = new Customer();
            customer.setId(new CustomerId(UUID.randomUUID()));
            customer.setTitle("G" + i);
            items.add(customer);
        }
        PageData<Customer> page = new PageData<>(items, 1, items.size(), false);
        when(restClient.getCustomersByEntityGroupId(any(EntityGroupId.class), any(PageLink.class))).thenReturn(page);

        String result = tools.getCustomersByEntityGroupId(groupUuid.toString(), 10, 3, "grp", "email", "DESC");

        ArgumentCaptor<EntityGroupId> egCap = ArgumentCaptor.forClass(EntityGroupId.class);
        ArgumentCaptor<PageLink> pageCap = ArgumentCaptor.forClass(PageLink.class);
        verify(restClient).getCustomersByEntityGroupId(egCap.capture(), pageCap.capture());

        assertThat(egCap.getValue().getId()).isEqualTo(groupUuid);

        PageLink pageLink = pageCap.getValue();
        assertThat(pageLink.getPageSize()).isEqualTo(10);
        assertThat(pageLink.getPage()).isEqualTo(3);
        assertThat(pageLink.getTextSearch()).isEqualTo("grp");
        assertThat(pageLink.getSortOrder().getDirection().name()).isEqualTo("DESC");

        assertThat(result).isEqualTo(JacksonUtil.toString(page));
    }

}
