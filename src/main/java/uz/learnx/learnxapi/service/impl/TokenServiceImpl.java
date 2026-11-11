package uz.learnx.learnxapi.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import uz.learnx.learnxapi.config.LearnxProperties;
import uz.learnx.learnxapi.service.TokenService;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final JwtEncoder encoder;
    private final LearnxProperties properties;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @Override
    public String accessToken(UUID userId) {
        Instant now = Instant.now(this.clock);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("learnx")
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plus(this.properties.jwt().accessTtl()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return this.encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Override
    public String newRandomToken() {
        byte[] bytes = new byte[32];
        this.random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", e);
        }
    }

    @Override
    public Instant refreshExpiry() {
        return Instant.now(this.clock).plus(this.properties.jwt().refreshTtl());
    }
}
