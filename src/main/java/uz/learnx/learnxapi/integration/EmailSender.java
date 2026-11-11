package uz.learnx.learnxapi.integration;

public interface EmailSender {

    void send(String to, String subject, String body);
}
