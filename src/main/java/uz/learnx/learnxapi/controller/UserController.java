package uz.learnx.learnxapi.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uz.learnx.learnxapi.dto.UserDto;
import uz.learnx.learnxapi.model.ApiResponse;
import uz.learnx.learnxapi.security.CurrentUserId;
import uz.learnx.learnxapi.service.AccountService;
import uz.learnx.learnxapi.service.UserService;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AccountService accountService;

    // authenticated — the current learner, used by the UI to restore a session on reload
    @GetMapping
    public ApiResponse me(@CurrentUserId UUID userId) {
        return this.userService.me(userId);
    }

    // authenticated — only the fields sent are changed
    @PatchMapping
    public ApiResponse update(@CurrentUserId UUID userId, @Valid @RequestBody UserDto.UpdateProfileRequest request) {
        return this.userService.updateProfile(userId, request);
    }

    // authenticated
    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse resendVerification(@CurrentUserId UUID userId) {
        return this.accountService.resendVerification(userId);
    }
}
