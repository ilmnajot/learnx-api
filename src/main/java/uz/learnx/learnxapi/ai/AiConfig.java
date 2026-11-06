package uz.learnx.learnxapi.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;
import uz.learnx.learnxapi.common.config.LearnxProperties;

@Configuration
class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Bean
    AiTutor aiTutor(LearnxProperties properties, JsonMapper json) {
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
