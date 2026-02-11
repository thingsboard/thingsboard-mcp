package org.thingsboard.ai.mcp.server.tools.query;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.annotation.ToolGroup;
import org.thingsboard.ai.mcp.server.tools.McpTools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@ToolGroup("edq")
public class GuideTools implements McpTools {

    @Tool(description = "Call this to get the full documentation and JSON schema for creating 'keyFilters'. Use it if you are unsure about filter keys, value types, or predicates.")
    public String getKeyFiltersGuide(
            @ToolParam(required = false, description = "Ignored. Pass any value or omit.") String unused) {
        return readResourceFile("guide/key-filter.md");
    }

    @Tool(description = "Call this to get the documentation for creating and run complex queries over platform entities based on filters.")
    public String getEdqGuide(
            @ToolParam(required = false, description = "Ignored. Pass any value or omit.") String unused) {
        return readResourceFile("guide/edq-guide.md");
    }

    @Tool(description = "Call this to get the documentation for creating and run complex queries to search the count of platform entities based on filters.")
    public String getEdqCountGuide(
            @ToolParam(required = false, description = "Ignored. Pass any value or omit.") String unused) {
        return readResourceFile("guide/edq-count-guide.md");
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
