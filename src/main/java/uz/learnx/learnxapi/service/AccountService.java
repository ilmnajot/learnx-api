package uz.learnx.learnxapi.service;

import java.util.UUID;
import uz.learnx.learnxapi.dto.AuthDto;
import uz.learnx.learnxapi.dto.User;
import uz.learnx.learnxapi.model.ApiResponse;

/** Email confirmation and password reset. */
public interface AccountService {
    void sendVerification(User user);

    ApiResponse resendVerification(UUID userId);

    ApiResponse verifyEmail(AuthDto.TokenRequest request);

    ApiResponse forgotPassword(AuthDto.ForgotPasswordRequest request);

    ApiResponse resetPassword(AuthDto.ResetPasswordRequest request);
}
