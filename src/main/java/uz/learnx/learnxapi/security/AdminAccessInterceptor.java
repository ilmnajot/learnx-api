package uz.learnx.learnxapi.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import uz.learnx.learnxapi.dto.User;
import uz.learnx.learnxapi.exception.ApiException;
import uz.learnx.learnxapi.mapper.UserMapper;

/**
 * Guards every admin route in one place, so a new endpoint can't forget the check. The role is read from the database
 * on each request instead of the token, so removing an admin takes effect immediately.
 */
@Component
@RequiredArgsConstructor
public class AdminAccessInterceptor implements HandlerInterceptor {

    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        User user = this.userMapper.findById(CurrentUserIdResolver.currentUserId()).orElse(null);
        if (user == null || !user.isAdmin() || user.bannedAt() != null) {
            throw ApiException.forbidden("forbidden", "Admin access required");
        }
        return true;
    }
}
