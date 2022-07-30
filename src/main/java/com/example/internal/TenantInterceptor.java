package com.example.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

public final class TenantInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantInterceptor.class);

    private static final String X_TENANT_ID = "X-Tenant-Id";

    private final ObjectMapper objectMapper;
    private final Tenants tenants;

    public TenantInterceptor(ObjectMapper objectMapper, Tenants tenants) {
        this.objectMapper = objectMapper;
        this.tenants = tenants;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        final var xTenantId = request.getHeader(X_TENANT_ID);
        if (xTenantId == null) {
            return respondMissingTenant(response);
        }

        if (!tenants.getNames().contains(xTenantId)) {
            return respondUnknownTenant(xTenantId, response);
        }

        return selectTenantAndContinue(xTenantId);
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) {
        Tenant.unset();
        LOGGER.debug("Removed tenant assigned previously before sending response to client");
    }

    private boolean respondMissingTenant(HttpServletResponse response) throws IOException {
        final var problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Missing database tenant");
        problemDetail.setDetail("Header X-Tenant-Id was not present in the request");

        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().print(objectMapper.writeValueAsString(problemDetail));
        return false;
    }

    private boolean respondUnknownTenant(String xTenantId, HttpServletResponse response) throws IOException {
        final var problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Unknown database tenant");
        problemDetail.setDetail("Value of header X-Tenant-Id does not match a known database tenant");
        problemDetail.setProperty("tenantId", xTenantId);

        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().print(objectMapper.writeValueAsString(problemDetail));
        return false;
    }

    private boolean selectTenantAndContinue(String xTenantId) {
        Tenant.set(xTenantId);
        LOGGER.debug("Handling request for tenant {}", Tenant.get());
        return true;
    }
}
