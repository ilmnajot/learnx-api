package uz.learnx.learnxapi.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uz.learnx.learnxapi.config.LearnxProperties;
import uz.learnx.learnxapi.integration.EmailSender;
import uz.learnx.learnxapi.service.EmailService;

/**
 * Sends in the background, so a slow mail server can't delay requests or reveal through timing whether an account
 * exists. Tokens travel in the URL fragment, which browsers never send to servers. Messages deliberately leave out
 * the display name, because anyone can register with someone else's email and put any text in that field.
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final EmailSender sender;
    private final String publicUrl;

    public EmailServiceImpl(EmailSender sender, LearnxProperties properties) {
        this.sender = sender;
        this.publicUrl = properties.mail().publicUrl().replaceAll("/+$", "");
    }

    @Async
    @Override
    public void sendVerification(String to, String token) {
        deliver(to, "LearnX: emailingizni tasdiqlang", """
                Salom!

                LearnX hisobingizni faollashtirish uchun emailingizni tasdiqlang:

                %s/verify-email#token=%s

                Havola 48 soat amal qiladi. Agar bu hisobni siz ochmagan boʻlsangiz, bu xatni eʼtiborsiz qoldiring.
                """.formatted(this.publicUrl, token));
    }

    @Async
    @Override
    public void sendPasswordReset(String to, String token) {
        deliver(to, "LearnX: parolni tiklash", """
                Salom!

                LearnX parolingizni tiklash uchun quyidagi havolani oching:

                %s/reset-password#token=%s

                Havola 1 soat amal qiladi va faqat bir marta ishlaydi. Agar buni siz soʻramagan boʻlsangiz, \
                bu xatni eʼtiborsiz qoldiring: parolingiz oʻzgarmaydi.
                """.formatted(this.publicUrl, token));
    }

    private void deliver(String to, String subject, String body) {
        try {
            this.sender.send(to, subject, body);
        } catch (RuntimeException e) {
            // Logs the failure type only: addresses and links must stay out of production logs.
            log.error("Could not send account email: {}", e.getClass().getSimpleName());
        }
    }
}
