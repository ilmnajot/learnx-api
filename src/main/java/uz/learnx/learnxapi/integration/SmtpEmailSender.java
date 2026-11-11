package uz.learnx.learnxapi.integration;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

/** Sends through any SMTP provider, so changing provider only means changing settings. */
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mail;
    private final String from;

    public SmtpEmailSender(JavaMailSender mail, String from) {
        this.mail = mail;
        this.from = from;
    }

    @Override
    public void send(String to, String subject, String body) {
        try {
            MimeMessage message = mail.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            mail.send(message);
        } catch (MessagingException e) {
            throw new MailSendException("Could not build message", e);
        }
    }
}
