package com.fitmentor.coach;

import com.fitmentor.ai.AiServiceClient;
import org.springframework.stereotype.Service;

/**
 * Asks the AI coach first and falls back to the offline rules when it does not answer.
 * The {@code source} field tells the client which one answered: "ai" or "rules".
 */
@Service
public class CoachService {

    private final AiServiceClient ai;
    private final RuleBasedCoach rules = new RuleBasedCoach();

    public CoachService(AiServiceClient ai) {
        this.ai = ai;
    }

    public CoachAnswer ask(String question, String context) {
        return ai.coach(question, context)
            .map(answer -> new CoachAnswer(answer, "ai"))
            .orElseGet(() -> new CoachAnswer(rules.answer(question), "rules"));
    }

    /** Answer returned by {@code POST /api/coach}. */
    public record CoachAnswer(String answer, String source) {
    }
}
