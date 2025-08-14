package org.thingsboard.ai.mcp.server.tools.mvc;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "spring.ai.mcp.server.stdio", havingValue = "false")
public class OriginEnforcementFilter extends OncePerRequestFilter {

    @Value("${spring.ai.mcp.server.sse-endpoint:/sse}")
    private String sseEndpoint;

    @Value("${spring.ai.mcp.server.sse-message-endpoint:/mcp/message}")
    private String sseMessageEndpoint;

    @Value("${spring.ai.mcp.cors.allowed-origins:*}")
    private String allowedOriginsProp;

    private Set<String> allowed;
    private String sse;
    private String sseMsg;
    private boolean allowAll;

    @PostConstruct
    void init() {
        this.sse = sseEndpoint.startsWith("/") ? sseEndpoint : "/" + sseEndpoint;
        this.sseMsg = sseMessageEndpoint.startsWith("/") ? sseMessageEndpoint : "/" + sseMessageEndpoint;
        this.allowed = Arrays.stream(allowedOriginsProp.split(","))
                .map(String::trim).filter(StringUtils::hasText).collect(Collectors.toSet());
        if (allowed.isEmpty()) {
            allowed = Set.of("*");
        }
        this.allowAll = allowed.contains("*");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, @NotNull HttpServletResponse res, @NotNull FilterChain chain) throws ServletException, IOException {
        String uri = req.getRequestURI();
        if (!uri.equals(sse) && !uri.equals(sseMsg)) {
            chain.doFilter(req, res);
            return;
        }

        String origin = req.getHeader("Origin");

        if (origin == null) {
            boolean loopback = InetAddress.getByName(req.getRemoteAddr()).isLoopbackAddress();
            if (!loopback) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Missing Origin");
                return;
            }
        } else if (!allowAll && !allowed.contains(origin)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden Origin");
            return;
        }

        chain.doFilter(req, res);
    }

}
