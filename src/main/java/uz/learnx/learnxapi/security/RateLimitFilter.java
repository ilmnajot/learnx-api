package uz.learnx.learnxapi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;
import uz.learnx.learnxapi.config.LearnxProperties;
import uz.learnx.learnxapi.dto.RateLimitDto;
import uz.learnx.learnxapi.model.ApiResponse;
import uz.learnx.learnxapi.service.RateLimitService;

/**
 * Limits sensitive and costly endpoints per IP address. Runs before security, so blocked requests never reach
 * password hashing. The IP comes from the servlet container, which only trusts proxy headers from internal proxies.
 */
@Slf4j
@Component
@Order(-200)
public class RateLimitFilter extends OncePerRequestFilter {

    private record Rule(String name, Pattern path, int limit, Duration window) {}

    private final RateLimitService rateLimitService;
    private final JsonMapper jsonMapper;
    private final List<Rule> rules;

    public RateLimitFilter(RateLimitService rateLimitService, JsonMapper jsonMapper, LearnxProperties properties) {
        this.rateLimitService = rateLimitService;
        this.jsonMapper = jsonMapper;
        LearnxProperties.RateLimit r = properties.rateLimit();
        Duration hour = Duration.ofHours(1);
        this.rules = List.of(
                new Rule("register", Pattern.compile("/api/v1/auth/register"), r.registerPerHour(), hour),
                new Rule("login", Pattern.compile("/api/v1/auth/login"), r.loginPerHour(), hour),
                new Rule("refresh", Pattern.compile("/api/v1/auth/refresh"), r.refreshPerHour(), hour),
                new Rule("password", Pattern.compile("/api/v1/auth/(forgot|reset)-password"), r.passwordPerHour(), hour),
                new Rule("verify", Pattern.compile("/api/v1/(auth/verify-email|me/resend-verification)"), r.verifyPerHour(), hour),
                new Rule("ai", Pattern.compile("/api/v1/(roleplay/[^/]+/turns|writing/improve)"), r.aiPerHour(), hour),
                new Rule("comment", Pattern.compile("/api/v1/comments/me"), r.commentPerHour(), hour));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) && !"PUT".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        for (Rule rule : this.rules) {
            if (rule.path().matcher(path).matches() && !allowed(rule, request, response)) {
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean allowed(Rule rule, HttpServletRequest request, HttpServletResponse response) throws IOException {
        RateLimitDto.Decision decision;
        try {
            decision = this.rateLimitService.check(rule.name() + ":" + request.getRemoteAddr(), rule.limit(), rule.window());
        } catch (RuntimeException e) {
            // A database problem will surface in the request itself. Limits must not become the reason for an outage.
            log.warn("Rate limit check failed: {}", e.getClass().getSimpleName());
            return true;
        }
        if (decision.allowed()) {
            return true;
        }
        ApiResponse body = ApiResponse.error(HttpStatus.TOO_MANY_REQUESTS, "rate_limited",
                "Too many requests. Try again later");
        body.getMeta().put("retryAfterSeconds", decision.retryAfterSeconds());
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(decision.retryAfterSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(this.jsonMapper.writeValueAsString(body));
        return false;
    }
}
