package com.bbu.vyaparbackend.auth;

import com.bbu.vyaparbackend.shared.ApiEndpoints;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Component
public class RefreshCookieService {
    private static final String SAME_SITE = "Strict";

    private final String name;
    private final boolean secure;
    private final Duration lifetime;

    public RefreshCookieService(@Value("${app.security.refresh-cookie-name}") String name,
                                @Value("${app.security.refresh-cookie-secure}") boolean secure,
                                @Value("${app.security.refresh-token-days}") long refreshDays) {
        this.name = name;
        this.secure = secure;
        this.lifetime = Duration.ofDays(refreshDays);
    }

    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    public ResponseCookie create(String refreshToken) {
        return base(refreshToken).maxAge(lifetime).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(SAME_SITE)
                .path(ApiEndpoints.Auth.ROOT);
    }
}
