package ma.formation.incidentsapi.service;

import ma.formation.incidentsapi.exception.UnauthorizedException;
import ma.formation.incidentsapi.model.RefreshToken;
import ma.formation.incidentsapi.model.User;
import ma.formation.incidentsapi.repository.RefreshTokenRepository;
import ma.formation.incidentsapi.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final JwtService jwtService;
    private final long expirationMs;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository,
                               JwtService jwtService,
                               @Value("${app.jwt.refresh-expiration-ms}") long expirationMs) {
        this.repository = repository;
        this.jwtService = jwtService;
        this.expirationMs = expirationMs;
    }

    @Transactional
    public IssuedTokens issue(User user) {
        String rawToken = randomToken();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenHash(hash(rawToken));
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(Instant.now().plusMillis(expirationMs));
        repository.save(refreshToken);
        return new IssuedTokens(jwtService.generateToken(user), rawToken);
    }

    @Transactional
    public IssuedTokens rotate(String rawToken) {
        RefreshToken current = repository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Refresh token invalide"));
        Instant now = Instant.now();
        if (current.getRevokedAt() != null || !current.getExpiresAt().isAfter(now) || !current.getUser().isEnabled()) {
            throw new UnauthorizedException("Refresh token invalide");
        }
        current.setRevokedAt(now);
        repository.save(current);
        return issue(current.getUser());
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        repository.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
                repository.save(token);
            }
        });
    }

    private String randomToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponible", exception);
        }
    }

    public record IssuedTokens(String accessToken, String refreshToken) {
    }
}
