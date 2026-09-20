package com.bbu.vyaparbackend.auth;

import com.bbu.vyaparbackend.shared.ApiEndpoints;
import com.bbu.vyaparbackend.shared.ApiException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiEndpoints.Auth.ROOT)
public class AuthController {
    private final AuthService auth;
    private final CurrentUser currentUser;
    private final RefreshCookieService refreshCookies;

    public AuthController(AuthService auth, CurrentUser currentUser, RefreshCookieService refreshCookies) {
        this.auth = auth;
        this.currentUser = currentUser;
        this.refreshCookies = refreshCookies;
    }

    @PostMapping(ApiEndpoints.Auth.REGISTER)
    @SecurityRequirements
    ResponseEntity<AuthApi.Session> register(@Valid @RequestBody AuthApi.Register request) {
        return session(auth.register(request.toCommand()));
    }

    @PostMapping(ApiEndpoints.Auth.LOGIN)
    @SecurityRequirements
    ResponseEntity<AuthApi.Session> login(@Valid @RequestBody AuthApi.Login request) {
        return session(auth.login(request.toCommand()));
    }

    @PostMapping(ApiEndpoints.Auth.REFRESH)
    @SecurityRequirement(name = "refreshCookie")
    ResponseEntity<AuthApi.Session> refresh(HttpServletRequest request) {
        String refreshToken = refreshCookies.read(request)
                .orElseThrow(() -> ApiException.unauthorized("Session is invalid or expired"));
        return session(auth.refresh(refreshToken));
    }

    @PostMapping(ApiEndpoints.Auth.LOGOUT)
    @SecurityRequirement(name = "refreshCookie")
    ResponseEntity<Void> logout(HttpServletRequest request) {
        refreshCookies.read(request).ifPresent(auth::logout);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookies.clear().toString())
                .build();
    }

    @GetMapping(ApiEndpoints.Auth.ME)
    AuthApi.UserView me(Authentication principal) {
        return AuthApi.UserView.from(currentUser.require(principal));
    }

    @PostMapping(ApiEndpoints.Auth.PASSWORD)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void password(Authentication principal, @Valid @RequestBody AuthApi.PasswordChange request) {
        auth.changePassword(currentUser.require(principal), request.currentPassword(), request.newPassword());
    }

    private ResponseEntity<AuthApi.Session> session(AuthCommands.Tokens tokens) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookies.create(tokens.refreshToken()).toString())
                .body(AuthApi.Session.from(tokens));
    }
}
