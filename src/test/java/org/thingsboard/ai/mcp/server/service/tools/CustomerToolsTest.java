package org.thingsboard.ai.mcp.server.service.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.ai.mcp.server.data.ThingsBoardEdition;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.customer.CustomerTools;
import org.thingsboard.ai.mcp.server.util.JacksonUtil;
import org.thingsboard.client.ThingsboardClient;
import org.thingsboard.client.model.Customer;
import org.thingsboard.client.model.PageDataCustomer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
    private ThingsboardClient restClient;

    @BeforeEach
    void setup() {
        when(clientService.getClient()).thenReturn(restClient);
    }

    @Test
    void testFindCustomerById() {
        String id = UUID.randomUUID().toString();
        Customer customer = new Customer().title("Acme");
        when(restClient.getCustomerById(anyString())).thenReturn(customer);

        String result = tools.getCustomerById(id);

        verify(restClient).getCustomerById(eq(id));
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
            items.add(new Customer().title("C" + i));
        }
        PageDataCustomer pageData = new PageDataCustomer(2, (long) items.size(), true).data(items);
        when(restClient.getCustomers(anyInt(), anyInt(), any(), any(), any())).thenReturn(pageData);

        String result = tools.getCustomers(Integer.toString(pageSize), Integer.toString(page), text, sortProp, dir);

        verify(restClient).getCustomers(eq(pageSize), eq(page), eq(text), eq(sortProp), eq(dir));
        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @Test
    void testFindTenantCustomer() {
        Customer customer = new Customer().title("Acme");
        when(restClient.getTenantCustomer("Acme")).thenReturn(customer);

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
            items.add(new Customer().title("U" + i));
        }
        PageDataCustomer pageData = new PageDataCustomer(1, (long) items.size(), false).data(items);
        when(restClient.getUserCustomers(any(), any(), any(), any(), any())).thenReturn(pageData);

        String result = tools.getUserCustomers(Integer.toString(pageSize), Integer.toString(page), text, sortProp, dir);

        verify(restClient).getUserCustomers(any(), any(), any(), any(), any());
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

        String groupId = UUID.randomUUID().toString();
        List<Customer> items = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            items.add(new Customer().title("G" + i));
        }
        PageDataCustomer pageData = new PageDataCustomer(1, (long) items.size(), false).data(items);
        when(restClient.getCustomersByEntityGroupId(anyString(), any(), any(), any(), any(), any())).thenReturn(pageData);

        String result = tools.getCustomersByEntityGroupId(groupId, Integer.toString(pageSize), Integer.toString(page), text, sortProp, dir);

        verify(restClient).getCustomersByEntityGroupId(eq(groupId), any(), any(), any(), any(), any());
        assertThat(result).isEqualTo(JacksonUtil.toString(pageData));
    }

    @Nested
    @DisplayName("saveCustomer variants")
    class SaveCustomerVariants {
        @Test
        void testSaveCustomer_withoutGroups() {
            Customer payload = new Customer().title("Acme");
            when(restClient.saveCustomer(any(Customer.class), isNull(), isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(payload);

            String res = tools.saveCustomer(JacksonUtil.toString(payload), null, null);

            verify(restClient).saveCustomer(any(Customer.class), isNull(), isNull(), isNull(), isNull(), isNull());
            assertThat(res).isEqualTo(JacksonUtil.toString(payload));
        }

        @Test
        void testSaveCustomer_withSingleGroup() {
            Customer payload = new Customer().title("Beta");
            String group = UUID.randomUUID().toString();

            when(restClient.saveCustomer(any(Customer.class), eq(group), isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(payload);

            String res = tools.saveCustomer(JacksonUtil.toString(payload), group, null);

            verify(restClient).saveCustomer(any(Customer.class), eq(group), isNull(), isNull(), isNull(), isNull());
            assertThat(res).isEqualTo(JacksonUtil.toString(payload));
        }

        @Test
        void testSaveCustomer_withMultipleGroups() {
            Customer payload = new Customer().title("Gamma");
            String id1 = UUID.randomUUID().toString();
            String id2 = UUID.randomUUID().toString();
            String groupIds = id1 + "," + id2;

            when(restClient.saveCustomer(any(Customer.class), isNull(), eq(List.of(id1, id2)), isNull(), isNull(), isNull()))
                    .thenReturn(payload);

            String res = tools.saveCustomer(JacksonUtil.toString(payload), null, groupIds);

            verify(restClient).saveCustomer(any(Customer.class), isNull(), eq(List.of(id1, id2)), isNull(), isNull(), isNull());
            assertThat(res).isEqualTo(JacksonUtil.toString(payload));
        }

    }

    @Nested
    @DisplayName("deleteCustomer JSON contract")
    class DeleteCustomerContract {
        @Test
        void testDeleteCustomer_ok() {
            String id = UUID.randomUUID().toString();
            String res = tools.deleteCustomer(id);

            verify(restClient).deleteCustomer(eq(id));

            assertThat(res).contains("\"status\":\"OK\"");
            assertThat(res).contains(id);
        }

        @Test
        void testDeleteCustomer_error() {
            String id = UUID.randomUUID().toString();
            doThrow(new RuntimeException("boom")).when(restClient).deleteCustomer(anyString());

            String res = tools.deleteCustomer(id);

            assertThat(res).contains("\"status\":\"ERROR\"");
            assertThat(res).contains(id);
            assertThat(res).contains("boom");
        }

    }

}
