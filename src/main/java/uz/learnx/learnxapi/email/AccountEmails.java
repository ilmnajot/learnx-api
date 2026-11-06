package uz.learnx.learnxapi.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uz.learnx.learnxapi.common.config.LearnxProperties;

/**
 * Sends in the background, so a slow mail server can't delay requests or reveal through timing whether an account
 * exists. Tokens travel in the URL fragment, which browsers never send to servers. Messages deliberately leave out
 * the display name, because anyone can register with someone else's email and put any text in that field.
 */
@Service
public class AccountEmails {

    private static final Logger log = LoggerFactory.getLogger(AccountEmails.class);

    private final EmailSender sender;
    private final String publicUrl;

    AccountEmails(EmailSender sender, LearnxProperties properties) {
        this.sender = sender;
        this.publicUrl = properties.mail().publicUrl().replaceAll("/+$", "");
    }

    @Async
    public void sendVerification(String to, String token) {
        deliver(to, "LearnX: emailingizni tasdiqlang", """
                Salom!

                LearnX hisobingizni faollashtirish uchun emailingizni tasdiqlang:

                %s/verify-email#token=%s

                Havola 48 soat amal qiladi. Agar bu hisobni siz ochmagan boʻlsangiz, bu xatni eʼtiborsiz qoldiring.
                """.formatted(publicUrl, token));
    }

    @Async
    public void sendPasswordReset(String to, String token) {
        deliver(to, "LearnX: parolni tiklash", """
                Salom!

                LearnX parolingizni tiklash uchun quyidagi havolani oching:

                %s/reset-password#token=%s

                Havola 1 soat amal qiladi va faqat bir marta ishlaydi. Agar buni siz soʻramagan boʻlsangiz, \
                bu xatni eʼtiborsiz qoldiring: parolingiz oʻzgarmaydi.
                """.formatted(publicUrl, token));
    }

    private void deliver(String to, String subject, String body) {
        try {
            sender.send(to, subject, body);
        } catch (RuntimeException e) {
            // Logs the failure type only: addresses and links must stay out of production logs.
            log.error("Could not send account email: {}", e.getClass().getSimpleName());
        }
    }
}
