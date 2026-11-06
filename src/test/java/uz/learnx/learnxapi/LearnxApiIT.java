package uz.learnx.learnxapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

/** Exercises the real HTTP API, security, MyBatis mappers and migrations against real PostgreSQL. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
            "learnx.ai.api-key=",
            "learnx.rate-limit.register-per-hour=100000",
            "learnx.rate-limit.login-per-hour=100000",
            "learnx.rate-limit.refresh-per-hour=100000",
            "learnx.rate-limit.password-per-hour=100000",
            "learnx.rate-limit.verify-per-hour=100000",
            "learnx.rate-limit.ai-per-hour=100000",
            "learnx.ai.global-daily-limit=100000"
        })
@Testcontainers
@Import(RecordingEmails.Config.class)
class LearnxApiIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final HttpClient http = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    int port;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    RecordingEmails emails;

    private record Reply(int status, JsonNode body, String setCookie) {}

    private record Account(String token, String cookie, String email) {}

    private Reply call(String method, String path, String token, Object body) throws Exception {
        return call(method, path, token, null, body);
    }

    private Reply call(String method, String path, String token, String cookie, Object body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path));
        request.method(method, body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
        request.header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        if (cookie != null) request.header("Cookie", cookie);
        HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        JsonNode parsed = response.body().isBlank() ? JSON.nullNode() : JSON.readTree(response.body());
        return new Reply(response.statusCode(), parsed,
                response.headers().firstValue("Set-Cookie").map(c -> c.split(";")[0]).orElse(null));
    }

    /** A learner who has confirmed their email, which is what most tests need. */
    private Account register() throws Exception {
        Account account = registerUnconfirmed();
        jdbc.update("UPDATE users SET email_verified_at = now() WHERE email = ?", account.email());
        return account;
    }

    private Account registerUnconfirmed() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        Reply reply = call("POST", "/auth/register", null,
                Map.of("email", email, "password", "test-password-1", "displayName", "Test User"));
        assertThat(reply.status()).isEqualTo(201);
        return new Account(reply.body().get("accessToken").asString(), reply.setCookie(), email);
    }

    @Test
    void newAccountsMustConfirmTheirEmailBeforeUsingAi() throws Exception {
        Account account = registerUnconfirmed();
        assertThat(call("GET", "/me", account.token(), null).body().get("emailVerified").asBoolean()).isFalse();

        Map<String, String> text = Map.of("text", "i have problem");
        Reply blocked = call("POST", "/writing/improve", account.token(), text);
        assertThat(blocked.status()).isEqualTo(403);
        assertThat(blocked.body().get("code").asString()).isEqualTo("email_not_verified");
        String session = call("POST", "/lessons/" + firstLessonId(account.token()) + "/roleplay", account.token(), null)
                .body().get("sessionId").asString();
        assertThat(call("POST", "/roleplay/" + session + "/turns", account.token(), text).status()).isEqualTo(403);

        String token = emails.awaitToken(account.email(), 1);
        assertThat(call("POST", "/auth/verify-email", null, Map.of("token", token)).status()).isEqualTo(204);

        assertThat(call("GET", "/me", account.token(), null).body().get("emailVerified").asBoolean()).isTrue();
        assertThat(call("POST", "/writing/improve", account.token(), text).status()).isEqualTo(200);
        assertThat(call("POST", "/auth/verify-email", null, Map.of("token", token)).status()).isEqualTo(400);
    }

    @Test
    void unconfirmedLearnersCanStillPractiseWithoutAi() throws Exception {
        Account account = registerUnconfirmed();
        String lesson = firstLessonId(account.token());
        assertThat(call("POST", "/lessons/" + lesson + "/complete", account.token(), null).status()).isEqualTo(200);
    }

    @Test
    void emailLinksNeverContainTheDisplayName() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        call("POST", "/auth/register", null, Map.of("email", email, "password", "test-password-1",
                "displayName", "Visit evil.example now"));
        emails.awaitToken(email, 1);
        assertThat(emails.to(email).getFirst().body()).doesNotContain("evil.example");
    }

    @Test
    void resendingRetiresTheOlderLink() throws Exception {
        Account account = registerUnconfirmed();
        String first = emails.awaitToken(account.email(), 1);
        assertThat(call("POST", "/me/resend-verification", account.token(), null).status()).isEqualTo(202);
        String second = emails.awaitToken(account.email(), 2);
        assertThat(call("POST", "/auth/verify-email", null, Map.of("token", first)).status()).isEqualTo(400);
        assertThat(call("POST", "/auth/verify-email", null, Map.of("token", second)).status()).isEqualTo(204);
    }

    @Test
    void expiredAndMadeUpLinksAreRejected() throws Exception {
        Account account = registerUnconfirmed();
        String token = emails.awaitToken(account.email(), 1);
        jdbc.update("UPDATE email_tokens SET expires_at = now() - interval '1 minute'");
        assertThat(call("POST", "/auth/verify-email", null, Map.of("token", token)).status()).isEqualTo(400);
        assertThat(call("POST", "/auth/verify-email", null, Map.of("token", "made-up")).status()).isEqualTo(400);
    }

    @Test
    void passwordResetChangesThePasswordAndSignsOutEverySession() throws Exception {
        Account account = register();
        assertThat(call("POST", "/auth/forgot-password", null, Map.of("email", account.email().toUpperCase())).status())
                .isEqualTo(202);
        // The sign-up confirmation arrives first, so the reset link is the second email.
        String token = emails.awaitToken(account.email(), 2);

        Map<String, String> tooShort = Map.of("token", token, "password", "short");
        assertThat(call("POST", "/auth/reset-password", null, tooShort).status()).isEqualTo(400);

        Map<String, String> reset = Map.of("token", token, "password", "brand-new-password");
        assertThat(call("POST", "/auth/reset-password", null, reset).status()).isEqualTo(204);

        assertThat(call("POST", "/auth/login", null, Map.of("email", account.email(), "password", "test-password-1")).status())
                .isEqualTo(401);
        assertThat(call("POST", "/auth/login", null, Map.of("email", account.email(), "password", "brand-new-password")).status())
                .isEqualTo(200);
        assertThat(call("POST", "/auth/refresh", null, account.cookie(), null).status()).isEqualTo(401);
        assertThat(call("POST", "/auth/reset-password", null, reset).status()).isEqualTo(400);
    }

    @Test
    void passwordResetUnlocksALockedAccountAndConfirmsTheEmail() throws Exception {
        Account account = registerUnconfirmed();
        for (int i = 0; i < 5; i++) {
            call("POST", "/auth/login", null, Map.of("email", account.email(), "password", "wrong-password"));
        }
        call("POST", "/auth/forgot-password", null, Map.of("email", account.email()));
        // The registration email arrives first, so this waits for the second one.
        String token = emails.awaitToken(account.email(), 2);
        call("POST", "/auth/reset-password", null, Map.of("token", token, "password", "brand-new-password"));
        Reply login = call("POST", "/auth/login", null, Map.of("email", account.email(), "password", "brand-new-password"));
        assertThat(login.status()).isEqualTo(200);
        assertThat(login.body().get("user").get("emailVerified").asBoolean()).isTrue();
    }

    @Test
    void forgotPasswordDoesNotRevealWhetherAnAccountExists() throws Exception {
        String unknown = "nobody-" + UUID.randomUUID() + "@example.com";
        Reply reply = call("POST", "/auth/forgot-password", null, Map.of("email", unknown));
        assertThat(reply.status()).isEqualTo(202);
        assertThat(reply.body().isNull()).isTrue();
        assertThat(emails.to(unknown)).isEmpty();
    }

    @Test
    void anInboxCannotBeFloodedWithResetEmails() throws Exception {
        Account account = register();
        for (int i = 0; i < 6; i++) {
            assertThat(call("POST", "/auth/forgot-password", null, Map.of("email", account.email())).status()).isEqualTo(202);
        }
        // One sign-up confirmation plus the hourly maximum of three reset emails.
        emails.awaitToken(account.email(), 4);
        Thread.sleep(300);
        assertThat(emails.to(account.email())).hasSize(4);
    }

    private String firstLessonId(String token) throws Exception {
        return call("GET", "/today", token, null).body().get("nextLesson").get("id").asString();
    }

    @Test
    void protectedEndpointsRequireAuthentication() throws Exception {
        assertThat(call("GET", "/today", null, null).status()).isEqualTo(401);
        assertThat(call("GET", "/today", "not-a-real-token", null).status()).isEqualTo(401);
    }

    @Test
    void registrationValidatesInputAndRejectsDuplicates() throws Exception {
        Reply invalid = call("POST", "/auth/register", null,
                Map.of("email", "not-an-email", "password", "short", "displayName", "X"));
        assertThat(invalid.status()).isEqualTo(400);
        assertThat(invalid.body().get("fields").has("email")).isTrue();

        Account account = register();
        Reply duplicate = call("POST", "/auth/register", null,
                Map.of("email", account.email().toUpperCase(), "password", "test-password-1", "displayName", "X"));
        assertThat(duplicate.status()).isEqualTo(409);
        assertThat(duplicate.body().get("code").asString()).isEqualTo("email_taken");
    }

    @Test
    void userResponsesNeverContainCredentials() throws Exception {
        Reply me = call("GET", "/me", register().token(), null);
        assertThat(me.status()).isEqualTo(200);
        assertThat(me.body().toString()).doesNotContain("password", "Hash", "failedLogin");
    }

    @Test
    void accountLocksAfterRepeatedFailedLogins() throws Exception {
        Account account = register();
        Map<String, String> wrong = Map.of("email", account.email(), "password", "wrong-password");
        for (int i = 0; i < 5; i++) {
            assertThat(call("POST", "/auth/login", null, wrong).status()).isEqualTo(401);
        }
        Reply locked = call("POST", "/auth/login", null, Map.of("email", account.email(), "password", "test-password-1"));
        assertThat(locked.status()).isEqualTo(429);
        assertThat(locked.body().get("code").asString()).isEqualTo("account_locked");
    }

    @Test
    void refreshRotatesTokensAndLateReuseRevokesEverySession() throws Exception {
        Account account = register();
        Reply rotated = call("POST", "/auth/refresh", null, account.cookie(), null);
        assertThat(rotated.status()).isEqualTo(200);
        assertThat(rotated.setCookie()).isNotEqualTo(account.cookie());

        jdbc.update("UPDATE refresh_tokens SET rotated_at = rotated_at - interval '1 minute' WHERE rotated_at IS NOT NULL");

        assertThat(call("POST", "/auth/refresh", null, account.cookie(), null).status()).isEqualTo(401);
        assertThat(call("POST", "/auth/refresh", null, rotated.setCookie(), null).status()).isEqualTo(401);
    }

    @Test
    void refreshCancelledByAReloadDoesNotSignTheUserOut() throws Exception {
        Account account = register();
        // The server rotated, but the browser never received the new cookie and retries with the old one.
        Reply lostResponse = call("POST", "/auth/refresh", null, account.cookie(), null);
        Reply retry = call("POST", "/auth/refresh", null, account.cookie(), null);
        assertThat(retry.status()).isEqualTo(200);
        assertThat(retry.setCookie()).isNotEqualTo(account.cookie());
        assertThat(call("POST", "/auth/refresh", null, retry.setCookie(), null).status()).isEqualTo(200);
        assertThat(call("POST", "/auth/refresh", null, lostResponse.setCookie(), null).status()).isEqualTo(200);
    }

    @Test
    void logoutRevokesTheRefreshToken() throws Exception {
        Account account = register();
        assertThat(call("POST", "/auth/logout", null, account.cookie(), null).status()).isEqualTo(204);
        assertThat(call("POST", "/auth/refresh", null, account.cookie(), null).status()).isEqualTo(401);
    }

    @Test
    void courseContentIsSeeded() throws Exception {
        Reply topics = call("GET", "/topics", register().token(), null);
        assertThat(topics.body().size()).isEqualTo(6);
        int lessons = 0;
        for (JsonNode topic : topics.body()) lessons += topic.get("lessons").size();
        assertThat(lessons).isEqualTo(30);
    }

    @Test
    void placementNeverExposesAnswersAndSetsALevel() throws Exception {
        Account account = register();
        Reply questions = call("GET", "/placement/questions", account.token(), null);
        assertThat(questions.body().size()).isEqualTo(12);
        assertThat(questions.body().toString()).doesNotContain("correct");

        List<Map<String, Object>> answers = new ArrayList<>();
        for (JsonNode q : questions.body()) {
            answers.add(Map.of("questionId", q.get("id").asString(), "selectedIndex", 0));
        }
        Reply result = call("POST", "/placement/submit", account.token(), Map.of("answers", answers));
        assertThat(result.status()).isEqualTo(200);
        assertThat(result.body().get("level").asString()).isIn("A1", "A2", "B1", "B2", "C1");
        assertThat(call("GET", "/me", account.token(), null).body().get("level").asString())
                .isEqualTo(result.body().get("level").asString());
    }

    @Test
    void completingALessonIsIdempotentStartsAStreakAndSchedulesReviews() throws Exception {
        Account account = register();
        String lessonId = firstLessonId(account.token());

        Reply lesson = call("GET", "/lessons/" + lessonId, account.token(), null);
        assertThat(lesson.body().get("phrases").size()).isEqualTo(3);
        assertThat(lesson.body().get("completed").asBoolean()).isFalse();

        assertThat(call("POST", "/lessons/" + lessonId + "/complete", account.token(), null)
                .body().get("firstCompletion").asBoolean()).isTrue();
        assertThat(call("POST", "/lessons/" + lessonId + "/complete", account.token(), null)
                .body().get("firstCompletion").asBoolean()).isFalse();

        JsonNode today = call("GET", "/today", account.token(), null).body();
        assertThat(today.get("streak").asInt()).isEqualTo(1);
        assertThat(today.get("completedLessons").asInt()).isEqualTo(1);
        assertThat(today.get("nextLesson").get("id").asString()).isNotEqualTo(lessonId);
        assertThat(today.get("dueReviews").asInt()).isZero();
        assertThat(call("GET", "/lessons/" + lessonId, account.token(), null).body().get("completed").asBoolean()).isTrue();
    }

    @Test
    void roleplayRunsThreeTurnsThenFinishes() throws Exception {
        Account account = register();
        String lessonId = firstLessonId(account.token());
        Reply started = call("POST", "/lessons/" + lessonId + "/roleplay", account.token(), null);
        assertThat(started.status()).isEqualTo(201);
        String session = started.body().get("sessionId").asString();

        Map<String, String> text = Map.of("text", "it is not finish, i have problem with database");
        Reply first = call("POST", "/roleplay/" + session + "/turns", account.token(), text);
        assertThat(first.status()).isEqualTo(200);
        assertThat(first.body().get("improved").asString()).contains("not finished", "a problem");
        assertThat(first.body().get("tips").size()).isEqualTo(2);
        assertThat(first.body().get("finished").asBoolean()).isFalse();

        call("POST", "/roleplay/" + session + "/turns", account.token(), text);
        Reply last = call("POST", "/roleplay/" + session + "/turns", account.token(), text);
        assertThat(last.body().get("finished").asBoolean()).isTrue();
        assertThat(call("POST", "/roleplay/" + session + "/turns", account.token(), text).status()).isEqualTo(409);
    }

    @Test
    void learnersCannotSeeEachOthersRoleplaySessions() throws Exception {
        Account owner = register();
        String session = call("POST", "/lessons/" + firstLessonId(owner.token()) + "/roleplay", owner.token(), null)
                .body().get("sessionId").asString();
        Reply other = call("POST", "/roleplay/" + session + "/turns", register().token(), Map.of("text", "hello"));
        assertThat(other.status()).isEqualTo(404);
    }

    @Test
    void aiRequestsAreValidated() throws Exception {
        String token = register().token();
        assertThat(call("POST", "/writing/improve", token, Map.of("text", "  ")).status()).isEqualTo(400);
        assertThat(call("POST", "/writing/improve", token, Map.of("text", "x".repeat(2001))).status()).isEqualTo(400);
        assertThat(call("POST", "/writing/improve", token, Map.of("text", "i have problem")).body()
                .get("improved").asString()).isEqualTo("I have a problem.");
    }

    @Test
    void gamesRejectImpossibleScoresAndCapPoints() throws Exception {
        String token = register().token();
        assertThat(call("GET", "/games/pool?size=8", token, null).body().size()).isEqualTo(8);
        assertThat(call("POST", "/games/results", token, Map.of("game", "PHRASE_MATCH", "correct", 9, "total", 8))
                .status()).isEqualTo(400);

        Map<String, Object> perfect = Map.of("game", "PHRASE_MATCH", "correct", 8, "total", 8);
        assertThat(call("POST", "/games/results", token, perfect).body().get("points").asInt()).isEqualTo(5);
        call("POST", "/games/results", token, perfect);
        call("POST", "/games/results", token, perfect);
        assertThat(call("POST", "/games/results", token, perfect).body().get("points").asInt()).isZero();
    }

    @Test
    void shadowingResultsCountAsPracticeWithCappedPoints() throws Exception {
        String token = register().token();
        Reply result = call("POST", "/games/results", token, Map.of("game", "SHADOWING", "correct", 3, "total", 5));
        assertThat(result.status()).isEqualTo(200);
        assertThat(result.body().get("points").asInt()).isEqualTo(3);
        assertThat(call("GET", "/today", token, null).body().get("streak").asInt()).isEqualTo(1);
    }

    @Test
    void squadFlowSharesProgressBetweenMembers() throws Exception {
        Account owner = register();
        Account member = register();
        assertThat(call("GET", "/squads/me", owner.token(), null).status()).isEqualTo(204);

        Reply created = call("POST", "/squads", owner.token(), Map.of("name", "Null Pointers"));
        assertThat(created.status()).isEqualTo(201);
        assertThat(call("POST", "/squads", owner.token(), Map.of("name", "Another")).status()).isEqualTo(409);

        String code = created.body().get("inviteCode").asString();
        Reply joined = call("POST", "/squads/join", member.token(), Map.of("inviteCode", code.toLowerCase()));
        assertThat(joined.status()).isEqualTo(200);
        assertThat(joined.body().get("members").size()).isEqualTo(2);
        assertThat(joined.body().get("requiredToday").asInt()).isEqualTo(2);

        call("POST", "/lessons/" + firstLessonId(member.token()) + "/complete", member.token(), null);
        JsonNode view = call("GET", "/squads/me", owner.token(), null).body();
        assertThat(view.get("incident").get("points").asInt()).isEqualTo(10);
        assertThat(view.get("practisedToday").asInt()).isEqualTo(1);
        assertThat(view.get("streakSafeToday").asBoolean()).isFalse();

        call("POST", "/lessons/" + firstLessonId(owner.token()) + "/complete", owner.token(), null);
        JsonNode both = call("GET", "/squads/me", member.token(), null).body();
        assertThat(both.get("streakSafeToday").asBoolean()).isTrue();
        assertThat(both.get("streak").asInt()).isEqualTo(1);
        assertThat(both.get("incident").get("points").asInt()).isEqualTo(20);

        assertThat(call("DELETE", "/squads/me", owner.token(), null).status()).isEqualTo(204);
        JsonNode remaining = call("GET", "/squads/me", member.token(), null).body();
        assertThat(remaining.get("owner").asBoolean()).isTrue();
        assertThat(remaining.get("members").size()).isEqualTo(1);
        assertThat(call("DELETE", "/squads/me", member.token(), null).status()).isEqualTo(204);
        assertThat(call("POST", "/squads/join", register().token(), Map.of("inviteCode", code)).status()).isEqualTo(404);
    }

    @Test
    void videosRequireAuthenticationAndAreSeeded() throws Exception {
        assertThat(call("GET", "/videos", null, null).status()).isEqualTo(401);
        Reply videos = call("GET", "/videos", register().token(), null);
        assertThat(videos.body().size()).isEqualTo(7);
        for (JsonNode video : videos.body()) {
            assertThat(video.get("youtubeId").asString()).hasSize(11);
            assertThat(video.get("durationSeconds").asInt()).isLessThanOrEqualTo(600);
        }
    }

    @Test
    void completingAVideoIsIdempotentAndFeedsReviewsStreakAndSquad() throws Exception {
        Account account = register();
        call("POST", "/squads", account.token(), Map.of("name", "Watchers"));
        String id = call("GET", "/videos", account.token(), null).body().get(0).get("id").asString();

        Reply detail = call("GET", "/videos/" + id, account.token(), null);
        assertThat(detail.body().get("phrases").size()).isEqualTo(3);
        assertThat(detail.body().get("video").has("completedAt")).isFalse();

        JsonNode first = call("POST", "/videos/" + id + "/complete", account.token(), null).body();
        assertThat(first.get("firstCompletion").asBoolean()).isTrue();
        assertThat(first.get("phrasesScheduled").asInt()).isEqualTo(3);
        JsonNode second = call("POST", "/videos/" + id + "/complete", account.token(), null).body();
        assertThat(second.get("firstCompletion").asBoolean()).isFalse();
        assertThat(second.get("phrasesScheduled").asInt()).isZero();

        assertThat(call("GET", "/today", account.token(), null).body().get("streak").asInt()).isEqualTo(1);
        assertThat(call("GET", "/squads/me", account.token(), null).body().get("incident").get("points").asInt())
                .isEqualTo(3);
        assertThat(call("GET", "/videos/" + id, account.token(), null).body().get("video").has("completedAt")).isTrue();
    }

    @Test
    void unknownVideoIsNotFound() throws Exception {
        String path = "/videos/" + UUID.randomUUID();
        assertThat(call("GET", path, register().token(), null).status()).isEqualTo(404);
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        HttpResponse<String> health = http.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(health.statusCode()).isEqualTo(200);
    }
}
