package uz.learnx.learnxapi;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import uz.learnx.learnxapi.integration.EmailSender;

/** Captures outgoing email, so tests can follow the links a real person would click. */
class RecordingEmails implements EmailSender {

    private static final Pattern TOKEN = Pattern.compile("#token=([A-Za-z0-9_-]+)");

    record Sent(String to, String subject, String body) {}

    private final List<Sent> sent = new CopyOnWriteArrayList<>();

    @Override
    public void send(String to, String subject, String body) {
        sent.add(new Sent(to, subject, body));
    }

    List<Sent> to(String address) {
        return sent.stream().filter(s -> s.to().equals(address)).toList();
    }

    /** Sending happens in the background, so this waits for it. */
    String awaitToken(String address, int expectedCount) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        while (to(address).size() < expectedCount) {
            if (Instant.now().isAfter(deadline)) {
                throw new AssertionError("Expected " + expectedCount + " emails to " + address);
            }
            Thread.sleep(20);
        }
        Matcher matcher = TOKEN.matcher(to(address).get(expectedCount - 1).body());
        if (!matcher.find()) {
            throw new AssertionError("Email has no link");
        }
        return matcher.group(1);
    }

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        RecordingEmails recordingEmails() {
            return new RecordingEmails();
        }
    }
}
