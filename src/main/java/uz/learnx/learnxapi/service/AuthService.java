package uz.learnx.learnxapi.service;

import java.util.UUID;
import uz.learnx.learnxapi.dto.AuthDto;

public interface AuthService {
    AuthDto.Session register(AuthDto.RegisterRequest request);

    AuthDto.Session login(AuthDto.LoginRequest request);

    AuthDto.Session refresh(String refreshToken);

    void logout(String refreshToken);

    /** Signs a user out everywhere. Access tokens already issued stop working when they expire. */
    void revokeAllSessions(UUID userId);
}
