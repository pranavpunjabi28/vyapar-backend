package com.bbu.vyaparbackend.auth;

public final class AuthCommands {
    private AuthCommands() {
    }

    public record Register(String email, String password, String displayName) {
    }

    public record Login(String email, String password) {
    }

    public record Tokens(String accessToken, String refreshToken, java.time.Instant accessTokenExpiresAt,
                         String userId, String email, String displayName, boolean passwordChangeRequired) {
    }
}
