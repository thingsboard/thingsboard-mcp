package org.thingsboard.ai.mcp.server.mvc;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thingsboard.server.common.data.StringUtils;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.ai.mcp.server.stdio", havingValue = "false")
class MvcCorsConfig implements WebMvcConfigurer {

    @Value("${spring.ai.mcp.server.sse-endpoint:}")
    private String sseEndpoint;

    @Value("${spring.ai.mcp.server.sse-message-endpoint:}")
    private String sseMessageEndpoint;

    @Value("${spring.ai.mcp.cors.allowed-origins:*}")
    private String allowedOriginsProp;

    @Bean
    WebMvcConfigurer corsConfigurer() {
        final String sse = normalizePath(sseEndpoint);
        final String sseMsg = normalizePath(sseMessageEndpoint);
        final List<String> allowedOrigins = parseAllowedOrigins(allowedOriginsProp);
        final boolean allowAll = allowedOrigins.size() == 1 && "*".equals(allowedOrigins.get(0));

        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NotNull CorsRegistry registry) {
                if (allowAll) {
                    registry.addMapping(sse)
                            .allowedOriginPatterns("*")
                            .allowedMethods("GET", "OPTIONS")
                            .allowedHeaders("Content-Type", "Accept", "Origin", "Authorization")
                            .allowCredentials(false)
                            .maxAge(3600);
                    registry.addMapping(sseMsg)
                            .allowedOriginPatterns("*")
                            .allowedMethods("POST", "OPTIONS")
                            .allowedHeaders("Content-Type", "Accept", "Origin", "Authorization")
                            .allowCredentials(false)
                            .maxAge(3600);
                } else {
                    registry.addMapping(sse)
                            .allowedOrigins(allowedOrigins.toArray(String[]::new))
                            .allowedMethods("GET", "OPTIONS")
                            .allowedHeaders("Content-Type", "Accept", "Origin", "Authorization")
                            .allowCredentials(false)
                            .maxAge(3600);
                    registry.addMapping(sseMsg)
                            .allowedOrigins(allowedOrigins.toArray(String[]::new))
                            .allowedMethods("POST", "OPTIONS")
                            .allowedHeaders("Content-Type", "Accept", "Origin", "Authorization")
                            .allowCredentials(false)
                            .maxAge(3600);
                }
            }
        };
    }

    private static String normalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private static List<String> parseAllowedOrigins(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of("*");
        }
        String[] tokens = Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toArray(String[]::new);
        return tokens.length == 0 ? List.of("*") : List.of(tokens);
    }

}
