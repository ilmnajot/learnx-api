package uz.learnx.learnxapi.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import uz.learnx.learnxapi.common.config.LearnxProperties;

@Configuration
class EmailConfig {

    private static final Logger log = LoggerFactory.getLogger(EmailConfig.class);

    @Bean
    EmailSender emailSender(
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
