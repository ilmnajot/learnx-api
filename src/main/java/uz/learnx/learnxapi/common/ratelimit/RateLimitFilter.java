package uz.learnx.learnxapi.common.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import uz.learnx.learnxapi.common.config.LearnxProperties;

/**
 * Limits sensitive and costly endpoints per IP address. Runs before security, so blocked requests never reach
 * password hashing. The IP comes from the servlet container, which only trusts proxy headers from internal proxies.
 */
@Component
@Order(-200)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private record Rule(String name, Pattern path, int limit, Duration window) {}

    private final RateLimiter limiter;
    private final List<Rule> rules;

    RateLimitFilter(RateLimiter limiter, LearnxProperties properties) {
        this.limiter = limiter;
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
        for (Rule rule : rules) {
            if (rule.path().matcher(path).matches() && !allowed(rule, request, response)) {
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean allowed(Rule rule, HttpServletRequest request, HttpServletResponse response) throws IOException {
        RateLimiter.Decision decision;
        try {
            decision = limiter.check(rule.name() + ":" + request.getRemoteAddr(), rule.limit(), rule.window());
        } catch (RuntimeException e) {
            // A database problem will surface in the request itself. Limits must not become the reason for an outage.
            log.warn("Rate limit check failed: {}", e.getClass().getSimpleName());
            return true;
        }
        if (decision.allowed()) {
            return true;
        }
        response.setStatus(429);
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(decision.retryAfterSeconds()));
        response.setContentType("application/problem+json");
        response.getWriter().write("""
                {"title":"Too Many Requests","status":429,"code":"rate_limited","detail":"Too many requests. Try again later"}""");
        return false;
    }
}
