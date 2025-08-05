package org.thingsboard.ai.mcp.server.rest;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.StringUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RestClientServiceImpl implements RestClientService {

    private static final String url = System.getenv().getOrDefault("THINGSBOARD_URL", "");
    private static final String username = System.getenv().getOrDefault("THINGSBOARD_USERNAME", "");
    private static final String password = System.getenv().getOrDefault("THINGSBOARD_PASSWORD", "");

    @Getter
    private RestClient client;
    private ScheduledExecutorService scheduledExecutorService;

    @PostConstruct
    public void init() {
        initClient();
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                client.login(username, password);
            } catch (Exception ignored) {
            }
        }, 1, 1, TimeUnit.HOURS);
    }

    @PreDestroy
    public void destroy() {
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
        }
    }

    private void initClient() {
        if (!StringUtils.isEmpty(url)) {
            this.client = new RestClient(url);
            try {
                client.login(username, password);
            } catch (Exception e) {
                log.error("Failed to login to thingsboard.");
                throw e;
            }
            log.debug("Successfully logged to thingsboard.");
        }
    }
}

