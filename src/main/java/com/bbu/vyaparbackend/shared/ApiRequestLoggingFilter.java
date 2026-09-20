package com.bbu.vyaparbackend.shared;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiRequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(ApiRequestLoggingFilter.class);
    private static final String REDACTED = "[REDACTED]";
    private static final String NOT_CAPTURED = "[NOT_CAPTURED]";
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "currentpassword", "newpassword", "pin", "token", "accesstoken", "refreshtoken",
            "invitationtoken", "secret", "apikey", "credential", "otp", "authorization", "cookie", "email", "phone", "address", "gstin",
            "fssai", "upiid", "reference", "paymentreference", "customername", "customerphone");

    private final ObjectMapper objectMapper;
    private final boolean includeBodies;
    private final int maxBodyBytes;

    public ApiRequestLoggingFilter(ObjectMapper objectMapper,
                                   @Value("${app.logging.http.include-bodies:true}") boolean includeBodies,
                                   @Value("${app.logging.http.max-body-bytes:8192}") int maxBodyBytes) {
        this.objectMapper = objectMapper;
        this.includeBodies = includeBodies;
        this.maxBodyBytes = maxBodyBytes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Map<String, String> previousContext = MDC.getCopyOfContextMap();
        String requestId = UUID.randomUUID().toString();
        MDC.put(RequestLogContext.REQUEST_ID, requestId);
        response.setHeader(RequestLogContext.REQUEST_ID_HEADER, requestId);

        boolean captureBodies = shouldCaptureBodies(request);
        ContentCachingRequestWrapper requestWrapper = captureBodies
                ? new ContentCachingRequestWrapper(request, maxBodyBytes) : null;
        ContentCachingResponseWrapper responseWrapper = captureBodies
                ? new ContentCachingResponseWrapper(response) : null;
        HttpServletRequest effectiveRequest = requestWrapper == null ? request : requestWrapper;
        HttpServletResponse effectiveResponse = responseWrapper == null ? response : responseWrapper;
        long startedAt = System.nanoTime();
        Exception failure = null;

        try {
            filterChain.doFilter(effectiveRequest, effectiveResponse);
        } catch (IOException | ServletException | RuntimeException exception) {
            failure = exception;
            throw exception;
        } finally {
            try {
                enrichAuthenticatedUser();
                long durationMillis = (System.nanoTime() - startedAt) / 1_000_000;
                if (failure != null) {
                    log.error(LogMessages.HTTP_FAILED, request.getMethod(), request.getRequestURI(),
                            effectiveResponse.getStatus(), durationMillis, failure);
                } else {
                    logCompleted(effectiveRequest, effectiveResponse, requestWrapper, responseWrapper, durationMillis);
                }
                if (responseWrapper != null) responseWrapper.copyBodyToResponse();
            } finally {
                restore(previousContext);
            }
        }
    }

    private void logCompleted(HttpServletRequest request, HttpServletResponse response,
                              ContentCachingRequestWrapper requestWrapper,
                              ContentCachingResponseWrapper responseWrapper, long durationMillis) {
        int status = response.getStatus();
        Object[] arguments = {request.getMethod(), request.getRequestURI(), status, durationMillis,
                queryParameterNames(request), requestBody(requestWrapper), responseBody(responseWrapper)};
        if (status >= 500) {
            log.error(LogMessages.HTTP_COMPLETED, arguments);
        } else if (status >= 400) {
            log.warn(LogMessages.HTTP_COMPLETED, arguments);
        } else {
            log.info(LogMessages.HTTP_COMPLETED, arguments);
        }
    }

    private boolean shouldCaptureBodies(HttpServletRequest request) {
        if (!includeBodies || !request.getRequestURI().startsWith(ApiEndpoints.API_V1)) return false;
        String path = request.getRequestURI().toLowerCase(Locale.ROOT);
        return !path.endsWith(".pdf") && !path.endsWith(".csv");
    }

    private String requestBody(ContentCachingRequestWrapper request) {
        if (request == null || !isJson(request.getContentType())) return NOT_CAPTURED;
        return sanitized(request.getContentAsByteArray());
    }

    private String responseBody(ContentCachingResponseWrapper response) {
        if (response == null || !isJson(response.getContentType())) return NOT_CAPTURED;
        return sanitized(response.getContentAsByteArray());
    }

    private String sanitized(byte[] content) {
        if (content.length == 0) return "[EMPTY]";
        int length = Math.min(content.length, maxBodyBytes);
        try {
            JsonNode root = objectMapper.readTree(new String(content, 0, length, StandardCharsets.UTF_8));
            redact(root, null);
            String result = objectMapper.writeValueAsString(root);
            return content.length > maxBodyBytes ? result + "[TRUNCATED]" : result;
        } catch (Exception ignored) {
            return "[UNREADABLE_JSON]";
        }
    }

    private void redact(JsonNode node, String parentField) {
        if (node instanceof ObjectNode object) {
            for (String field : new ArrayList<>(object.propertyNames())) {
                JsonNode value = object.get(field);
                if (sensitive(field) || personalName(parentField, field)) {
                    object.put(field, REDACTED);
                } else {
                    redact(value, field);
                }
            }
        } else if (node instanceof ArrayNode array) {
            array.forEach(value -> redact(value, parentField));
        }
    }

    private boolean sensitive(String field) {
        String normalized = field.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
        return SENSITIVE_FIELDS.contains(normalized) || normalized.contains("token")
                || normalized.contains("password") || normalized.contains("secret")
                || normalized.startsWith("pin") || normalized.endsWith("pin");
    }

    private boolean personalName(String parentField, String field) {
        if (parentField == null || !(field.equalsIgnoreCase("name") || field.equalsIgnoreCase("displayName"))) {
            return false;
        }
        String parent = parentField.toLowerCase(Locale.ROOT);
        return parent.contains("customer") || parent.contains("user") || parent.contains("staff");
    }

    private boolean isJson(String contentType) {
        if (contentType == null) return false;
        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            return MediaType.APPLICATION_JSON.includes(mediaType)
                    || MediaType.APPLICATION_PROBLEM_JSON.includes(mediaType)
                    || mediaType.getSubtype().endsWith("+json");
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private String queryParameterNames(HttpServletRequest request) {
        return request.getParameterMap().keySet().stream().sorted(Comparator.naturalOrder()).toList().toString();
    }

    private void enrichAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            RequestLogContext.user(jwt.getSubject());
        }
    }

    private void restore(Map<String, String> previousContext) {
        if (previousContext == null || previousContext.isEmpty()) {
            MDC.clear();
        } else {
            MDC.setContextMap(previousContext);
        }
    }
}
