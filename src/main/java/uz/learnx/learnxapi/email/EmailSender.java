package uz.learnx.learnxapi.email;

public interface EmailSender {

    void send(String to, String subject, String body);
}
