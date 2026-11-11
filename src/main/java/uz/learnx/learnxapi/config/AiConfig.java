package uz.learnx.learnxapi.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;
import uz.learnx.learnxapi.integration.AiTutor;
import uz.learnx.learnxapi.integration.ClaudeTutor;
import uz.learnx.learnxapi.integration.FallbackTutor;
import uz.learnx.learnxapi.integration.RuleBasedTutor;

@Slf4j
@Configuration
public class AiConfig {

    @Bean
    public AiTutor aiTutor(LearnxProperties properties, JsonMapper json) {
        RuleBasedTutor ruleBased = new RuleBasedTutor();
        LearnxProperties.Ai ai = properties.ai();
        if (!ai.hasApiKey()) {
            log.warn("ANTHROPIC_API_KEY is not set: using the rule-based tutor");
            return ruleBased;
        }
        AnthropicClient client = AnthropicOkHttpClient.builder()
                .apiKey(ai.apiKey())
                .timeout(ai.timeout())
                .maxRetries(1)
                .build();
        return new FallbackTutor(new ClaudeTutor(client, ai.model(), json), ruleBased);
    }
}
