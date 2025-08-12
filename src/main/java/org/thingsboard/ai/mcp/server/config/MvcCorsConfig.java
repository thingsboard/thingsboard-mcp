package org.thingsboard.ai.mcp.server.config;


import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(WebMvcConfigurer.class)
class MvcCorsConfig implements WebMvcConfigurer {

    @Value("${spring.ai.mcp.server.sse-endpoint:}")
    private String sseEndpoint;

    @Value("${spring.ai.mcp.server.sse-message-endpoint:}")
    private String sseMessageEndpoint;

    @Bean
    WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NotNull CorsRegistry registry) {
                registry.addMapping(sseEndpoint).allowedOrigins("*").allowedMethods("GET");
                registry.addMapping(sseMessageEndpoint).allowedOrigins("*").allowedMethods("POST");
            }
        };
    }

}
