package org.thingsboard.ai.mcp.server.rest;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.data.EditionChangedEvent;
import org.thingsboard.ai.mcp.server.data.ThingsBoardEdition;
import org.thingsboard.server.common.data.StringUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestClientService {

    private final ApplicationEventPublisher events;

    @Value("${thingsboard.url:}")
    private String url;

    @Value("${thingsboard.api-key:}")
    private String apiKey;

    @Value("${thingsboard.username:}")
    private String username;

    @Value("${thingsboard.password:}")
    private String password;

    @Value("${thingsboard.login-interval-seconds:1800}")
    private int intervalSeconds;

    @Value("${thingsboard.connection.max-retries:3}")
    private int maxRetries;

    @Value("${thingsboard.connection.retry-delay-seconds:5}")
    private int retryDelaySeconds;

    @Getter
    private RestClient client;
    private ThingsBoardEdition edition;
    @Getter
    private String version;
    private ScheduledExecutorService scheduledExecutorService;
    private boolean usingCredentials;

    @PostConstruct
    public void init() {
        try {
            initClientWithRetry();
            if (usingCredentials) {
                scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
                scheduledExecutorService.scheduleAtFixedRate(() -> {
                    try {
                        client.login(username, password);
                    } catch (Exception ignored) {
                    }
                }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.error("Failed to init client service", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
        }
    }

    private void initClientWithRetry() {
        int attempt = 0;
        Exception lastException = null;
        while (attempt < maxRetries) {
            attempt++;
            try {
                initClient();
                return;
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries) {
                    log.warn("Connection attempt {} of {} failed, retrying in {} seconds...", attempt, maxRetries, retryDelaySeconds);
                    try {
                        TimeUnit.SECONDS.sleep(retryDelaySeconds);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during connection retry", ie);
                    }
                }
            }
        }
        log.error("Failed to connect after {} attempts", maxRetries);
        throw new RuntimeException("Failed to connect to ThingsBoard after " + maxRetries + " attempts", lastException);
    }

    private void initClient() {
        usingCredentials = false;
        try {
            if (StringUtils.isNotBlank(url)) {
                if (StringUtils.isNotBlank(apiKey)) {
                    client = RestClient.withApiKey(url, apiKey);
                } else if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
                    usingCredentials = true;
                    client = new RestClient(url);
                    client.login(username, password);
                }
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
        } catch (Exception e) {
            if (usingCredentials) {
                log.error("Failed to login to ThingsBoard {} using credentials for user '{}'", url, username, e);
            } else {
                log.error("Failed to login to ThingsBoard {} using API key", url, e);
            }
            throw new RuntimeException(e);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initEdition() {
        events.publishEvent(new EditionChangedEvent(edition));
    }

    public ThingsBoardEdition getEdition() {
        return edition != null ? edition : ThingsBoardEdition.CE;
    }

}
