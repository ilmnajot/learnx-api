package uz.learnx.learnxapi.service;

public interface EmailService {
    void sendVerification(String to, String token);

    void sendPasswordReset(String to, String token);
}
