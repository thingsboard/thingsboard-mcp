package org.thingsboard.ai.mcp.server.tools.query;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.tools.McpTools;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class GuideTools implements McpTools {

    @Tool(description = "Call this to get the full documentation and JSON schema for creating 'keyFilters'. Use it if you are unsure about filter keys, value types, or predicates.")
    public String getKeyFiltersGuide() {
        return readResourceFile("src/main/resources/guide/key-filter.md");
    }

    @Tool(description = "Call this to get the documentation for creating and run complex queries over platform entities based on filters.")
    public String getEdqGuide() {
        return readResourceFile("src/main/resources/guide/edq-guide.md");
    }

    @Tool(description = "Call this to get the documentation for creating and run complex queries to search the count of platform entities based on filters.")
    public String getEdqCountGuide() {
        return readResourceFile("src/main/resources/guide/edq-count-guide.md");
    }

    private String readResourceFile(String path) {
        try {
            var resource = new org.springframework.core.io.ClassPathResource(path);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "Error: Could not read documentation file.";
        }
    }

}
