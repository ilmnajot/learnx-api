package uz.learnx.learnxapi.admin;

import java.util.List;
import java.util.Locale;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import uz.learnx.learnxapi.common.config.LearnxProperties;

/**
 * Makes the addresses in LEARNX_ADMIN_EMAILS admins, but only once the email is confirmed. Otherwise anyone could
 * register an admin's address before they do and take over the panel.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    @Mapper
    public interface PromotionMapper {
        @Update("""
                <script>
                UPDATE users SET role = 'ADMIN', updated_at = now()
                WHERE role &lt;&gt; 'ADMIN' AND email_verified_at IS NOT NULL AND email IN
                <foreach collection="emails" item="email" open="(" separator="," close=")">#{email}</foreach>
                </script>
                """)
        int promote(@Param("emails") List<String> emails);
    }

    private final PromotionMapper promotions;
    private final List<String> emails;

    AdminBootstrap(PromotionMapper promotions, LearnxProperties properties) {
        this.promotions = promotions;
        this.emails = properties.admin().emails().stream()
                .map(email -> email.strip().toLowerCase(Locale.ROOT))
                .filter(email -> !email.isEmpty())
                .toList();
    }

    @Override
    public void run(ApplicationArguments args) {
        promoteConfigured();
    }

    /** Called at startup and after any email is confirmed. */
    public void promoteConfigured() {
        if (!emails.isEmpty()) {
            promotions.promote(emails);
        }
    }

    public boolean isConfigured(String email) {
        return emails.contains(email);
    }
}
