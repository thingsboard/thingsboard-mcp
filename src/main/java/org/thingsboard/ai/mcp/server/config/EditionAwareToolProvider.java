package org.thingsboard.ai.mcp.server.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
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
import org.thingsboard.ai.mcp.server.annotation.ToolGroup;
import org.thingsboard.ai.mcp.server.data.EditionChangedEvent;
import org.thingsboard.ai.mcp.server.data.RemoveToolsEvent;
import org.thingsboard.ai.mcp.server.data.ThingsBoardEdition;
import org.thingsboard.ai.mcp.server.tools.McpTools;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class EditionAwareToolProvider implements ToolCallbackProvider {

    private final MethodToolCallbackProvider delegate;
    private final ApplicationEventPublisher eventPublisher;
    private final Set<String> ceOnlyToolNames;
    private final Set<String> peOnlyToolNames;

    private volatile ThingsBoardEdition edition = ThingsBoardEdition.PE;

    public EditionAwareToolProvider(List<McpTools> tools, ApplicationEventPublisher eventPublisher,
                                    ToolGroupsProperties toolGroupsProperties) {
        List<McpTools> enabledTools = filterByGroups(tools, toolGroupsProperties);
        this.delegate = MethodToolCallbackProvider.builder().toolObjects(enabledTools.toArray()).build();
        this.peOnlyToolNames = scanEditionToolName(enabledTools, true);
        this.ceOnlyToolNames = scanEditionToolName(enabledTools, false);
        Set<String> disabledGroupToolNames = scanDisabledGroupToolNames(tools, toolGroupsProperties);
        this.eventPublisher = eventPublisher;

        if (!disabledGroupToolNames.isEmpty()) {
            Map<String, Long> groupCounts = tools.stream()
                    .filter(tool -> {
                        ToolGroup groupAnn = AnnotationUtils.findAnnotation(AopUtils.getTargetClass(tool), ToolGroup.class);
                        return groupAnn != null && !toolGroupsProperties.isGroupEnabled(groupAnn.value());
                    })
                    .collect(Collectors.groupingBy(
                            tool -> Objects.requireNonNull(AnnotationUtils.findAnnotation(AopUtils.getTargetClass(tool), ToolGroup.class)).value(),
                            Collectors.counting()
                    ));
            log.info("Disabled tool groups: {}. Total tools disabled: {}", groupCounts, disabledGroupToolNames.size());
        }
        log.info("Total enabled tools: {}", enabledTools.stream()
                .mapToLong(tool -> countToolMethods(AopUtils.getTargetClass(tool)))
                .sum());
    }

    private List<McpTools> filterByGroups(List<McpTools> tools, ToolGroupsProperties properties) {
        return tools.stream()
                .filter(tool -> {
                    Class<?> targetClass = AopUtils.getTargetClass(tool);
                    ToolGroup groupAnn = AnnotationUtils.findAnnotation(targetClass, ToolGroup.class);
                    if (groupAnn == null) {
                        return true;
                    }
                    boolean enabled = properties.isGroupEnabled(groupAnn.value());
                    if (!enabled) {
                        log.debug("Tool group '{}' is disabled, excluding: {}", groupAnn.value(), targetClass.getSimpleName());
                    }
                    return enabled;
                })
                .collect(Collectors.toList());
    }

    private Set<String> scanDisabledGroupToolNames(List<McpTools> allTools, ToolGroupsProperties properties) {
        Set<String> names = new HashSet<>();
        for (Object bean : allTools) {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            ToolGroup groupAnn = AnnotationUtils.findAnnotation(targetClass, ToolGroup.class);
            if (groupAnn != null && !properties.isGroupEnabled(groupAnn.value())) {
                for (var m : targetClass.getMethods()) {
                    Tool toolAnn = AnnotationUtils.findAnnotation(m, Tool.class);
                    if (toolAnn != null) {
                        String name = StringUtils.isNotBlank(toolAnn.name()) ? toolAnn.name() : m.getName();
                        names.add(name);
                    }
                }
            }
        }
        return Collections.unmodifiableSet(names);
    }

    private long countToolMethods(Class<?> targetClass) {
        long count = 0;
        for (var m : targetClass.getMethods()) {
            if (AnnotationUtils.findAnnotation(m, Tool.class) != null) {
                count++;
            }
        }
        return count;
    }

    @NonNull
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
                    String name = StringUtils.isNotBlank(toolAnn.name()) ? toolAnn.name() : m.getName();
                    names.add(name);
                } else {
                    CeOnly ceOnly = AnnotationUtils.findAnnotation(m, CeOnly.class);
                    if (ceOnly == null) {
                        continue;
                    }
                    String name = StringUtils.isNotBlank(toolAnn.name()) ? toolAnn.name() : m.getName();
                    names.add(name);
                }
            }
        }
        return Collections.unmodifiableSet(names);
    }

}
