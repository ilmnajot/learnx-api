package uz.learnx.learnxapi.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A deterministic coach that needs no network: it fixes common mistakes from a fixed list. It keeps the
 * product working without an API key and during provider outages. It cannot understand meaning.
 */
public class RuleBasedTutor implements AiTutor {

    public static final String PROVIDER = "rule_based";

    private record Rule(Pattern pattern, String replacement, String tip) {
        static Rule of(String regex, String replacement, String tip) {
            return new Rule(Pattern.compile(regex, Pattern.CASE_INSENSITIVE), replacement, tip);
        }
    }

    private static final List<Rule> RULES = List.of(
            Rule.of("\\b(is|are|am|was|were|not) finish\\b", "$1 finished",
                    "\"finish\" emas, \"finished\" deb ayting: holatni bildirganda feʼlning uchinchi shakli kerak."),
            Rule.of("\\b(didn't|did not) (finished|fixed|deployed|tested|merged)\\b", "$1 #BASE#",
                    "\"didn't\" dan keyin feʼl oʻzgarmaydi: \"didn't finish\"."),
            Rule.of("\\bhave problem\\b", "have a problem",
                    "Sanaladigan otlar oldidan \"a\" artikli kerak: \"a problem\"."),
            Rule.of("\\bhas problem\\b", "has a problem",
                    "Sanaladigan otlar oldidan \"a\" artikli kerak: \"a problem\"."),
            Rule.of("\\bhave issue\\b", "have an issue",
                    "Unli tovush bilan boshlangan soʻz oldidan \"an\" keladi: \"an issue\"."),
            Rule.of("\\bam agree\\b", "agree", "\"I am agree\" emas, shunchaki \"I agree\" deyiladi."),
            Rule.of("\\bdepends of\\b", "depends on", "\"depend\" feʼli \"on\" bilan ishlatiladi: \"depends on\"."),
            Rule.of("\\bdiscuss about\\b", "discuss", "\"discuss\" dan keyin \"about\" kerak emas."),
            Rule.of("\\bexplain me\\b", "explain to me", "\"explain to me\" deb ayting."),
            Rule.of("\\bi will send you tomorrow[.!?]?$", "I will send it to you tomorrow",
                    "Nimani yuborishni koʻrsating: \"send it to you\"."),
            Rule.of("\\bpeoples\\b", "people", "\"people\" soʻzi oʻzi koʻplikda, \"s\" qoʻshilmaydi."),
            Rule.of("\\binformations\\b", "information", "\"information\" sanalmaydi, \"s\" qoʻshilmaydi."));

    private static final List<String> REPLIES = List.of(
            "Thanks for the update. What do you need from me?",
            "Understood. When do you think it will be ready?");

    private static final String CLOSING = "Okay, that works. Thanks, and let me know if anything changes.";

    @Override
    public RoleplayFeedback roleplayTurn(RoleplayRequest request) {
        Correction correction = correct(request.learnerText());
        String reply = request.finalTurn()
                ? CLOSING
                : REPLIES.get(Math.min(request.history().size(), REPLIES.size() - 1));
        return new RoleplayFeedback(correction.text(), correction.tips(), reply, PROVIDER);
    }

    @Override
    public MessageFeedback improveMessage(String text, String nativeLanguage) {
        Correction correction = correct(text);
        return new MessageFeedback(correction.text(), correction.tips(), PROVIDER);
    }

    private record Correction(String text, List<String> tips) {}

    private static Correction correct(String input) {
        String text = input.strip().replaceAll("\\s+", " ");
        List<String> tips = new ArrayList<>();
        for (Rule rule : RULES) {
            Matcher matcher = rule.pattern().matcher(text);
            if (matcher.find()) {
                text = applyRule(rule, matcher);
                if (tips.size() < 2) {
                    tips.add(rule.tip());
                }
            }
        }
        text = text.replaceAll("\\bi\\b", "I");
        text = Character.toUpperCase(text.charAt(0)) + text.substring(1);
        if (!text.matches(".*[.!?]$")) {
            text += ".";
        }
        return new Correction(text, List.copyOf(tips));
    }

    private static String applyRule(Rule rule, Matcher matcher) {
        if (!rule.replacement().contains("#BASE#")) {
            return matcher.replaceAll(rule.replacement());
        }
        return matcher.replaceAll(m -> m.group(1) + " " + m.group(2).replaceFirst("(?i)(ed|d)$", "")
                .replaceFirst("(?i)^merg$", "merge"));
    }
}
