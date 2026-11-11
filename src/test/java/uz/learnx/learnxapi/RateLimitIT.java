package uz.learnx.learnxapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Runs with deliberately low limits. Requests arrive from loopback, which counts as a trusted internal proxy. */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "learnx.ai.api-key=",
            "learnx.rate-limit.register-per-hour=3",
            "learnx.rate-limit.login-per-hour=2",
            "learnx.rate-limit.password-per-hour=2",
            "learnx.ai.global-daily-limit=2"
        })
@Testcontainers
@Import(RecordingEmails.Config.class)
class RateLimitIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final HttpClient http = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    int port;

    @Autowired
    JdbcTemplate jdbc;

    private HttpResponse<String> post(String path, String forwardedFor, String body, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (forwardedFor != null) request.header("X-Forwarded-For", forwardedFor);
        if (token != null) request.header("Authorization", "Bearer " + token);
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String registerBody() {
        return """
                {"email":"%s@example.com","password":"test-password-1","displayName":"X"}""".formatted(UUID.randomUUID());
    }

    private static String ip() {
        var random = java.util.concurrent.ThreadLocalRandom.current();
        return "203.0." + random.nextInt(256) + "." + random.nextInt(1, 255);
    }

    @Test
    void blocksAnAddressOverTheLimitWithARetryHint() throws Exception {
        String address = ip();
        for (int i = 0; i < 3; i++) {
            assertThat(post("/auth/register", address, registerBody(), null).statusCode()).isEqualTo(201);
        }
        HttpResponse<String> blocked = post("/auth/register", address, registerBody(), null);
        assertThat(blocked.statusCode()).isEqualTo(429);
        assertThat(JSON.readTree(blocked.body()).get("meta").get("code").asString()).isEqualTo("rate_limited");
        assertThat(blocked.headers().firstValueAsLong("Retry-After").orElse(0)).isPositive();
    }

    @Test
    void otherAddressesAreNotAffected() throws Exception {
        String address = ip();
        for (int i = 0; i < 4; i++) post("/auth/register", address, registerBody(), null);
        assertThat(post("/auth/register", ip(), registerBody(), null).statusCode()).isEqualTo(201);
    }

    @Test
    void fakingAnAddressInTheHeaderDoesNotEscapeTheLimit() throws Exception {
        // A real proxy appends the true address on the right. Whatever the visitor sends ends up on the left.
        String realAddress = ip();
        int blocked = 0;
        for (int i = 0; i < 6; i++) {
            String spoofed = ip() + ", " + realAddress;
            if (post("/auth/register", spoofed, registerBody(), null).statusCode() == 429) blocked++;
        }
        assertThat(blocked).isEqualTo(3);
    }

    @Test
    void limitsAreSeparatePerEndpoint() throws Exception {
        String address = ip();
        String login = """
                {"email":"nobody@example.com","password":"wrong-password"}""";
        assertThat(post("/auth/login", address, login, null).statusCode()).isEqualTo(401);
        assertThat(post("/auth/login", address, login, null).statusCode()).isEqualTo(401);
        assertThat(post("/auth/login", address, login, null).statusCode()).isEqualTo(429);
        assertThat(post("/auth/register", address, registerBody(), null).statusCode()).isEqualTo(201);
    }

    @Test
    void passwordEndpointsAreLimited() throws Exception {
        String address = ip();
        String body = """
                {"email":"nobody@example.com"}""";
        post("/auth/forgot-password", address, body, null);
        post("/auth/forgot-password", address, body, null);
        assertThat(post("/auth/forgot-password", address, body, null).statusCode()).isEqualTo(429);
    }

    @Test
    void globalCeilingStopsAiForEveryoneOnceReached() throws Exception {
        jdbc.update("DELETE FROM ai_global_usage");
        String lastCode = null;
        int allowed = 0;
        for (int i = 0; i < 3; i++) {
            JsonNode session = JSON.readTree(post("/auth/register", ip(), registerBody(), null).body());
            jdbc.update("UPDATE users SET email_verified_at = now()");
            HttpResponse<String> result = post("/writing/improve", ip(), """
                    {"text":"i have problem"}""", session.get("accessToken").asString());
            if (result.statusCode() == 200) {
                allowed++;
            } else {
                lastCode = JSON.readTree(result.body()).get("meta").get("code").asString();
            }
        }
        assertThat(allowed).isEqualTo(2);
        assertThat(lastCode).isEqualTo("ai_capacity_reached");
    }
}
