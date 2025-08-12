package org.thingsboard.ai.mcp.server.rest;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.data.ThingsBoardEdition;
import org.thingsboard.server.common.data.StringUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RestClientService {

    @Value("${thingsboard.url:}")
    private String url;

    @Value("${thingsboard.username:}")
    private String username;

    @Value("${thingsboard.password:}")
    private String password;

    @Value("${thingsboard.login-interval-seconds:1800}")
    private int intervalSeconds;

    @Getter
    private RestClient client;
    @Getter
    private ThingsBoardEdition edition;
    @Getter
    private String version;
    private ScheduledExecutorService scheduledExecutorService;

    @PostConstruct
    public void init() {
        initClient();
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                client.login(username, password);
            } catch (Exception ignored) {}
        }, 1, intervalSeconds, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy() {
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
        }
    }

    private void initClient() {
        if (StringUtils.isNotBlank(url)) {
            client = new RestClient(url);
            try {
                client.login(username, password);
            } catch (Exception e) {
                log.error("Failed to login to thingsboard {} using credentials [{} {}]", url, username, password, e);
                return;
            }
            JsonNode jsonNode = client.getSystemVersionInfo().orElse(null);
            if (jsonNode != null) {
                edition = ThingsBoardEdition.valueOf(jsonNode.get("type").asText());
                version = jsonNode.get("version").asText();
            } else {
                edition = ThingsBoardEdition.CE;
                version = "latest";
            }
            log.info("Connected to ThingsBoard [{} {}] at {}", edition.getName(), version, url);
        }
    }

}
