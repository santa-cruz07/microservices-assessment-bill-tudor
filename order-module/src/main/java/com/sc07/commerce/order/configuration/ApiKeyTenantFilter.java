package com.sc07.commerce.order.configuration;

import io.opentelemetry.api.trace.Span;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Component
public class ApiKeyTenantFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-Api-Key";

    private static final Map<String, TenantPrincipal> API_KEYS = Map.of(
            "company-1-key",
            new TenantPrincipal(
                    UUID.fromString("11111111-1111-1111-1111-111111111111")
            ),
            "company-2-key",
            new TenantPrincipal(
                    UUID.fromString("22222222-2222-2222-2222-222222222222")
            )
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        TenantPrincipal principal = resolvePrincipal(request);

        if (principal == null) {
            SecurityContextHolder.clearContext();
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing or invalid API key"
            );
            return;
        }

        ApiKeyAuthToken authentication =
                new ApiKeyAuthToken(principal);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        Span.current().setAttribute("tenant.id", principal.tenantId().toString());

        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private TenantPrincipal resolvePrincipal(HttpServletRequest request) {
        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }

        return API_KEYS.get(apiKey);
    }
}