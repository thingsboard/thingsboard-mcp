package org.thingsboard.ai.mcp.server.service.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.ai.mcp.server.data.ThingsBoardEdition;
import org.thingsboard.ai.mcp.server.rest.RestClient;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.customer.CustomerTools;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CustomerToolsTest {

    @InjectMocks
    private CustomerTools tools;

    @Mock
    private RestClientService clientService;

    @Mock
    private RestClient restClient;

    @BeforeEach
    void setup() {
        when(clientService.getClient()).thenReturn(restClient);
    }

    @Test
    void testFindCustomerById() {
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

    @ParameterizedTest(name = "getCustomers page={1} size={0} text={2} sort={3} {4}")
    @CsvSource({
            "50,2,acme,title,DESC",
            "10,0,,createdTime,ASC"
    })
    void testFindCustomers(int pageSize, int page, String text, String sortProp, String dir) throws Exception {
        List<Customer> items = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Customer c = new Customer();
            c.setId(new CustomerId(UUID.randomUUID()));
            c.setTitle("C" + i);
            items.add(c);
        }
        PageData<Customer> pageData = new PageData<>(items, 2, items.size(), true);
        when(restClient.getCustomers(any(PageLink.class))).thenReturn(pageData);

        String result = tools.getCustomers(Integer.toString(pageSize), Integer.toString(page), text, sortProp, dir);

        ArgumentCaptor<PageLink> pageCap = ArgumentCaptor.forClass(PageLink.class);
        verify(restClient).getCustomers(pageCap.capture());
        PageLink pageLink = pageCap.getValue();
        assertThat(pageLink.getPageSize()).isEqualTo(pageSize);
        assertThat(pageLink.getPage()).isEqualTo(page);
        assertThat(pageLink.getTextSearch()).isEqualTo(text);
        if (sortProp != null) {
            assertThat(pageLink.getSortOrder()).isNotNull();
            assertThat(pageLink.getSortOrder().getProperty()).isEqualTo(sortProp);
            assertThat(pageLink.getSortOrder().getDirection()).isEqualTo(SortOrder.Direction.valueOf(dir));
        }
        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @Test
    void testFindTenantCustomer() {
        Customer customer = new Customer();
        customer.setId(new CustomerId(UUID.randomUUID()));
        customer.setTitle("Acme");
        when(restClient.getTenantCustomer("Acme")).thenReturn(Optional.of(customer));

        String result = tools.getTenantCustomer("Acme");

        verify(restClient).getTenantCustomer("Acme");
        assertThat(result).isEqualTo(JacksonUtil.toString(customer));
    }

    @ParameterizedTest(name = "getUserCustomers page={1} size={0} text={2} sort={3} {4}")
    @CsvSource({
            "25,1,user,createdTime,ASC",
            "5,0,,title,DESC"
    })
    void testFindUserCustomers(int pageSize, int page, String text, String sortProp, String dir) throws Exception {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.PE);
        when(clientService.getClient()).thenReturn(restClient);

        List<Customer> items = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Customer c = new Customer();
            c.setId(new CustomerId(UUID.randomUUID()));
            c.setTitle("U" + i);
            items.add(c);
        }
        PageData<Customer> pageData = new PageData<>(items, 1, items.size(), false);
        when(restClient.getUserCustomers(any(PageLink.class))).thenReturn(pageData);

        String result = tools.getUserCustomers(Integer.toString(pageSize), Integer.toString(page), text, sortProp, dir);

        ArgumentCaptor<PageLink> pageCap = ArgumentCaptor.forClass(PageLink.class);
        verify(restClient).getUserCustomers(pageCap.capture());
        PageLink pageLink = pageCap.getValue();
        assertThat(pageLink.getPageSize()).isEqualTo(pageSize);
        assertThat(pageLink.getPage()).isEqualTo(page);
        assertThat(pageLink.getTextSearch()).isEqualTo(text);
        if (sortProp != null) {
            assertThat(pageLink.getSortOrder()).isNotNull();
            assertThat(pageLink.getSortOrder().getProperty()).isEqualTo(sortProp);
            assertThat(pageLink.getSortOrder().getDirection()).isEqualTo(SortOrder.Direction.valueOf(dir));
        }
        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @ParameterizedTest(name = "customersByGroup page={1} size={0} text={2} sort={3} {4}")
    @CsvSource({
            "10,3,grp,email,DESC",
            "5,0,,createdTime,ASC"
    })
    void testFindCustomersByEntityGroupId(int pageSize, int page, String text, String sortProp, String dir) throws Exception {
        when(clientService.getEdition()).thenReturn(ThingsBoardEdition.PE);
        when(clientService.getClient()).thenReturn(restClient);

        UUID groupUuid = UUID.randomUUID();
        List<Customer> items = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Customer c = new Customer();
            c.setId(new CustomerId(UUID.randomUUID()));
            c.setTitle("G" + i);
            items.add(c);
        }
        PageData<Customer> pageData = new PageData<>(items, 1, items.size(), false);
        when(restClient.getCustomersByEntityGroupId(any(EntityGroupId.class), any(PageLink.class))).thenReturn(pageData);

        String result = tools.getCustomersByEntityGroupId(groupUuid.toString(), Integer.toString(pageSize), Integer.toString(page), text, sortProp, dir);

        ArgumentCaptor<EntityGroupId> egCap = ArgumentCaptor.forClass(EntityGroupId.class);
        ArgumentCaptor<PageLink> pageCap = ArgumentCaptor.forClass(PageLink.class);
        verify(restClient).getCustomersByEntityGroupId(egCap.capture(), pageCap.capture());

        assertThat(egCap.getValue().getId()).isEqualTo(groupUuid);
        PageLink pageLink = pageCap.getValue();
        assertThat(pageLink.getPageSize()).isEqualTo(pageSize);
        assertThat(pageLink.getPage()).isEqualTo(page);
        assertThat(pageLink.getTextSearch()).isEqualTo(text);
        if (sortProp != null) {
            assertThat(pageLink.getSortOrder()).isNotNull();
            assertThat(pageLink.getSortOrder().getProperty()).isEqualTo(sortProp);
            assertThat(pageLink.getSortOrder().getDirection()).isEqualTo(SortOrder.Direction.valueOf(dir));
        }
        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @Nested
    @DisplayName("saveCustomer variants")
    class SaveCustomerVariants {
        @Test
        void testSaveCustomer_withoutGroups() {
            Customer payload = new Customer();
            payload.setTitle("Acme");
            when(restClient.saveCustomer(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            String res = tools.saveCustomer(JacksonUtil.toString(payload), null, null);

            ArgumentCaptor<Customer> cap = ArgumentCaptor.forClass(Customer.class);
            verify(restClient).saveCustomer(cap.capture());
            assertThat(cap.getValue().getTitle()).isEqualTo("Acme");
            assertThat(res).isEqualTo(JacksonUtil.toString(payload));
        }

        @Test
        void testSaveCustomer_withSingleGroup() {
            Customer payload = new Customer();
            payload.setTitle("Beta");
            UUID group = UUID.randomUUID();

            when(restClient.saveCustomer(any(Customer.class), any(EntityGroupId.class), eq(null)))
                    .thenAnswer(inv -> inv.getArgument(0));

            String res = tools.saveCustomer(JacksonUtil.toString(payload), group.toString(), null);

            ArgumentCaptor<EntityGroupId> egCap = ArgumentCaptor.forClass(EntityGroupId.class);
            verify(restClient).saveCustomer(any(Customer.class), egCap.capture(), eq(null));
            assertThat(egCap.getValue().getId()).isEqualTo(group);
            assertThat(res).isEqualTo(JacksonUtil.toString(payload));
        }

        @Test
        void testSaveCustomer_withMultipleGroups() {
            Customer payload = new Customer();
            payload.setTitle("Gamma");
            String groupIds = UUID.randomUUID() + "," + UUID.randomUUID();

            when(restClient.saveCustomer(any(Customer.class), eq(null), eq(groupIds)))
                    .thenAnswer(inv -> inv.getArgument(0));

            String res = tools.saveCustomer(JacksonUtil.toString(payload), null, groupIds);

            verify(restClient).saveCustomer(any(Customer.class), eq(null), eq(groupIds));
            assertThat(res).isEqualTo(JacksonUtil.toString(payload));
        }

    }

    @Nested
    @DisplayName("deleteCustomer JSON contract")
    class DeleteCustomerContract {
        @Test
        void testDeleteCustomer_ok() {
            UUID id = UUID.randomUUID();
            String res = tools.deleteCustomer(id.toString());

            ArgumentCaptor<CustomerId> idCap = ArgumentCaptor.forClass(CustomerId.class);
            verify(restClient).deleteCustomer(idCap.capture());
            assertThat(idCap.getValue().getId()).isEqualTo(id);

            assertThat(res).contains("\"status\":\"OK\"");
            assertThat(res).contains(id.toString());
        }

        @Test
        void testDeleteCustomer_error() {
            UUID id = UUID.randomUUID();
            doThrow(new RuntimeException("boom")).when(restClient).deleteCustomer(any(CustomerId.class));

            String res = tools.deleteCustomer(id.toString());

            assertThat(res).contains("\"status\":\"ERROR\"");
            assertThat(res).contains(id.toString());
            assertThat(res).contains("boom");
        }

    }

}
