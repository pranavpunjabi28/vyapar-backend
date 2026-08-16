package com.bbu.vyaparbackend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AuthApi {
    private AuthApi() {
    }

    public record Register(@NotBlank @Email String email, @NotBlank @Size(min = 8, max = 100) String password,
                           @NotBlank @Size(max = 120) String displayName) {
        AuthCommands.Register toCommand() {
            return new AuthCommands.Register(email, password, displayName);
        }
    }

    public record Login(@NotBlank @Email String email, @NotBlank String password) {
        AuthCommands.Login toCommand() {
            return new AuthCommands.Login(email, password);
        }
    }

    public record Refresh(@NotBlank String refreshToken) {
    }

    public record PasswordChange(@NotBlank String currentPassword,
                                 @NotBlank @Size(min = 8, max = 100) String newPassword) {
    }

    public record Tokens(String accessToken, String refreshToken, Instant accessTokenExpiresAt, String userId,
                         String email, String displayName) {
        static Tokens from(AuthCommands.Tokens tokens) {
            return new Tokens(tokens.accessToken(), tokens.refreshToken(), tokens.accessTokenExpiresAt(), tokens.userId(),
                    tokens.email(), tokens.displayName());
        }
    }

    public record UserView(String id, String email, String displayName, boolean passwordChangeRequired) {
        static UserView from(User user) {
            return new UserView(user.getId(), user.getEmail(), user.getDisplayName(), user.isPasswordChangeRequired());
        }
    }
}
