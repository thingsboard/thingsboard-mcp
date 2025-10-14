package org.thingsboard.ai.mcp.server.config;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.thingsboard.ai.mcp.server.annotation.CeOnly;
import org.thingsboard.ai.mcp.server.annotation.PeOnly;
import org.thingsboard.ai.mcp.server.data.EditionChangedEvent;
import org.thingsboard.ai.mcp.server.data.RemoveToolsEvent;
import org.thingsboard.ai.mcp.server.data.ThingsBoardEdition;
import org.thingsboard.ai.mcp.server.tools.McpTools;
import org.thingsboard.server.common.data.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class EditionAwareToolProvider implements ToolCallbackProvider {

    private final MethodToolCallbackProvider delegate;
    private final ApplicationEventPublisher eventPublisher;
    private final Set<String> ceOnlyToolNames;
    private final Set<String> peOnlyToolNames;

    private volatile ThingsBoardEdition edition = ThingsBoardEdition.PE;

    public EditionAwareToolProvider(List<McpTools> tools, ApplicationEventPublisher eventPublisher) {
        this.delegate = MethodToolCallbackProvider.builder().toolObjects(tools.toArray()).build();
        this.peOnlyToolNames = scanEditionToolName(tools, true);
        this.ceOnlyToolNames = scanEditionToolName(tools, false);
        this.eventPublisher = eventPublisher;
    }

    @NotNull
    @Override
    public ToolCallback[] getToolCallbacks() {
        return Arrays.stream(delegate.getToolCallbacks()).toArray(ToolCallback[]::new);
    }

    @EventListener
    public void onEditionChanged(EditionChangedEvent evt) {
        this.edition = evt.edition();
        if (edition == ThingsBoardEdition.CE) {
            eventPublisher.publishEvent(new RemoveToolsEvent(peOnlyToolNames.stream().toList()));
        } else if (edition == ThingsBoardEdition.PE) {
            eventPublisher.publishEvent(new RemoveToolsEvent(ceOnlyToolNames.stream().toList()));
        }
    }

    private static Set<String> scanEditionToolName(List<McpTools> tools, boolean isPe) {
        Set<String> names = new HashSet<>();
        for (Object bean : tools) {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            for (var m : targetClass.getMethods()) {
                Tool toolAnn = AnnotationUtils.findAnnotation(m, Tool.class);
                if (toolAnn == null) {
                    continue;
                }
                if (isPe) {
                    PeOnly peOnly = AnnotationUtils.findAnnotation(m, PeOnly.class);
                    if (peOnly == null) {
                        continue;
                    }
                    String name = StringUtils.hasText(toolAnn.name()) ? toolAnn.name() : m.getName();
                    names.add(name);
                } else {
                    CeOnly peOnly = AnnotationUtils.findAnnotation(m, CeOnly.class);
                    if (peOnly == null) {
                        continue;
                    }
                    String name = StringUtils.hasText(toolAnn.name()) ? toolAnn.name() : m.getName();
                    names.add(name);
                }
            }
        }
        return Collections.unmodifiableSet(names);
    }

}
