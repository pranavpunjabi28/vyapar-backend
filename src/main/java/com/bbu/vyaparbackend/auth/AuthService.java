package com.bbu.vyaparbackend.auth;

import com.bbu.vyaparbackend.shared.ApiException;
import com.bbu.vyaparbackend.shared.PrefixedIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
@Transactional
public class AuthService {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwords;
    private final JwtEncoder jwtEncoder;
    private final long accessMinutes;
    private final long refreshDays;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens, PasswordEncoder passwords,
                       JwtEncoder jwtEncoder, @Value("${app.security.access-token-minutes}") long accessMinutes,
                       @Value("${app.security.refresh-token-days}") long refreshDays) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwords = passwords;
        this.jwtEncoder = jwtEncoder;
        this.accessMinutes = accessMinutes;
        this.refreshDays = refreshDays;
    }

    public static String hash(String value) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public AuthCommands.Tokens register(AuthCommands.Register request) {
        String email = normalize(request.email());
        if (users.existsByEmailIgnoreCase(email)) throw ApiException.conflict("Email is already registered");
        User user = new User();
        user.setEmail(email);
        user.setDisplayName(request.displayName().trim());
        user.setPasswordHash(passwords.encode(request.password()));
        users.save(user);
        return issue(user);
    }

    public AuthCommands.Tokens login(AuthCommands.Login request) {
        User user = users.findByEmailIgnoreCase(normalize(request.email())).filter(u -> !u.isArchived())
                .orElseThrow(() -> new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED, "invalid_credentials", "Invalid email or password"));
        if (!passwords.matches(request.password(), user.getPasswordHash()))
            throw new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED, "invalid_credentials", "Invalid email or password");
        return issue(user);
    }

    public AuthCommands.Tokens refresh(String rawToken) {
        RefreshToken old = refreshTokens.findByTokenHash(hash(rawToken)).orElseThrow(() -> ApiException.invalid("Invalid refresh token"));
        if (old.getRevokedAt() != null || old.getExpiresAt().isBefore(Instant.now()))
            throw ApiException.invalid("Refresh token expired or revoked");
        old.setRevokedAt(Instant.now());
        return issue(old.getUser());
    }

    public void logout(String rawToken) {
        refreshTokens.findByTokenHash(hash(rawToken)).ifPresent(token -> token.setRevokedAt(Instant.now()));
    }

    public void changePassword(User user, String current, String replacement) {
        if (!passwords.matches(current, user.getPasswordHash()))
            throw ApiException.invalid("Current password is incorrect");
        user.setPasswordHash(passwords.encode(replacement));
        user.setPasswordChangeRequired(false);
    }

    private AuthCommands.Tokens issue(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessMinutes, ChronoUnit.MINUTES);
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("vyapar-backend").issuedAt(now).expiresAt(expiry)
                .subject(user.getId()).claim("email", user.getEmail()).build();
        String access = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        String rawRefresh = PrefixedIdGenerator.randomPart(40) + "." + PrefixedIdGenerator.randomPart(40);
        RefreshToken refresh = new RefreshToken();
        refresh.setUser(user);
        refresh.setTokenHash(hash(rawRefresh));
        refresh.setExpiresAt(now.plus(refreshDays, ChronoUnit.DAYS));
        refreshTokens.save(refresh);
        return new AuthCommands.Tokens(access, rawRefresh, expiry, user.getId(), user.getEmail(), user.getDisplayName());
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
