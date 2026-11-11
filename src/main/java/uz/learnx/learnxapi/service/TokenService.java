package uz.learnx.learnxapi.service;

import java.time.Instant;
import java.util.UUID;

/** Issues short-lived access JWTs and opaque random tokens. Only token hashes are ever stored. */
public interface TokenService {
    String accessToken(UUID userId);

    String newRandomToken();

    String hash(String token);

    Instant refreshExpiry();
}
