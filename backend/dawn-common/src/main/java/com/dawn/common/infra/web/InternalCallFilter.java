package com.dawn.common.infra.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Guards internal-only HTTP endpoints (permitAll in SecurityConfig, since internalRestClient
// carries no user JWT) with a shared secret header instead of leaving them open to the internet.
// Registered explicitly (not a @Component) via WebConfig, scoped to specific internal URL patterns only.
public class InternalCallFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Secret";

    private final String expectedSecret;

    public InternalCallFilter(String expectedSecret) {
        this.expectedSecret = expectedSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (expectedSecret != null && !expectedSecret.isBlank() && expectedSecret.equals(request.getHeader(HEADER))) {
            chain.doFilter(request, response);
            return;
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Internal endpoint");
    }
}
