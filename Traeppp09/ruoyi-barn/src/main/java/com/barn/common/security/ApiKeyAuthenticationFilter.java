package com.barn.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    private static final String PATH_PREFIX = "/barn/ai-query/";
    private static final String HEADER = "X-AI-API-Key";

    @Value("${security.ai-query.api-key:}")
    private String configuredApiKey;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (pathMatcher.match(PATH_PREFIX + "**", request.getRequestURI())
                && SecurityContextHolder.getContext().getAuthentication() == null
                && configuredApiKey != null && !configuredApiKey.isBlank()) {
            String supplied = request.getHeader(HEADER);
            if (supplied != null && MessageDigest.isEqual(
                    supplied.getBytes(StandardCharsets.UTF_8),
                    configuredApiKey.getBytes(StandardCharsets.UTF_8))) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        "ai-query", null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_AI_QUERY")));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }
}
