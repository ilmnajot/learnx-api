package uz.learnx.learnxapi.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uz.learnx.learnxapi.common.error.ApiException;
import uz.learnx.learnxapi.user.User;
import uz.learnx.learnxapi.user.UserMapper;

/**
 * Guards every admin route in one place, so a new endpoint can't forget the check. The role is read from the database
 * on each request instead of the token, so removing an admin takes effect immediately.
 */
@Configuration
class AdminAccess implements WebMvcConfigurer {

    private final UserMapper users;

    AdminAccess(UserMapper users) {
        this.users = users;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
                    throw ApiException.unauthorized("unauthenticated", "Authentication required");
                }
                User user = users.findById(UUID.fromString(jwt.getSubject())).orElse(null);
                if (user == null || !user.isAdmin() || user.bannedAt() != null) {
                    throw new ApiException(HttpStatus.FORBIDDEN, "forbidden", "Admin access required");
                }
                return true;
            }
        }).addPathPatterns("/api/v1/admin/**");
    }
}
