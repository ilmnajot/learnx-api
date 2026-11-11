package uz.learnx.learnxapi.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import uz.learnx.learnxapi.config.EmailConfig;
import uz.learnx.learnxapi.config.LearnxProperties;
import uz.learnx.learnxapi.service.impl.EmailServiceImpl;

/** Sends through a real local SMTP server, covering everything except a specific provider's settings. */
class SmtpEmailSenderTest {

    private GreenMail smtp;
    private JavaMailSenderImpl mail;

    @BeforeEach
    void start() {
        smtp = new GreenMail(ServerSetupTest.SMTP.dynamicPort());
        smtp.start();
        mail = new JavaMailSenderImpl();
        mail.setHost("127.0.0.1");
        mail.setPort(smtp.getSmtp().getPort());
    }

    @AfterEach
    void stop() {
        smtp.stop();
    }

    private static LearnxProperties properties(boolean requireSmtp) {
        return new LearnxProperties(
                new LearnxProperties.Jwt("x".repeat(32), Duration.ofMinutes(1), Duration.ofDays(1), false),
                new LearnxProperties.Cors(List.of("http://localhost")),
                new LearnxProperties.Ai("", "m", Duration.ofSeconds(1), 1, 1),
                new LearnxProperties.Mail("LearnX <no-reply@example.test>", "https://app.example.test/", requireSmtp),
                new LearnxProperties.RateLimit(1, 1, 1, 1, 1, 1, 1),
                new LearnxProperties.Admin(List.of()),
                new LearnxProperties.PublicSite(Duration.ofSeconds(1)));
    }

    @Test
    void deliversUzbekTextIntact() throws Exception {
        new SmtpEmailSender(mail, "LearnX <no-reply@example.test>").send("learner@example.test", "Parolni oʻzgartirish", "Yigʻilish: oʻqiy");

        MimeMessage[] received = smtp.getReceivedMessages();
        assertThat(received).hasSize(1);
        assertThat(received[0].getSubject()).isEqualTo("Parolni oʻzgartirish");
        assertThat(received[0].getContent().toString()).contains("Yigʻilish: oʻqiy");
        assertThat(received[0].getFrom()[0].toString()).contains("no-reply@example.test");
        assertThat(GreenMailUtil.getAddressList(received[0].getAllRecipients())).isEqualTo("learner@example.test");
    }

    @Test
    void linksPointAtThePublicSiteWithTheTokenInTheFragment() throws Exception {
        EmailServiceImpl emails = new EmailServiceImpl(new SmtpEmailSender(mail, "no-reply@example.test"), properties(true));
        emails.sendPasswordReset("learner@example.test", "abc123");

        String body = smtp.getReceivedMessages()[0].getContent().toString();
        assertThat(body).contains("https://app.example.test/reset-password#token=abc123");
    }

    @Test
    void aMailServerFailureDoesNotReachTheRequest() {
        smtp.stop();
        EmailServiceImpl emails = new EmailServiceImpl(new SmtpEmailSender(mail, "no-reply@example.test"), properties(true));
        emails.sendVerification("learner@example.test", "abc123");
    }

    @SuppressWarnings("unchecked")
    @Test
    void productionRefusesToStartWithoutAMailServer() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        assertThatThrownBy(() -> new EmailConfig().emailSender("", provider, properties(true)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LEARNX_SMTP_HOST");
    }

    @SuppressWarnings("unchecked")
    @Test
    void developmentFallsBackToTheLog() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        new EmailConfig().emailSender("", provider, properties(false)).send("a@example.test", "s", "b");
    }
}
