package com.bbu.vyaparbackend.auth;

import com.bbu.vyaparbackend.shared.ApiException;
import com.bbu.vyaparbackend.shared.RequestLogContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;


@Component
public class CurrentUser {
    private final UserRepository users;

    public CurrentUser(UserRepository users) {
        this.users = users;
    }

    public User require(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt))
            throw ApiException.forbidden();
        String id = jwt.getSubject();
        if (id == null || id.isBlank()) throw ApiException.forbidden();
        RequestLogContext.user(id);
        return users.findById(id).filter(u -> !u.isArchived()).orElseThrow(ApiException::forbidden);
    }
}
