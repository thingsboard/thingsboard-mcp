package org.thingsboard.ai.mcp.server;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.thingsboard.ai.mcp.server.config.EditionAwareToolProvider;
import org.thingsboard.ai.mcp.server.config.ToolGroupsProperties;

@SpringBootApplication
@EnableConfigurationProperties(ToolGroupsProperties.class)
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider toolCallbackProvider(EditionAwareToolProvider provider) {
        return provider;
    }

}
