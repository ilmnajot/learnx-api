package uz.learnx.learnxapi.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import uz.learnx.learnxapi.integration.EmailSender;
import uz.learnx.learnxapi.integration.SmtpEmailSender;

@Slf4j
@Configuration
public class EmailConfig {

    @Bean
    public EmailSender emailSender(
            @Value("${spring.mail.host:}") String smtpHost,
            ObjectProvider<JavaMailSender> mail,
            LearnxProperties properties) {
        JavaMailSender sender = mail.getIfAvailable();
        if (!smtpHost.isBlank() && sender != null) {
            return new SmtpEmailSender(sender, properties.mail().from());
        }
        if (properties.mail().requireSmtp()) {
            throw new IllegalStateException("LEARNX_SMTP_HOST must be set: without it no email can be delivered");
        }
        log.warn("No mail server configured: emails, including their links, will be written to this log");
        return (to, subject, body) -> log.info("Development email to {}: {}\n{}", to, subject, body);
    }
}
