package org.thingsboard.ai.mcp.server.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to assign a tool class to a named group.
 * Users can enable/disable tool groups via configuration to reduce context size.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ToolGroup {

    /**
     * The name of the tool group (e.g., "query", "telemetry", "device", "alarm").
     */
    String value();

}
