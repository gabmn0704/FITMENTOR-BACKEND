package com.fitmentor.coach;

import com.fitmentor.ai.AiServiceClient;
import com.fitmentor.ai.AiServiceClient.CoachContext;
import com.fitmentor.training.SessionSummary;
import com.fitmentor.training.TrainingFacade;
import com.fitmentor.user.UserEntity;
import com.fitmentor.user.UserService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Asks the AI coach first and falls back to the offline rules when it does not answer.
 *
 * <p>The question goes to the AI service with a structured context built on the server from
 * the user's level and last saved session, so the client cannot inject arbitrary context.
 * The {@code source} field tells the client who answered, with the same values as
 * FITMENTOR-IA: "claude" (Claude via the AI service) or "rules" (rules in the AI service or,
 * when it is down, {@link RuleBasedCoach} in this backend).
 */
@Service
public class CoachService {

    public static final String SOURCE_CLAUDE = "claude";
    public static final String SOURCE_RULES = "rules";

    private final AiServiceClient ai;
    private final UserService users;
    private final TrainingFacade training;
    private final RuleBasedCoach rules = new RuleBasedCoach();

    public CoachService(AiServiceClient ai, UserService users, TrainingFacade training) {
        this.ai = ai;
        this.users = users;
        this.training = training;
    }

    public CoachAnswer ask(UUID userId, String question) {
        CoachContext context = contextFor(userId);
        return ai.coach(question, context)
            .map(reply -> new CoachAnswer(reply.answer(), SOURCE_CLAUDE.equals(reply.source()) ? SOURCE_CLAUDE : SOURCE_RULES))
            .orElseGet(() -> new CoachAnswer(rules.answer(question), SOURCE_RULES));
    }

    private CoachContext contextFor(UUID userId) {
        UserEntity user = users.require(userId);
        List<SessionSummary> last = training.recentSessions(userId, 1);
        if (last.isEmpty()) {
            return new CoachContext(null, null, null, user.getLevel());
        }
        SessionSummary session = last.get(0);
        return new CoachContext(session.exercise(), session.accuracy(), session.reps(), user.getLevel());
    }

    /** Answer returned by {@code POST /api/coach}; {@code source} is "claude" or "rules". */
    public record CoachAnswer(String answer, String source) {
    }
}
