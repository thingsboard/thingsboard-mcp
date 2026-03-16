package org.thingsboard.ai.mcp.server.rest;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.data.EditionChangedEvent;
import org.thingsboard.ai.mcp.server.data.ThingsBoardEdition;
import org.thingsboard.client.ThingsboardClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.ai.mcp.server.util.JsonUtils.getMapper;

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

    @Value("${thingsboard.connection.max-retries:3}")
    private int maxRetries;

    @Value("${thingsboard.connection.retry-delay-seconds:5}")
    private int retryDelaySeconds;

    @Getter
    private ThingsboardClient client;
    private ThingsBoardEdition edition;
    @Getter
    private String version;

    @PostConstruct
    public void init() {
        try {
            initClientWithRetry();
        } catch (Exception e) {
            log.error("Failed to init client service", e);
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
        try {
            if (StringUtils.isNotBlank(url)) {
                if (StringUtils.isNotBlank(apiKey)) {
                    client = ThingsboardClient.builder().url(url).apiKey(apiKey).build();
                } else if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
                    client = ThingsboardClient.builder().url(url).credentials(username, password).build();
                }
            }
            detectEdition();
            log.info("Connected to ThingsBoard [{} {}] at {}", edition.getName(), version, url);
        } catch (Exception e) {
            if (StringUtils.isNotBlank(apiKey)) {
                log.error("Failed to login to ThingsBoard {} using API key", url, e);
            } else {
                log.error("Failed to login to ThingsBoard {} using credentials for user '{}'", url, username, e);
            }
            throw new RuntimeException(e);
        }
    }

    private void detectEdition() {
        try {
            String baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/system/info"))
                    .header("X-Authorization", "Bearer " + client.getToken())
                    .GET()
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode jsonNode = getMapper().readTree(response.body());
                edition = ThingsBoardEdition.fromType(jsonNode.get("type").asText());
                version = jsonNode.get("version").asText();
            } else {
                edition = ThingsBoardEdition.CE;
                version = "latest";
            }
        } catch (Exception e) {
            log.warn("Failed to detect ThingsBoard edition, defaulting to CE", e);
            edition = ThingsBoardEdition.CE;
            version = "latest";
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
