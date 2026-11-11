package uz.learnx.learnxapi.service.impl;

import java.util.List;
import java.util.Locale;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import uz.learnx.learnxapi.config.LearnxProperties;
import uz.learnx.learnxapi.mapper.UserMapper;
import uz.learnx.learnxapi.service.AdminPromotionService;

@Service
public class AdminPromotionServiceImpl implements AdminPromotionService, ApplicationRunner {

    private final UserMapper userMapper;
    private final List<String> emails;

    public AdminPromotionServiceImpl(UserMapper userMapper, LearnxProperties properties) {
        this.userMapper = userMapper;
        this.emails = properties.admin().emails().stream()
                .map(email -> email.strip().toLowerCase(Locale.ROOT))
                .filter(email -> !email.isEmpty())
                .toList();
    }

    @Override
    public void run(ApplicationArguments args) {
        promoteConfigured();
    }

    @Override
    public void promoteConfigured() {
        if (!this.emails.isEmpty()) {
            this.userMapper.promoteToAdmin(this.emails);
        }
    }

    @Override
    public boolean isConfigured(String email) {
        return this.emails.contains(email);
    }
}
