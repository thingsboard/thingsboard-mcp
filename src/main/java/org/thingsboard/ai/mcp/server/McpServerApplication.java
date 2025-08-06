package org.thingsboard.ai.mcp.server;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.thingsboard.ai.mcp.server.tools.AdminTools;
import org.thingsboard.ai.mcp.server.tools.AlarmTools;
import org.thingsboard.ai.mcp.server.tools.AssetTools;
import org.thingsboard.ai.mcp.server.tools.CustomerTools;
import org.thingsboard.ai.mcp.server.tools.DeviceTools;
import org.thingsboard.ai.mcp.server.tools.RelationTools;
import org.thingsboard.ai.mcp.server.tools.TelemetryTools;
import org.thingsboard.ai.mcp.server.tools.UserTools;

@SpringBootApplication
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider toolCallbackProvider(DeviceTools deviceTools,
                                                     AssetTools assetTools,
                                                     CustomerTools customerTools,
                                                     AdminTools adminTools,
                                                     UserTools userTools,
                                                     AlarmTools alarmTools,
                                                     RelationTools relationTools,
                                                     TelemetryTools telemetryTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(deviceTools, assetTools, customerTools, adminTools, userTools, alarmTools, relationTools, telemetryTools)
                .build();
    }

}
