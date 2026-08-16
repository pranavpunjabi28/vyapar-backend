package com.bbu.vyaparbackend.auth;

import com.bbu.vyaparbackend.shared.ApiEndpoints;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiEndpoints.Auth.ROOT)
public class AuthController {
    private final AuthService auth;
    private final CurrentUser currentUser;

    public AuthController(AuthService auth, CurrentUser currentUser) {
        this.auth = auth;
        this.currentUser = currentUser;
    }

    @PostMapping(ApiEndpoints.Auth.REGISTER)
    AuthApi.Tokens register(@Valid @RequestBody AuthApi.Register request) {
        return AuthApi.Tokens.from(auth.register(request.toCommand()));
    }

    @PostMapping(ApiEndpoints.Auth.LOGIN)
    AuthApi.Tokens login(@Valid @RequestBody AuthApi.Login request) {
        return AuthApi.Tokens.from(auth.login(request.toCommand()));
    }

    @PostMapping(ApiEndpoints.Auth.REFRESH)
    AuthApi.Tokens refresh(@Valid @RequestBody AuthApi.Refresh request) {
        return AuthApi.Tokens.from(auth.refresh(request.refreshToken()));
    }

    @PostMapping(ApiEndpoints.Auth.LOGOUT)
    void logout(@Valid @RequestBody AuthApi.Refresh request) {
        auth.logout(request.refreshToken());
    }

    @GetMapping(ApiEndpoints.Auth.ME)
    AuthApi.UserView me(Authentication principal) {
        return AuthApi.UserView.from(currentUser.require(principal));
    }

    @PostMapping(ApiEndpoints.Auth.PASSWORD)
    void password(Authentication principal, @Valid @RequestBody AuthApi.PasswordChange request) {
        auth.changePassword(currentUser.require(principal), request.currentPassword(), request.newPassword());
    }
}
