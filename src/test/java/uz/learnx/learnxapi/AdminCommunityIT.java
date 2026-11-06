package uz.learnx.learnxapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "learnx.ai.api-key=",
            "learnx.admin.emails=owner@example.com",
            "learnx.public-site.cache-ttl=0s",
            "learnx.rate-limit.register-per-hour=100000",
            "learnx.rate-limit.login-per-hour=100000",
            "learnx.rate-limit.refresh-per-hour=100000",
            "learnx.rate-limit.verify-per-hour=100000",
            "learnx.rate-limit.comment-per-hour=100000"
        })
@Testcontainers
@Import(RecordingEmails.Config.class)
class AdminCommunityIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    /** A test card number, not a real one. */
    private static final String TEST_CARD = "1234567812345678";

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final HttpClient http = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    int port;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    RecordingEmails emails;

    private record Reply(int status, JsonNode body) {}

    private record Account(UUID id, String token, String email) {}

    private Reply call(String method, String path, String token, Object body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path));
        request.method(method, body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
        request.header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), response.body().isBlank() ? JSON.nullNode() : JSON.readTree(response.body()));
    }

    private Account register(String email, boolean verified) throws Exception {
        Reply reply = call("POST", "/auth/register", null,
                Map.of("email", email, "password", "test-password-1", "displayName", "Aziz Karimov"));
        assertThat(reply.status()).isEqualTo(201);
        if (verified) {
            jdbc.update("UPDATE users SET email_verified_at = now() WHERE email = ?", email);
        }
        return new Account(UUID.fromString(reply.body().get("user").get("id").asString()),
                reply.body().get("accessToken").asString(), email);
    }

    private Account learner() throws Exception {
        return register("user-" + UUID.randomUUID() + "@example.com", true);
    }

    private Account admin() throws Exception {
        Account account = learner();
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE id = ?", account.id());
        return account;
    }

    private Map<String, Object> settings(boolean donationEnabled, String card, int minLearners) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("donationEnabled", donationEnabled);
        values.put("donationCard", card);
        values.put("donationHolder", "T**** H*****");
        values.put("donationMessage", "Thank you");
        values.put("commentsEnabled", true);
        values.put("statsMinLearners", minLearners);
        return values;
    }

    @BeforeEach
    void resetSettings() {
        jdbc.update("DELETE FROM site_settings");
    }

    @Test
    void everyAdminRouteRejectsAnonymousAndOrdinaryUsers() throws Exception {
        String learner = learner().token();
        UUID someone = UUID.randomUUID();
        List<String[]> routes = List.of(
                new String[] {"GET", "/admin/stats"},
                new String[] {"GET", "/admin/users"},
                new String[] {"POST", "/admin/users/" + someone + "/ban"},
                new String[] {"POST", "/admin/users/" + someone + "/unban"},
                new String[] {"DELETE", "/admin/users/" + someone},
                new String[] {"GET", "/admin/comments"},
                new String[] {"GET", "/admin/videos"},
                new String[] {"GET", "/admin/settings"},
                new String[] {"GET", "/admin/audit"});
        for (String[] route : routes) {
            assertThat(call(route[0], route[1], null, null).status()).as(route[1]).isEqualTo(401);
            assertThat(call(route[0], route[1], learner, null).status()).as(route[1]).isEqualTo(403);
        }
    }

    @Test
    void aConfiguredEmailBecomesAdminOnlyAfterItIsConfirmed() throws Exception {
        Account owner = register("owner@example.com", false);
        assertThat(call("GET", "/me", owner.token(), null).body().get("admin").asBoolean()).isFalse();
        assertThat(call("GET", "/admin/stats", owner.token(), null).status()).isEqualTo(403);

        String token = emails.awaitToken("owner@example.com", 1);
        assertThat(call("POST", "/auth/verify-email", null, Map.of("token", token)).status()).isEqualTo(204);

        assertThat(call("GET", "/me", owner.token(), null).body().get("admin").asBoolean()).isTrue();
        assertThat(call("GET", "/admin/stats", owner.token(), null).status()).isEqualTo(200);
    }

    @Test
    void removingAnAdminTakesEffectImmediately() throws Exception {
        Account first = admin();
        Account second = admin();
        assertThat(call("GET", "/admin/stats", second.token(), null).status()).isEqualTo(200);
        assertThat(call("POST", "/admin/users/" + second.id() + "/role", first.token(), Map.of("admin", false)).status())
                .isEqualTo(204);
        assertThat(call("GET", "/admin/stats", second.token(), null).status()).isEqualTo(403);
    }

    @Test
    void adminsCannotLockThemselvesOut() throws Exception {
        Account admin = admin();
        String path = "/admin/users/" + admin.id();
        assertThat(call("POST", path + "/ban", admin.token(), null).status()).isEqualTo(409);
        assertThat(call("POST", path + "/role", admin.token(), Map.of("admin", false)).status()).isEqualTo(409);
        assertThat(call("DELETE", path, admin.token(), null).status()).isEqualTo(409);
    }

    @Test
    void banningStopsLoginAndRefreshAndUnbanRestoresLogin() throws Exception {
        Account admin = admin();
        Account target = learner();
        Map<String, String> login = Map.of("email", target.email(), "password", "test-password-1");

        assertThat(call("POST", "/admin/users/" + target.id() + "/ban", admin.token(), null).status()).isEqualTo(204);
        Reply blocked = call("POST", "/auth/login", null, login);
        assertThat(blocked.status()).isEqualTo(403);
        assertThat(blocked.body().get("code").asString()).isEqualTo("account_banned");
        assertThat(call("POST", "/writing/improve", target.token(), Map.of("text", "hello there")).status()).isEqualTo(403);

        assertThat(call("POST", "/admin/users/" + target.id() + "/unban", admin.token(), null).status()).isEqualTo(204);
        assertThat(call("POST", "/auth/login", null, login).status()).isEqualTo(200);
    }

    @Test
    void searchFindsUsersTreatsWildcardsLiterallyAndPaginates() throws Exception {
        Account admin = admin();
        String tag = UUID.randomUUID().toString().substring(0, 8);
        register("find-" + tag + "-1@example.com", true);
        register("find-" + tag + "-2@example.com", true);

        JsonNode page = call("GET", "/admin/users?q=find-" + tag + "&size=1", admin.token(), null).body();
        assertThat(page.get("total").asInt()).isEqualTo(2);
        assertThat(page.get("users").size()).isEqualTo(1);
        assertThat(page.toString()).doesNotContain("password", "Hash");

        assertThat(call("GET", "/admin/users?q=%25", admin.token(), null).body().get("total").asInt()).isZero();
    }

    @Test
    void deletingAUserRemovesThemAndTheirSquadOwnership() throws Exception {
        Account admin = admin();
        Account owner = learner();
        Account member = learner();
        String code = call("POST", "/squads", owner.token(), Map.of("name", "Testers")).body().get("inviteCode").asString();
        call("POST", "/squads/join", member.token(), Map.of("inviteCode", code));

        assertThat(call("DELETE", "/admin/users/" + owner.id(), admin.token(), null).status()).isEqualTo(204);

        assertThat(call("GET", "/squads/me", member.token(), null).body().get("owner").asBoolean()).isTrue();
        assertThat(call("POST", "/auth/login", null, Map.of("email", owner.email(), "password", "test-password-1")).status())
                .isEqualTo(401);
        assertThat(call("DELETE", "/admin/users/" + owner.id(), admin.token(), null).status()).isEqualTo(404);
    }

    @Test
    void reviewsStayHiddenUntilApprovedAndNeverExposeEmail() throws Exception {
        Account admin = admin();
        Account learner = learner();
        Map<String, Object> review = Map.of("body", "This helped me speak at standups.", "rating", 5);

        Reply saved = call("PUT", "/comments/me", learner.token(), review);
        assertThat(saved.status()).isEqualTo(200);
        assertThat(saved.body().get("status").asString()).isEqualTo("PENDING");
        assertThat(call("GET", "/public/site", null, null).body().toString()).doesNotContain("speak at standups");

        JsonNode queue = call("GET", "/admin/comments?status=PENDING", admin.token(), null).body();
        String id = queue.get(0).get("id").asString();
        assertThat(queue.get(0).get("email").asString()).isEqualTo(learner.email());
        assertThat(call("POST", "/admin/comments/" + id + "/status", admin.token(), Map.of("status", "APPROVED")).status())
                .isEqualTo(204);

        String page = call("GET", "/public/site", null, null).body().toString();
        assertThat(page).contains("speak at standups", "Aziz Karimov");
        assertThat(page).doesNotContain(learner.email(), learner.id().toString());

        call("PUT", "/comments/me", learner.token(), Map.of("body", "Edited after being approved here.", "rating", 4));
        assertThat(call("GET", "/public/site", null, null).body().toString()).doesNotContain("Edited after", "speak at standups");
    }

    @Test
    void reviewsAreValidatedAndNeedAConfirmedEmail() throws Exception {
        Account unconfirmed = register("user-" + UUID.randomUUID() + "@example.com", false);
        Map<String, Object> good = Map.of("body", "A perfectly good review.", "rating", 5);
        assertThat(call("PUT", "/comments/me", unconfirmed.token(), good).status()).isEqualTo(403);
        assertThat(call("PUT", "/comments/me", null, good).status()).isEqualTo(401);

        String token = learner().token();
        assertThat(call("PUT", "/comments/me", token, Map.of("body", "short", "rating", 5)).status()).isEqualTo(400);
        assertThat(call("PUT", "/comments/me", token, Map.of("body", "x".repeat(501), "rating", 5)).status()).isEqualTo(400);
        assertThat(call("PUT", "/comments/me", token, Map.of("body", "A perfectly good review.", "rating", 6)).status()).isEqualTo(400);
    }

    @Test
    void reviewsFromBannedUsersDisappearFromTheHomepage() throws Exception {
        Account admin = admin();
        Account learner = learner();
        call("PUT", "/comments/me", learner.token(), Map.of("body", "Soon to be banned review text.", "rating", 5));
        jdbc.update("UPDATE comments SET status = 'APPROVED'");
        assertThat(call("GET", "/public/site", null, null).body().toString()).contains("Soon to be banned");
        call("POST", "/admin/users/" + learner.id() + "/ban", admin.token(), null);
        assertThat(call("GET", "/public/site", null, null).body().toString()).doesNotContain("Soon to be banned");
    }

    @Test
    void statisticsStayHiddenUntilTheThresholdAndAreRealCounts() throws Exception {
        Account admin = admin();
        assertThat(call("GET", "/public/site", null, null).body().has("stats")).isFalse();

        call("PUT", "/admin/settings", admin.token(), settings(false, "", 0));
        jdbc.update("UPDATE users SET level = 'B1' WHERE id = ?", admin.id());
        JsonNode stats = call("GET", "/public/site", null, null).body().get("stats");
        Integer learners = jdbc.queryForObject("SELECT count(*) FROM users WHERE banned_at IS NULL AND email_verified_at IS NOT NULL", Integer.class);
        assertThat(stats.get("totals").get("learners").asInt()).isEqualTo(learners);

        register("user-" + UUID.randomUUID() + "@example.com", false);
        assertThat(call("GET", "/public/site", null, null).body().get("stats").get("totals").get("learners").asInt())
                .as("unconfirmed accounts must not inflate public numbers").isEqualTo(learners);
        assertThat(stats.get("levels").toString()).contains("B1");
    }

    @Test
    void donationAppearsOnlyWhenEnabledWithACard() throws Exception {
        Account admin = admin();
        assertThat(call("GET", "/public/site", null, null).body().has("donation")).isFalse();

        call("PUT", "/admin/settings", admin.token(), settings(true, "", 50));
        assertThat(call("GET", "/public/site", null, null).body().has("donation")).isFalse();

        assertThat(call("PUT", "/admin/settings", admin.token(), settings(true, "1234", 50)).status()).isEqualTo(400);
        assertThat(call("PUT", "/admin/settings", admin.token(), settings(true, "1234 5678 1234 5678", 50)).status())
                .isEqualTo(400);

        assertThat(call("PUT", "/admin/settings", admin.token(), settings(true, TEST_CARD, 50)).status()).isEqualTo(200);
        JsonNode donation = call("GET", "/public/site", null, null).body().get("donation");
        assertThat(donation.get("card").asString()).isEqualTo(TEST_CARD);
        assertThat(donation.get("holder").asString()).isEqualTo("T**** H*****");

        call("PUT", "/admin/settings", admin.token(), settings(false, TEST_CARD, 50));
        assertThat(call("GET", "/public/site", null, null).body().toString()).doesNotContain(TEST_CARD);
    }

    @Test
    void hiddenVideosDisappearForLearners() throws Exception {
        Account admin = admin();
        String token = learner().token();
        String id = call("GET", "/videos", token, null).body().get(0).get("id").asString();

        call("POST", "/admin/videos/" + id + "/visibility", admin.token(), Map.of("hidden", true));
        assertThat(call("GET", "/videos", token, null).body().toString()).doesNotContain(id);
        assertThat(call("GET", "/videos/" + id, token, null).status()).isEqualTo(404);

        call("POST", "/admin/videos/" + id + "/visibility", admin.token(), Map.of("hidden", false));
        assertThat(call("GET", "/videos/" + id, token, null).status()).isEqualTo(200);
    }

    @Test
    void adminActionsAreAuditedWithoutCardDetails() throws Exception {
        Account admin = admin();
        Account target = learner();
        call("POST", "/admin/users/" + target.id() + "/ban", admin.token(), null);
        call("PUT", "/admin/settings", admin.token(), settings(true, TEST_CARD, 50));

        String log = call("GET", "/admin/audit", admin.token(), null).body().toString();
        assertThat(log).contains("USER_BANNED", target.email(), admin.email(), "SETTINGS_UPDATED");
        assertThat(log).doesNotContain(TEST_CARD);
    }

    @Test
    void statsReportTotalsAndLevels() throws Exception {
        Account admin = admin();
        JsonNode stats = call("GET", "/admin/stats", admin.token(), null).body();
        assertThat(stats.get("totals").get("users").asInt()).isPositive();
        assertThat(stats.get("aiGlobalDailyLimit").asInt()).isPositive();
        assertThat(stats.get("signups").size()).isPositive();
    }
}
