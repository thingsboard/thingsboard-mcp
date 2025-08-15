package org.thingsboard.ai.mcp.server.service.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.ai.mcp.server.rest.RestClient;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.ai.mcp.server.tools.settings.AdminTools;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.SystemInfo;
import org.thingsboard.server.common.data.UsageInfo;
import org.thingsboard.server.common.data.security.model.SecuritySettings;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AdminToolsTest {

    @InjectMocks
    private AdminTools tools;

    @Mock
    private RestClientService clientService;

    @Mock
    private RestClient restClient;

    @Test
    void testFindAdminSettings() {
        when(clientService.getClient()).thenReturn(restClient);

        AdminSettings admin = new AdminSettings();
        when(restClient.getAdminSettings("general")).thenReturn(Optional.of(admin));

        String result = tools.getAdminSettings("general");

        verify(restClient).getAdminSettings("general");
        assertThat(result).isEqualTo(JacksonUtil.toString(admin));
    }

    @Test
    void testFindSecuritySettings() {
        when(clientService.getClient()).thenReturn(restClient);

        SecuritySettings security = new SecuritySettings();
        when(restClient.getSecuritySettings()).thenReturn(Optional.of(security));

        String result = tools.getSecuritySettings();

        verify(restClient).getSecuritySettings();
        assertThat(result).isEqualTo(JacksonUtil.toString(security));
    }

    @Test
    void testFindSystemInfo() {
        when(clientService.getClient()).thenReturn(restClient);

        SystemInfo info = new SystemInfo();
        when(restClient.getSystemInfo()).thenReturn(info);

        String result = tools.getSystemInfo();

        verify(restClient).getSystemInfo();
        assertThat(result).isEqualTo(JacksonUtil.toString(info));
    }

    @Test
    void testFindUsageInfo() {
        when(clientService.getClient()).thenReturn(restClient);

        UsageInfo usage = new UsageInfo();
        when(restClient.getUsageInfo()).thenReturn(usage);

        String result = tools.getUsageInfo();

        verify(restClient).getUsageInfo();
        assertThat(result).isEqualTo(JacksonUtil.toString(usage));
    }

}
