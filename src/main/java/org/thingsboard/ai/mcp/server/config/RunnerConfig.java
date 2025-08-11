package org.thingsboard.ai.mcp.server.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class RunnerConfig {

    @Bean
    ApplicationRunner modeChecker(Environment env) {
        return args -> {
            var appType = env.getProperty("spring.main.web-application-type", "none");
            switch (appType) {
                case "none" -> {
                    String webType = env.getProperty("spring.main.web-application-type");
                    String bannerMode = env.getProperty("spring.main.banner-mode");
                    if (!"none".equalsIgnoreCase(webType) || !"off".equalsIgnoreCase(bannerMode)) {
                        throw new IllegalStateException(
                                "STDIO mode requires spring.main.web-application-type=none and spring.main.banner-mode=off"
                        );
                    }
                }
                case "servlet" -> {
                    var mcpType = env.getProperty("spring.ai.mcp.server.type");
                    if (!"SYNC".equalsIgnoreCase(mcpType)) {
                        throw new IllegalStateException("MCP stack mismatch: web-application-type=" + appType +
                                " requires spring.ai.mcp.server.type=SYNC");
                    }
                }
                case "reactive" -> {
                    var mcpType = env.getProperty("spring.ai.mcp.server.type");
                    if (!"ASYNC".equalsIgnoreCase(mcpType)) {
                        throw new IllegalStateException("MCP stack mismatch: web-application-type=" + appType +
                                " requires spring.ai.mcp.server.type=ASYNC");
                    }
                }
            }
        };
    }

}
