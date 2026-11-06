package uz.learnx.learnxapi.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import uz.learnx.learnxapi.common.config.LearnxProperties;

/** Issues short-lived access JWTs and opaque refresh tokens. Only refresh token hashes are ever stored. */
@Service
class TokenService {

    private final JwtEncoder encoder;
    private final LearnxProperties.Jwt config;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    TokenService(JwtEncoder encoder, LearnxProperties properties, Clock clock) {
        this.encoder = encoder;
        this.config = properties.jwt();
        this.clock = clock;
    }

    String accessToken(UUID userId) {
        Instant now = Instant.now(clock);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("learnx")
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plus(config.accessTtl()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    String newRefreshToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    String hash(String refreshToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", e);
        }
    }

    Instant refreshExpiry() {
        return Instant.now(clock).plus(config.refreshTtl());
    }
}
