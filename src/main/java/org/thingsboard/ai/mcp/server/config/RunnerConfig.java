package org.thingsboard.ai.mcp.server.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class RunnerConfig {

    @Bean
    ApplicationRunner sseModeChecker(Environment env) {
        return args -> {
            var appType = env.getProperty("spring.main.web-application-type", "servlet");
            var mcpType = env.getProperty("spring.ai.mcp.server.type", "SYNC");
            boolean ok = (appType.equalsIgnoreCase("servlet") && mcpType.equalsIgnoreCase("SYNC")) ||
                    (appType.equalsIgnoreCase("reactive") && mcpType.equalsIgnoreCase("ASYNC"));
            if (!ok) {
                throw new IllegalStateException("MCP stack mismatch: web-application-type=" + appType +
                        " requires spring.ai.mcp.server.type=" + (appType.equalsIgnoreCase("servlet") ? "SYNC" : "ASYNC"));
            }
        };
    }

    @Bean
    ApplicationRunner stdioModeChecker(Environment env) {
        return args -> {
            boolean stdio = env.getProperty("spring.ai.mcp.server.stdio", Boolean.class, false);
            if (stdio) {
                String webType = env.getProperty("spring.main.web-application-type", "servlet");
                String bannerMode = env.getProperty("spring.main.banner-mode", "console");
                if (!"none".equalsIgnoreCase(webType) || !"off".equalsIgnoreCase(bannerMode)) {
                    throw new IllegalStateException(
                            "STDIO mode requires spring.main.web-application-type=none and spring.main.banner-mode=off"
                    );
                }
            }
        };
    }

}
